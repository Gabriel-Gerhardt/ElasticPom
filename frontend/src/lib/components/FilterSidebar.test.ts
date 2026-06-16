import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, fireEvent, waitFor } from '@testing-library/svelte';
import FilterSidebar from './FilterSidebar.svelte';
import type { PaperDto } from '$lib/types/paper';
import type { FilterDefinition, FilterRequest } from '$lib/types/api';

// Mock the filters API
vi.mock('$lib/api/filters', () => ({
	getFilters: vi.fn()
}));

import { getFilters } from '$lib/api/filters';

const mockFilters: FilterDefinition[] = [
	{ filtername: 'subjects.keyword', order: 1, type: 'option' },
	{ filtername: 'contributors.keyword', order: 2, type: 'option' },
	{ filtername: 'date', order: 3, type: 'range' }
];

function makePaper(overrides: Partial<PaperDto> = {}): PaperDto {
	return {
		id: '1',
		paperId: 'pid-1',
		datestamp: null,
		title: 'Test Paper',
		creators: [],
		subjects: ['Machine Learning', 'NLP'],
		description: '',
		publisher: 'MIT Press',
		contributors: ['Alice', 'Bob'],
		date: '2022',
		type: 'article',
		format: 'pdf',
		identifier: 'doi:10.1',
		source: 'arXiv',
		language: 'en',
		relations: [],
		coverage: '',
		rights: 'open',
		paperType: 'research',
		uniqueFields: [],
		...overrides
	};
}

const papers: PaperDto[] = [
	makePaper({ subjects: ['Machine Learning', 'NLP'], contributors: ['Alice', 'Bob'] }),
	makePaper({ id: '2', subjects: ['Computer Vision'], contributors: ['Alice', 'Charlie'] })
];

beforeEach(() => {
	vi.mocked(getFilters).mockResolvedValue(mockFilters);
});

describe('FilterSidebar', () => {
	it('renders the "Filters" heading', () => {
		const { getByText } = render(FilterSidebar, {
			props: { papers, activeFilters: [] }
		});
		expect(getByText('Filters')).toBeInTheDocument();
	});

	it('renders section headers for loaded filter definitions', async () => {
		const { getByText } = render(FilterSidebar, {
			props: { papers, activeFilters: [] }
		});
		await waitFor(() => {
			expect(getByText('Subjects')).toBeInTheDocument();
			expect(getByText('Contributors')).toBeInTheDocument();
			expect(getByText('Date')).toBeInTheDocument();
		});
	});

	it('renders unique subjects extracted from papers', async () => {
		const { getByText } = render(FilterSidebar, {
			props: { papers, activeFilters: [] }
		});
		await waitFor(() => {
			expect(getByText('Machine Learning')).toBeInTheDocument();
			expect(getByText('NLP')).toBeInTheDocument();
			expect(getByText('Computer Vision')).toBeInTheDocument();
		});
	});

	it('renders unique contributors extracted from papers', async () => {
		const { getByText } = render(FilterSidebar, {
			props: { papers, activeFilters: [] }
		});
		await waitFor(() => {
			expect(getByText('Alice')).toBeInTheDocument();
			expect(getByText('Bob')).toBeInTheDocument();
			expect(getByText('Charlie')).toBeInTheDocument();
		});
	});

	it('renders a date input for range filter', async () => {
		const { container } = render(FilterSidebar, {
			props: { papers, activeFilters: [] }
		});
		await waitFor(() => {
			const input = container.querySelector('input[type="date"]');
			expect(input).toBeInTheDocument();
		});
	});

	it('emits "change" event with new filter added when option button is clicked', async () => {
		const { getByText, component } = render(FilterSidebar, {
			props: { papers, activeFilters: [] }
		});
		const changeHandler = vi.fn();
		component.$on('change', changeHandler);

		await waitFor(() => expect(getByText('NLP')).toBeInTheDocument());
		await fireEvent.click(getByText('NLP'));

		expect(changeHandler).toHaveBeenCalledOnce();
		const newFilters: FilterRequest[] = changeHandler.mock.calls[0][0].detail;
		expect(newFilters).toContainEqual({ filter_name: 'subjects.keyword', filter_option: 'NLP' });
	});

	it('emits "change" removing filter when active filter button is clicked again (toggle off)', async () => {
		const activeFilters: FilterRequest[] = [{ filter_name: 'subjects.keyword', filter_option: 'NLP' }];
		const { getByText, component } = render(FilterSidebar, {
			props: { papers, activeFilters }
		});
		const changeHandler = vi.fn();
		component.$on('change', changeHandler);

		await waitFor(() => expect(getByText('NLP')).toBeInTheDocument());
		await fireEvent.click(getByText('NLP'));

		expect(changeHandler).toHaveBeenCalledOnce();
		const newFilters: FilterRequest[] = changeHandler.mock.calls[0][0].detail;
		expect(newFilters).not.toContainEqual({ filter_name: 'subjects.keyword', filter_option: 'NLP' });
	});

	it('shows "Clear all filters" button when activeFilters is non-empty', () => {
		const activeFilters: FilterRequest[] = [{ filter_name: 'subjects.keyword', filter_option: 'NLP' }];
		const { getByText } = render(FilterSidebar, {
			props: { papers, activeFilters }
		});
		expect(getByText('Clear all filters')).toBeInTheDocument();
	});

	it('does NOT show "Clear all filters" when activeFilters is empty', () => {
		const { queryByText } = render(FilterSidebar, {
			props: { papers, activeFilters: [] }
		});
		expect(queryByText('Clear all filters')).not.toBeInTheDocument();
	});

	it('emits "change" with empty array when "Clear all filters" is clicked', async () => {
		const activeFilters: FilterRequest[] = [{ filter_name: 'subjects.keyword', filter_option: 'NLP' }];
		const { getByText, component } = render(FilterSidebar, {
			props: { papers, activeFilters }
		});
		const changeHandler = vi.fn();
		component.$on('change', changeHandler);

		await fireEvent.click(getByText('Clear all filters'));

		expect(changeHandler).toHaveBeenCalledOnce();
		expect(changeHandler.mock.calls[0][0].detail).toEqual([]);
	});

	it('renders no option sections when papers array is empty', async () => {
		const { queryByText } = render(FilterSidebar, {
			props: { papers: [], activeFilters: [] }
		});
		await waitFor(() => {
			// Subjects and Contributors sections should not appear (no values)
			expect(queryByText('Subjects')).not.toBeInTheDocument();
			expect(queryByText('Contributors')).not.toBeInTheDocument();
		});
	});

	it('falls back to empty filterDefs when getFilters rejects', async () => {
		vi.mocked(getFilters).mockRejectedValue(new Error('network error'));
		const { queryByText } = render(FilterSidebar, {
			props: { papers, activeFilters: [] }
		});
		await waitFor(() => {
			expect(queryByText('Subjects')).not.toBeInTheDocument();
		});
	});

	it('emits "change" with date filter when date input changes', async () => {
		const { container, component } = render(FilterSidebar, {
			props: { papers, activeFilters: [] }
		});
		const changeHandler = vi.fn();
		component.$on('change', changeHandler);

		await waitFor(() => {
			const input = container.querySelector('input[type="date"]');
			expect(input).toBeInTheDocument();
		});

		const input = container.querySelector('input[type="date"]') as HTMLInputElement;
		await fireEvent.change(input, { target: { value: '2023-06-15' } });

		expect(changeHandler).toHaveBeenCalledOnce();
		const newFilters: FilterRequest[] = changeHandler.mock.calls[0][0].detail;
		expect(newFilters).toContainEqual({ filter_name: 'date', filter_option: '2023-06-15' });
	});
});
