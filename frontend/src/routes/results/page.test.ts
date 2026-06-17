import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, waitFor } from '@testing-library/svelte';

// Minimal hand-rolled store (avoids importing svelte/store inside vi.hoisted,
// which runs before regular imports are initialized).
const mockPage = vi.hoisted(() => {
	let value: unknown;
	const subscribers = new Set<(v: unknown) => void>();
	return {
		set(v: unknown) {
			value = v;
			subscribers.forEach((fn) => fn(value));
		},
		subscribe(fn: (v: unknown) => void) {
			subscribers.add(fn);
			fn(value);
			return () => subscribers.delete(fn);
		}
	};
});

vi.mock('$app/stores', () => ({
	page: mockPage
}));

vi.mock('$lib/api/papers', () => ({
	getMostRelevant: vi.fn(),
	searchByQuery: vi.fn(),
	getFilters: vi.fn(),
	getFilterOptions: vi.fn()
}));

import Page from './+page.svelte';
import * as papersApi from '$lib/api/papers';

function setQuery(q: string) {
	const url = new URL('http://localhost/results');
	if (q) url.searchParams.set('q', q);
	mockPage.set({
		url,
		params: {},
		route: { id: null },
		status: 200,
		error: null,
		data: {},
		form: undefined,
		state: {}
	});
}

beforeEach(() => {
	vi.resetAllMocks();
	vi.mocked(papersApi.getMostRelevant).mockResolvedValue([]);
	vi.mocked(papersApi.searchByQuery).mockResolvedValue([]);
	vi.mocked(papersApi.getFilters).mockResolvedValue([]);
});

describe('results +page.svelte', () => {
	it('does not render the filter sidebar when browsing (no query)', async () => {
		setQuery('');
		const { queryByText } = render(Page);
		await waitFor(() => expect(papersApi.getMostRelevant).toHaveBeenCalled());
		expect(queryByText('Filters')).not.toBeInTheDocument();
	});

	it('renders the filter sidebar when there is an active search query', async () => {
		setQuery('neural networks');
		const { findByText } = render(Page);
		await waitFor(() => expect(papersApi.searchByQuery).toHaveBeenCalled());
		expect(await findByText('Filters')).toBeInTheDocument();
	});
});
