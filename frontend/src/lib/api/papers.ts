import type { PaperDto } from '$lib/types/paper';
import type { SearchRequest, FilterRequest, FilterDto } from '$lib/types/api';

const BASE = '/api/papers';

export async function getMostRelevant(pageSize = 10, page = 0): Promise<PaperDto[]> {
	const res = await fetch(`${BASE}/most-relevant/?page-size=${pageSize}&page=${page}`);
	if (!res.ok) {
		throw { message: `Failed to fetch papers: ${res.statusText}`, status: res.status };
	}
	return res.json();
}

export async function searchByQuery(
	query: string,
	pageSize = 10,
	page = 0,
	filters: FilterRequest[] = []
): Promise<PaperDto[]> {
	const body: SearchRequest = { query, pageSize, page, filters };
	const res = await fetch(`${BASE}/search-by-query`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify(body)
	});
	if (!res.ok) {
		throw { message: `Search failed: ${res.statusText}`, status: res.status };
	}
	return res.json();
}

export async function getFilters(): Promise<FilterDto[]> {
	const res = await fetch('/api/filters');
	if (!res.ok) {
		throw { message: `Failed to fetch filters: ${res.statusText}`, status: res.status };
	}
	return res.json();
}

export async function getFilterOptions(
	query: string,
	filterName: string,
	filters: FilterRequest[] = []
): Promise<string[]> {
	const res = await fetch(`${BASE}/filter-options`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ query, filter_name: filterName, filters })
	});
	if (!res.ok) {
		throw { message: `Failed to fetch filter options: ${res.statusText}`, status: res.status };
	}
	return res.json();
}
