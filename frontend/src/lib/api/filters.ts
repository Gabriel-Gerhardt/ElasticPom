import type { FilterDefinition } from '$lib/types/api';

export async function getFilters(): Promise<FilterDefinition[]> {
	const res = await fetch('/api/papers/filters');
	if (!res.ok) {
		throw { message: `Failed to fetch filters: ${res.statusText}`, status: res.status };
	}
	return res.json();
}
