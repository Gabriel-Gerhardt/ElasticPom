import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/svelte';
import PaperCard from './PaperCard.svelte';
import type { PaperDto } from '$lib/types/paper';

function makePaper(overrides: Partial<PaperDto> = {}): PaperDto {
	return {
		id: 'paper-1',
		paperId: 'pid-1',
		datestamp: '2021-06-15',
		title: 'Understanding Neural Networks',
		creators: ['Alice Smith', 'Bob Jones'],
		subjects: ['Machine Learning', 'Deep Learning', 'AI', 'NLP'],
		description: 'A comprehensive study of neural networks.',
		publisher: 'Springer',
		contributors: ['Carol White'],
		date: '2021',
		type: 'article',
		format: 'pdf',
		identifier: 'doi:10.1234/nn',
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

describe('PaperCard', () => {
	it('renders the paper title', () => {
		const { getByRole } = render(PaperCard, { props: { paper: makePaper() } });
		const heading = getByRole('heading', { level: 2 });
		expect(heading).toHaveTextContent('Understanding Neural Networks');
	});

	it('renders "Untitled" when title is empty', () => {
		const { getByRole } = render(PaperCard, { props: { paper: makePaper({ title: '' }) } });
		const heading = getByRole('heading', { level: 2 });
		expect(heading).toHaveTextContent('Untitled');
	});

	it('renders authors (creators) as a comma-separated string', () => {
		const { getByText } = render(PaperCard, { props: { paper: makePaper() } });
		expect(getByText('Alice Smith, Bob Jones')).toBeInTheDocument();
	});

	it('renders the date when present', () => {
		const { getByText } = render(PaperCard, { props: { paper: makePaper({ date: '2021' }) } });
		expect(getByText('2021')).toBeInTheDocument();
	});

	it('falls back to datestamp when date is null', () => {
		const paper = makePaper({ date: null, datestamp: '2020-03-10' });
		const { getByText } = render(PaperCard, { props: { paper } });
		expect(getByText('2020-03-10')).toBeInTheDocument();
	});

	it('does not render date section when both date and datestamp are null', () => {
		const paper = makePaper({ date: null, datestamp: null });
		const { queryByText } = render(PaperCard, { props: { paper } });
		// The date text should not appear
		expect(queryByText('2021')).not.toBeInTheDocument();
		expect(queryByText('2021-06-15')).not.toBeInTheDocument();
	});

	it('renders the description', () => {
		const { getByText } = render(PaperCard, { props: { paper: makePaper() } });
		expect(getByText('A comprehensive study of neural networks.')).toBeInTheDocument();
	});

	it('renders only the first 3 subjects as tags', () => {
		const { getByText, queryByText } = render(PaperCard, { props: { paper: makePaper() } });
		// First 3 subjects should be visible
		expect(getByText('Machine Learning')).toBeInTheDocument();
		expect(getByText('Deep Learning')).toBeInTheDocument();
		expect(getByText('AI')).toBeInTheDocument();
		// 4th subject should NOT be visible
		expect(queryByText('NLP')).not.toBeInTheDocument();
	});

	it('renders the paperType badge when present', () => {
		const { getByText } = render(PaperCard, { props: { paper: makePaper({ paperType: 'research' }) } });
		expect(getByText('research')).toBeInTheDocument();
	});

	it('does not render paperType badge when paperType is empty', () => {
		const paper = makePaper({ paperType: '' });
		const { queryByText } = render(PaperCard, { props: { paper } });
		// No badge text for empty paperType
		expect(queryByText('research')).not.toBeInTheDocument();
	});

	it('does not render creators section when creators array is empty', () => {
		const paper = makePaper({ creators: [] });
		const { queryByText } = render(PaperCard, { props: { paper } });
		expect(queryByText('Alice Smith, Bob Jones')).not.toBeInTheDocument();
	});

	it('does not render subjects when subjects array is empty', () => {
		const paper = makePaper({ subjects: [] });
		const { queryByText } = render(PaperCard, { props: { paper } });
		expect(queryByText('Machine Learning')).not.toBeInTheDocument();
	});

	it('renders as an article element', () => {
		const { container } = render(PaperCard, { props: { paper: makePaper() } });
		expect(container.querySelector('article')).toBeInTheDocument();
	});

	it('renders the paper link button when identifier is a valid DOI', () => {
		const { getByRole } = render(PaperCard, { props: { paper: makePaper() } });
		const link = getByRole('link', { name: 'View official paper page' });
		expect(link).toBeInTheDocument();
		expect(link).toHaveAttribute('href', 'https://doi.org/10.1234/nn');
	});

	it('renders the paper link button when identifier is a full URL', () => {
		const paper = makePaper({ identifier: 'https://example.com/paper/1' });
		const { getByRole } = render(PaperCard, { props: { paper } });
		expect(getByRole('link', { name: 'View official paper page' })).toHaveAttribute(
			'href',
			'https://example.com/paper/1'
		);
	});

	it('does not render the paper link button when identifier is empty', () => {
		const paper = makePaper({ identifier: '' });
		const { queryByRole } = render(PaperCard, { props: { paper } });
		expect(queryByRole('link', { name: 'View official paper page' })).not.toBeInTheDocument();
	});

	it('does not render the paper link button when identifier is not a recognizable link', () => {
		const paper = makePaper({ identifier: 'not-a-link-or-doi' });
		const { queryByRole } = render(PaperCard, { props: { paper } });
		expect(queryByRole('link', { name: 'View official paper page' })).not.toBeInTheDocument();
	});
});
