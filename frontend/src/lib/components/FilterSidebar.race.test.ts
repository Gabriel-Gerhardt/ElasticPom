import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, waitFor } from '@testing-library/svelte';
import FilterSidebar from './FilterSidebar.svelte';
import type { FilterDto } from '$lib/types/api';
import * as papersApi from '$lib/api/papers';

vi.mock('$lib/api/papers', () => ({
	getFilters: vi.fn(),
	getFilterOptions: vi.fn()
}));

const filterDefs: FilterDto[] = [{ filtername: 'language', order: 0, type: 'option' }];

/**
 * Exercises the real-world "fast typing" scenario flagged by the prior code
 * review as a race-condition risk: FilterSidebar's reactive `$: query,
 * activeFilters, scheduleReload()` statement debounces with a single shared
 * `debounceTimer` via clearTimeout/setTimeout. Because each query change
 * clears the PREVIOUS timer before scheduling a new one, only the LAST
 * query value in a rapid burst should ever actually trigger a
 * getFilterOptions call — earlier, superseded calls inside the debounce
 * window must never fire at all (so there is no in-flight response from an
 * earlier query that could race with / overwrite the later one).
 *
 * This goes beyond the existing 2-step "re-fetches when query changes" test
 * in FilterSidebar.test.ts (which waits out the full debounce between each
 * change, so it never actually exercises overlapping/in-flight timers) by
 * firing 3+ query changes back-to-back WITHIN the 300ms debounce window.
 */
describe('FilterSidebar rapid query changes (debounce race condition)', () => {
	beforeEach(() => {
		vi.resetAllMocks();
		vi.useFakeTimers();
	});

	afterEach(() => {
		vi.useRealTimers();
	});

	it('only fetches options for the LAST query when 3+ changes happen within the debounce window', async () => {
		vi.mocked(papersApi.getFilters).mockResolvedValue(filterDefs);
		vi.mocked(papersApi.getFilterOptions).mockResolvedValue(['en']);

		const { component } = render(FilterSidebar, {
			props: { activeFilters: [], query: 'q1' }
		});

		// Advance partway through the debounce window — not enough to fire yet.
		await vi.advanceTimersByTimeAsync(100);
		component.$set({ query: 'q2' });

		await vi.advanceTimersByTimeAsync(100);
		component.$set({ query: 'q3' });

		await vi.advanceTimersByTimeAsync(100);
		component.$set({ query: 'q4' });

		// At this point all three intermediate timers (for the initial mount,
		// 'q2', and 'q3') must have been cleared by the subsequent change before
		// they ever reached 300ms, so getFilterOptions must not have fired yet
		// for ANY query value.
		expect(papersApi.getFilterOptions).not.toHaveBeenCalled();

		// Now let the final ('q4') debounce timer actually elapse.
		await vi.advanceTimersByTimeAsync(300);

		// Exactly one debounced load should have fired, and it must be for the
		// LAST query value only.
		expect(papersApi.getFilterOptions).toHaveBeenCalledTimes(1);
		expect(papersApi.getFilterOptions).toHaveBeenCalledWith('q4', 'language', []);
		expect(papersApi.getFilterOptions).not.toHaveBeenCalledWith('q1', 'language', []);
		expect(papersApi.getFilterOptions).not.toHaveBeenCalledWith('q2', 'language', []);
		expect(papersApi.getFilterOptions).not.toHaveBeenCalledWith('q3', 'language', []);
	});

	it('a slow-resolving earlier query response does not overwrite the latest rendered options', async () => {
		// Simulate the network resolving out of order: if 'slow-query' had been
		// allowed to fire, it would resolve AFTER 'fast-query' (e.g. a slower
		// backend round-trip for the first keystroke's request). Because the
		// debounce timer is cleared on every change, 'slow-query' must never
		// actually be requested at all, so this ordering can't cause a stale
		// overwrite in the first place.
		let resolveSlow: (value: string[]) => void = () => {};
		const slowPromise = new Promise<string[]>((resolve) => {
			resolveSlow = resolve;
		});

		vi.mocked(papersApi.getFilters).mockResolvedValue(filterDefs);
		vi.mocked(papersApi.getFilterOptions).mockImplementation(async (query: string) => {
			if (query === 'slow-query') return slowPromise;
			return ['fr'];
		});

		const { getByText, component } = render(FilterSidebar, {
			props: { activeFilters: [], query: 'slow-query' }
		});

		// Change query again well within the debounce window, before
		// 'slow-query' would ever have been dispatched.
		await vi.advanceTimersByTimeAsync(50);
		component.$set({ query: 'fast-query' });

		// Let the (only remaining) debounce timer for 'fast-query' elapse.
		await vi.advanceTimersByTimeAsync(300);
		await waitFor(() => expect(papersApi.getFilterOptions).toHaveBeenCalledTimes(1));
		expect(papersApi.getFilterOptions).toHaveBeenCalledWith('fast-query', 'language', []);

		await waitFor(() => expect(getByText('fr')).toBeInTheDocument());

		// Now resolve the slow promise late, simulating a stale in-flight
		// response finally arriving. Since 'slow-query' was never actually
		// requested (its timer was cleared), this resolution is a no-op and
		// must not affect the rendered options.
		resolveSlow(['en']);
		await vi.advanceTimersByTimeAsync(0);

		expect(getByText('fr')).toBeInTheDocument();
		expect(papersApi.getFilterOptions).toHaveBeenCalledTimes(1);
	});
});
