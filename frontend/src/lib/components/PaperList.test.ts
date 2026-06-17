import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/svelte';
import PaperList from './PaperList.svelte';
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

describe('PaperList — paper link button integration (GAB-26)', () => {
	it('renders an independent link button per card for a mix of link/DOI/no-link papers', () => {
		const papers = [
			makePaper({ id: 'a', title: 'Has full URL', identifier: 'https://example.com/paper/a' }),
			makePaper({ id: 'b', title: 'Has DOI', identifier: 'doi:10.1234/b' }),
			makePaper({ id: 'c', title: 'Has bare DOI', identifier: '10.5678/c' }),
			makePaper({ id: 'd', title: 'No link', identifier: null }),
			makePaper({ id: 'e', title: 'Unrecognizable identifier', identifier: 'not-a-link-or-doi' })
		];

		const { getAllByRole } = render(PaperList, { props: { papers } });

		const links = getAllByRole('link', { name: 'View official page' });
		// Only the 3 papers with a resolvable link should get a button — no leakage
		// between cards (e.g. card "d" or "e" picking up a neighboring link).
		expect(links).toHaveLength(3);
		expect(links.map((l) => l.getAttribute('href'))).toEqual([
			'https://example.com/paper/a',
			'https://doi.org/10.1234/b',
			'https://doi.org/10.5678/c'
		]);
	});

	it('opens paper links in a new tab with safe rel attributes', () => {
		const papers = [makePaper({ id: 'a', identifier: 'https://example.com/paper/a' })];
		const { getByRole } = render(PaperList, { props: { papers } });

		const link = getByRole('link', { name: 'View official page' });
		expect(link).toHaveAttribute('target', '_blank');
		expect(link).toHaveAttribute('rel', 'noopener noreferrer');
	});

	it('renders no link buttons at all when every paper lacks a resolvable link', () => {
		const papers = [
			makePaper({ id: 'a', identifier: null }),
			makePaper({ id: 'b', identifier: '' }),
			makePaper({ id: 'c', identifier: 'doi:' }),
			makePaper({ id: 'd', identifier: 'garbage' })
		];
		const { queryByRole } = render(PaperList, { props: { papers } });
		expect(queryByRole('link', { name: 'View official page' })).not.toBeInTheDocument();
	});

	it('renders the "no papers found" message and no link buttons for an empty list', () => {
		const { getByText, queryByRole } = render(PaperList, { props: { papers: [] } });
		expect(getByText('No papers found.')).toBeInTheDocument();
		expect(queryByRole('link', { name: 'View official page' })).not.toBeInTheDocument();
	});
});
