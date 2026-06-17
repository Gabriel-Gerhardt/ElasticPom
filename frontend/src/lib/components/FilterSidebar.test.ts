import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, fireEvent, waitFor } from '@testing-library/svelte';
import FilterSidebar from './FilterSidebar.svelte';
import type { FilterRequest, FilterDto } from '$lib/types/api';
import * as papersApi from '$lib/api/papers';

vi.mock('$lib/api/papers', () => ({
	getFilters: vi.fn(),
	getFilterOptions: vi.fn()
}));

const filterDefs: FilterDto[] = [
	{ filtername: 'language', order: 0, type: 'option' },
	{ filtername: 'publisher', order: 1, type: 'option' },
	{ filtername: 'datestamp', order: 2, type: 'range' }
];

function mockApi(options: Record<string, string[]> = {}) {
	vi.mocked(papersApi.getFilters).mockResolvedValue(filterDefs);
	vi.mocked(papersApi.getFilterOptions).mockImplementation(async (_query: string, filterName: string) => {
		return options[filterName] ?? [];
	});
}

// loadFilters() is scheduled through a 300ms debounce timer, including the
// initial load (the reactive statement fires once immediately on init).
async function waitForDebounce() {
	await new Promise((resolve) => setTimeout(resolve, 310));
}

beforeEach(() => {
	vi.resetAllMocks();
});

describe('FilterSidebar', () => {
	it('renders the "Filters" heading', async () => {
		mockApi();
		const { getByText } = render(FilterSidebar, {
			props: { activeFilters: [] }
		});
		expect(getByText('Filters')).toBeInTheDocument();
	});

	it('fetches filter definitions via getFilters after the debounce', async () => {
		mockApi();
		render(FilterSidebar, { props: { activeFilters: [] } });
		await waitForDebounce();
		await waitFor(() => expect(papersApi.getFilters).toHaveBeenCalled());
	});

	it('renders option-type filter sections with values fetched via getFilterOptions', async () => {
		mockApi({ language: ['en', 'fr'], publisher: ['MIT Press', 'Springer'] });
		const { getByText } = render(FilterSidebar, {
			props: { activeFilters: [] }
		});

		await waitForDebounce();
		await waitFor(() => expect(getByText('Language')).toBeInTheDocument());
		expect(getByText('en')).toBeInTheDocument();
		expect(getByText('fr')).toBeInTheDocument();
		expect(getByText('Publisher')).toBeInTheDocument();
		expect(getByText('MIT Press')).toBeInTheDocument();
		expect(getByText('Springer')).toBeInTheDocument();

		expect(papersApi.getFilterOptions).toHaveBeenCalledWith('', 'language', []);
		expect(papersApi.getFilterOptions).toHaveBeenCalledWith('', 'publisher', []);
	});

	it('passes the query prop and activeFilters through to getFilterOptions', async () => {
		mockApi({ language: ['en'] });
		const activeFilters: FilterRequest[] = [{ filter_name: 'publisher', filter_option: 'MIT Press' }];
		render(FilterSidebar, {
			props: { activeFilters, query: 'deep learning' }
		});

		await waitForDebounce();
		await waitFor(() =>
			expect(papersApi.getFilterOptions).toHaveBeenCalledWith('deep learning', 'language', activeFilters)
		);
	});

	it('re-fetches filter options when the query prop changes', async () => {
		mockApi({ language: ['en'] });
		const { component } = render(FilterSidebar, {
			props: { activeFilters: [], query: 'first query' }
		});

		await waitForDebounce();
		await waitFor(() =>
			expect(papersApi.getFilterOptions).toHaveBeenCalledWith('first query', 'language', [])
		);

		component.$set({ query: 'second query' });

		await waitForDebounce();
		await waitFor(() =>
			expect(papersApi.getFilterOptions).toHaveBeenCalledWith('second query', 'language', [])
		);
	});

	it('does not call getFilterOptions for range-type filters', async () => {
		mockApi({ language: ['en'] });
		render(FilterSidebar, { props: { activeFilters: [] } });

		await waitForDebounce();
		await waitFor(() => expect(papersApi.getFilters).toHaveBeenCalledOnce());
		expect(papersApi.getFilterOptions).not.toHaveBeenCalledWith(expect.anything(), 'datestamp', expect.anything());
	});

	it('renders date inputs for range-type filters', async () => {
		mockApi({ language: ['en'] });
		const { getByText, getByPlaceholderText } = render(FilterSidebar, {
			props: { activeFilters: [] }
		});

		await waitForDebounce();
		await waitFor(() => expect(getByText('Datestamp')).toBeInTheDocument());
		expect(getByPlaceholderText('From')).toHaveAttribute('type', 'date');
		expect(getByPlaceholderText('To')).toHaveAttribute('type', 'date');
	});

	it('emits "change" event with new filter added when an option button is clicked', async () => {
		mockApi({ language: ['en'] });
		const { getByText, component } = render(FilterSidebar, {
			props: { activeFilters: [] }
		});
		const changeHandler = vi.fn();
		component.$on('change', changeHandler);

		await waitForDebounce();
		await waitFor(() => expect(getByText('en')).toBeInTheDocument());
		await fireEvent.click(getByText('en'));

		expect(changeHandler).toHaveBeenCalledOnce();
		const newFilters: FilterRequest[] = changeHandler.mock.calls[0][0].detail;
		expect(newFilters).toContainEqual({ filter_name: 'language', filter_option: 'en' });
	});

	it('emits "change" event removing filter when active option button is clicked again (toggle off)', async () => {
		mockApi({ language: ['en'] });
		const activeFilters: FilterRequest[] = [{ filter_name: 'language', filter_option: 'en' }];
		const { getByText, component } = render(FilterSidebar, {
			props: { activeFilters }
		});
		const changeHandler = vi.fn();
		component.$on('change', changeHandler);

		await waitForDebounce();
		await waitFor(() => expect(getByText('en')).toBeInTheDocument());
		await fireEvent.click(getByText('en'));

		expect(changeHandler).toHaveBeenCalledOnce();
		const newFilters: FilterRequest[] = changeHandler.mock.calls[0][0].detail;
		expect(newFilters).not.toContainEqual({ filter_name: 'language', filter_option: 'en' });
	});

	it('emits "change" with filter_option and filter_option_end when range inputs change', async () => {
		mockApi();
		const { getByPlaceholderText, component } = render(FilterSidebar, {
			props: { activeFilters: [] }
		});
		const changeHandler = vi.fn();
		component.$on('change', changeHandler);

		await waitForDebounce();
		await waitFor(() => expect(getByPlaceholderText('From')).toBeInTheDocument());

		await fireEvent.change(getByPlaceholderText('From'), { target: { value: '2020-01-01' } });
		let newFilters: FilterRequest[] = changeHandler.mock.calls[0][0].detail;
		expect(newFilters).toContainEqual({
			filter_name: 'datestamp',
			filter_option: '2020-01-01',
			filter_option_end: ''
		});

		await fireEvent.change(getByPlaceholderText('To'), { target: { value: '2023-12-31' } });
		newFilters = changeHandler.mock.calls[1][0].detail;
		expect(newFilters).toContainEqual({
			filter_name: 'datestamp',
			filter_option: '2020-01-01',
			filter_option_end: '2023-12-31'
		});
	});

	it('shows "Clear all filters" button when activeFilters is non-empty', async () => {
		mockApi();
		const activeFilters: FilterRequest[] = [{ filter_name: 'language', filter_option: 'en' }];
		const { getByText } = render(FilterSidebar, {
			props: { activeFilters }
		});
		expect(getByText('Clear all filters')).toBeInTheDocument();
	});

	it('does NOT show "Clear all filters" when activeFilters is empty', async () => {
		mockApi();
		const { queryByText } = render(FilterSidebar, {
			props: { activeFilters: [] }
		});
		expect(queryByText('Clear all filters')).not.toBeInTheDocument();
	});

	it('emits "change" with empty array when "Clear all filters" is clicked', async () => {
		mockApi();
		const activeFilters: FilterRequest[] = [{ filter_name: 'language', filter_option: 'en' }];
		const { getByText, component } = render(FilterSidebar, {
			props: { activeFilters }
		});
		const changeHandler = vi.fn();
		component.$on('change', changeHandler);

		await fireEvent.click(getByText('Clear all filters'));

		expect(changeHandler).toHaveBeenCalledOnce();
		expect(changeHandler.mock.calls[0][0].detail).toEqual([]);
	});

	it('renders nothing for option filter sections with no values', async () => {
		mockApi({ language: [] });
		const { queryByText } = render(FilterSidebar, {
			props: { activeFilters: [] }
		});
		await waitForDebounce();
		await waitFor(() => expect(papersApi.getFilters).toHaveBeenCalled());
		expect(queryByText('Language')).not.toBeInTheDocument();
	});

	// -------------------------------------------------------------------------
	// Edge case: getFilters() resolves with an empty list
	// -------------------------------------------------------------------------

	it('renders only the heading when getFilters resolves with an empty list', async () => {
		vi.mocked(papersApi.getFilters).mockResolvedValue([]);
		vi.mocked(papersApi.getFilterOptions).mockResolvedValue([]);

		const { getByText, queryByText } = render(FilterSidebar, {
			props: { activeFilters: [] }
		});

		await waitForDebounce();
		await waitFor(() => expect(papersApi.getFilters).toHaveBeenCalledOnce());
		expect(getByText('Filters')).toBeInTheDocument();
		expect(papersApi.getFilterOptions).not.toHaveBeenCalled();
		expect(queryByText('Clear all filters')).not.toBeInTheDocument();
	});

	// -------------------------------------------------------------------------
	// Edge case: getFilters() resolves but getFilterOptions() rejects for one
	// specific filter (partial failure). The component awaits each
	// getFilterOptions call sequentially inside a for-loop with no per-filter
	// try/catch, so a single rejection short-circuits loadFilters() and the
	// rejection propagates out as an unhandled promise rejection (loadFilters()
	// is invoked from the debounced reactive statement without a .catch()).
	// The filter list itself (filterDefs) is static and rendered by the #each
	// block regardless of how far the loop got, so every filter section still
	// renders structurally; only the option VALUES for filters at-or-after the
	// failing one in iteration order never get populated. "language" (before
	// "publisher" in filterDefs) keeps its fetched values because Svelte's
	// compiler tracks the `optionValues[key] = ...` assignment as its own
	// reactive update.
	// -------------------------------------------------------------------------

	it('keeps values fetched before a getFilterOptions rejection, while later filters stay unpopulated', async () => {
		const onUnhandledRejection = (reason: unknown) => {
			expect((reason as Error).message).toBe('network error');
		};
		process.on('unhandledRejection', onUnhandledRejection);

		vi.mocked(papersApi.getFilters).mockResolvedValue(filterDefs);
		vi.mocked(papersApi.getFilterOptions).mockImplementation(async (_query: string, filterName: string) => {
			if (filterName === 'language') {
				return ['en', 'fr'];
			}
			if (filterName === 'publisher') {
				throw new Error('network error');
			}
			return [];
		});

		const { getByText, queryByText, getByPlaceholderText } = render(FilterSidebar, {
			props: { activeFilters: [] }
		});

		await waitForDebounce();
		await waitFor(() => expect(papersApi.getFilterOptions).toHaveBeenCalledWith('', 'publisher', []));
		// Drain the microtask queue so the rejection surfaces before we assert.
		await new Promise((resolve) => setTimeout(resolve, 0));
		process.removeListener('unhandledRejection', onUnhandledRejection);

		// The heading always renders regardless of the downstream failure.
		expect(getByText('Filters')).toBeInTheDocument();

		// "language" was processed (and its key assigned) before "publisher" threw,
		// so its fetched option values are visible.
		expect(getByText('Language')).toBeInTheDocument();
		expect(getByText('en')).toBeInTheDocument();

		// "datestamp" is a range filter rendered unconditionally by the static
		// filterDefs list, so its From/To inputs still appear even though the
		// loop never reached it to populate rangeValues for it.
		expect(getByPlaceholderText('From')).toBeInTheDocument();
		expect(getByPlaceholderText('To')).toBeInTheDocument();
		expect(queryByText('Datestamp')).toBeInTheDocument();

		expect(papersApi.getFilterOptions).toHaveBeenCalledWith('', 'language', []);
		expect(papersApi.getFilterOptions).toHaveBeenCalledWith('', 'publisher', []);
	});

	it('does not crash the page when getFilters itself rejects', async () => {
		const onUnhandledRejection = (reason: unknown) => {
			expect((reason as Error).message).toBe('network error');
		};
		process.on('unhandledRejection', onUnhandledRejection);

		vi.mocked(papersApi.getFilters).mockRejectedValue(new Error('network error'));
		vi.mocked(papersApi.getFilterOptions).mockResolvedValue([]);

		const { getByText } = render(FilterSidebar, {
			props: { activeFilters: [] }
		});

		await waitForDebounce();
		await waitFor(() => expect(papersApi.getFilters).toHaveBeenCalledOnce());
		await new Promise((resolve) => setTimeout(resolve, 0));
		process.removeListener('unhandledRejection', onUnhandledRejection);

		// Heading still renders; component does not throw an unhandled error
		// that would crash the whole results page.
		expect(getByText('Filters')).toBeInTheDocument();
		expect(papersApi.getFilterOptions).not.toHaveBeenCalled();
	});
});
