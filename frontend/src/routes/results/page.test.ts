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
import type { PaperDto } from '$lib/types/paper';

function makePaper(overrides: Partial<PaperDto> = {}): PaperDto {
	return {
		id: 'paper-1',
		paperId: 'pid-1',
		datestamp: '2021-06-15',
		title: 'Untitled Paper',
		creators: [],
		subjects: [],
		description: '',
		publisher: '',
		contributors: [],
		date: null,
		type: 'article',
		format: 'pdf',
		identifier: null,
		source: 'arXiv',
		language: 'en',
		relations: [],
		coverage: '',
		rights: 'open',
		paperType: '',
		uniqueFields: [],
		...overrides
	};
}

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

	it('renders paper link buttons end-to-end for a mix of link/no-link results, with no leakage between cards', async () => {
		setQuery('neural networks');
		vi.mocked(papersApi.searchByQuery).mockResolvedValue([
			makePaper({ id: 'a', title: 'Paper With DOI', identifier: 'doi:10.1234/a' }),
			makePaper({ id: 'b', title: 'Paper Without Link', identifier: null })
		]);

		const { findAllByRole, getByText } = render(Page);
		await waitFor(() => expect(papersApi.searchByQuery).toHaveBeenCalled());

		expect(getByText('Paper With DOI')).toBeInTheDocument();
		expect(getByText('Paper Without Link')).toBeInTheDocument();

		const links = await findAllByRole('link', { name: 'View official paper page' });
		expect(links).toHaveLength(1);
		expect(links[0]).toHaveAttribute('href', 'https://doi.org/10.1234/a');
		expect(links[0]).toHaveAttribute('target', '_blank');
	});
});
