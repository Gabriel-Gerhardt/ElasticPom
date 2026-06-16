import { describe, it, expect, vi } from 'vitest';
import { render, fireEvent } from '@testing-library/svelte';
import FilterSidebar from './FilterSidebar.svelte';
import type { PaperDto } from '$lib/types/paper';
import type { FilterRequest } from '$lib/types/api';

function makePaper(overrides: Partial<PaperDto> = {}): PaperDto {
	return {
		id: '1',
		paperId: 'pid-1',
		datestamp: null,
		title: 'Test Paper',
		creators: [],
		subjects: [],
		description: '',
		publisher: 'MIT Press',
		contributors: [],
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
	makePaper({ language: 'en', type: 'article', paperType: 'research', publisher: 'MIT Press' }),
	makePaper({ id: '2', language: 'fr', type: 'thesis', paperType: 'survey', publisher: 'Springer' }),
	makePaper({ id: '3', language: 'en', type: 'article', paperType: 'research', publisher: 'MIT Press' })
];

describe('FilterSidebar', () => {
	it('renders the "Filters" heading', () => {
		const { getByText } = render(FilterSidebar, {
			props: { papers, activeFilters: [] }
		});
		expect(getByText('Filters')).toBeInTheDocument();
	});

	it('renders section headers for filter fields that have values', () => {
		const { getByText } = render(FilterSidebar, {
			props: { papers, activeFilters: [] }
		});
		expect(getByText('Language')).toBeInTheDocument();
		expect(getByText('Type')).toBeInTheDocument();
		expect(getByText('Paper Type')).toBeInTheDocument();
		expect(getByText('Publisher')).toBeInTheDocument();
	});

	it('renders unique language options extracted from papers', () => {
		const { getByText } = render(FilterSidebar, {
			props: { papers, activeFilters: [] }
		});
		// 'en' and 'fr' should appear as buttons
		expect(getByText('en')).toBeInTheDocument();
		expect(getByText('fr')).toBeInTheDocument();
	});

	it('renders unique type options extracted from papers', () => {
		const { getAllByText, getByText } = render(FilterSidebar, {
			props: { papers, activeFilters: [] }
		});
		expect(getByText('article')).toBeInTheDocument();
		expect(getByText('thesis')).toBeInTheDocument();
	});

	it('renders unique publisher options extracted from papers', () => {
		const { getByText } = render(FilterSidebar, {
			props: { papers, activeFilters: [] }
		});
		expect(getByText('MIT Press')).toBeInTheDocument();
		expect(getByText('Springer')).toBeInTheDocument();
	});

	it('emits "change" event with new filter added when filter button is clicked', async () => {
		const { getByText, component } = render(FilterSidebar, {
			props: { papers, activeFilters: [] }
		});
		const changeHandler = vi.fn();
		component.$on('change', changeHandler);

		await fireEvent.click(getByText('en'));

		expect(changeHandler).toHaveBeenCalledOnce();
		const newFilters: FilterRequest[] = changeHandler.mock.calls[0][0].detail;
		expect(newFilters).toContainEqual({ filter_name: 'language', filter_option: 'en' });
	});

	it('emits "change" event removing filter when active filter button is clicked again (toggle off)', async () => {
		const activeFilters: FilterRequest[] = [{ filter_name: 'language', filter_option: 'en' }];
		const { getByText, component } = render(FilterSidebar, {
			props: { papers, activeFilters }
		});
		const changeHandler = vi.fn();
		component.$on('change', changeHandler);

		await fireEvent.click(getByText('en'));

		expect(changeHandler).toHaveBeenCalledOnce();
		const newFilters: FilterRequest[] = changeHandler.mock.calls[0][0].detail;
		expect(newFilters).not.toContainEqual({ filter_name: 'language', filter_option: 'en' });
	});

	it('shows "Clear all filters" button when activeFilters is non-empty', () => {
		const activeFilters: FilterRequest[] = [{ filter_name: 'language', filter_option: 'en' }];
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
		const activeFilters: FilterRequest[] = [{ filter_name: 'language', filter_option: 'en' }];
		const { getByText, component } = render(FilterSidebar, {
			props: { papers, activeFilters }
		});
		const changeHandler = vi.fn();
		component.$on('change', changeHandler);

		await fireEvent.click(getByText('Clear all filters'));

		expect(changeHandler).toHaveBeenCalledOnce();
		expect(changeHandler.mock.calls[0][0].detail).toEqual([]);
	});

	it('renders nothing for filter sections with no values', () => {
		// papers with no type — those sections should not appear
		const papersNoType = papers.map(p => ({ ...p, type: '' }));
		const { queryByText } = render(FilterSidebar, {
			props: { papers: papersNoType, activeFilters: [] }
		});
		expect(queryByText('Type')).not.toBeInTheDocument();
	});

	it('renders no filter options when papers array is empty', () => {
		const { queryByText } = render(FilterSidebar, {
			props: { papers: [], activeFilters: [] }
		});
		// No language/type/paperType/publisher options to show
		expect(queryByText('Language')).not.toBeInTheDocument();
	});
});
