<script lang="ts">
	import type { FilterRequest, FilterDto } from '$lib/types/api';
	import { createEventDispatcher } from 'svelte';
	import { getFilters, getFilterOptions } from '$lib/api/papers';

	export let activeFilters: FilterRequest[];
	export let query: string = '';

	const dispatch = createEventDispatcher<{ change: FilterRequest[] }>();

	let filterDefs: FilterDto[] = [];
	let optionValues: Record<string, string[]> = {};
	let rangeValues: Record<string, { from: string; to: string }> = {};

	function label(filtername: string): string {
		return filtername.charAt(0).toUpperCase() + filtername.slice(1);
	}

	async function loadFilters() {
		filterDefs = await getFilters();
		for (const filter of filterDefs) {
			if (filter.type === 'option') {
				optionValues[filter.filtername] = await getFilterOptions(query, filter.filtername, activeFilters);
			} else if (filter.type === 'range') {
				const existing = activeFilters.find(f => f.filter_name === filter.filtername);
				rangeValues[filter.filtername] = {
					from: existing?.filter_option ?? '',
					to: existing?.filter_option_end ?? ''
				};
			}
		}
		optionValues = { ...optionValues };
		rangeValues = { ...rangeValues };
	}

	// Load on mount and re-fetch option values whenever the active query or
	// filters change, so options stay scoped to what's actually being
	// searched. Debounced with a simple timer (no new dependency) to avoid a
	// request storm if both change in quick succession (e.g. typing a query
	// while filters update). Svelte runs this reactive statement once
	// immediately, which covers the initial load too.
	let debounceTimer: ReturnType<typeof setTimeout>;
	$: query, activeFilters, scheduleReload();

	function scheduleReload() {
		clearTimeout(debounceTimer);
		debounceTimer = setTimeout(loadFilters, 300);
	}

	function isActive(fieldKey: string, option: string): boolean {
		return activeFilters.some(f => f.filter_name === fieldKey && f.filter_option === option);
	}

	function toggle(fieldKey: string, option: string) {
		if (isActive(fieldKey, option)) {
			dispatch('change', activeFilters.filter(f => !(f.filter_name === fieldKey && f.filter_option === option)));
		} else {
			dispatch('change', [...activeFilters, { filter_name: fieldKey, filter_option: option }]);
		}
	}

	function inputValue(e: Event): string {
		return (e.target as HTMLInputElement).value;
	}

	function updateRange(fieldKey: string, from: string, to: string) {
		rangeValues[fieldKey] = { from, to };
		rangeValues = { ...rangeValues };

		const others = activeFilters.filter(f => f.filter_name !== fieldKey);
		if (!from && !to) {
			dispatch('change', others);
			return;
		}
		dispatch('change', [...others, { filter_name: fieldKey, filter_option: from, filter_option_end: to }]);
	}
</script>

<aside class="bg-brand-800 rounded-xl border border-neutral-600 p-4 min-w-[200px]">
	<h2 class="text-sm font-semibold text-neutral-300 uppercase tracking-wider mb-4">Filters</h2>

	{#each filterDefs as filter (filter.filtername)}
		{#if filter.type === 'option'}
			{#if optionValues[filter.filtername]?.length > 0}
				<div class="mb-5">
					<h3 class="text-xs font-semibold text-neutral-300 uppercase mb-2">{label(filter.filtername)}</h3>
					<ul class="flex flex-col gap-1">
						{#each optionValues[filter.filtername] as opt}
							<li>
								<button
									class="w-full text-left text-sm px-2 py-1 rounded transition-colors duration-150
										{isActive(filter.filtername, opt)
											? 'bg-accent-500 text-white'
											: 'text-neutral-100 hover:bg-brand-700'}"
									on:click={() => toggle(filter.filtername, opt)}
								>
									{opt}
								</button>
							</li>
						{/each}
					</ul>
				</div>
			{/if}
		{:else if filter.type === 'range'}
			<div class="mb-5">
				<h3 class="text-xs font-semibold text-neutral-300 uppercase mb-2">{label(filter.filtername)}</h3>
				<div class="flex flex-col gap-2">
					<input
						type="date"
						placeholder="From"
						class="w-full text-sm px-2 py-1 rounded bg-brand-700 text-neutral-100 border border-neutral-600"
						value={rangeValues[filter.filtername]?.from ?? ''}
						on:change={(e) => updateRange(filter.filtername, inputValue(e), rangeValues[filter.filtername]?.to ?? '')}
					/>
					<input
						type="date"
						placeholder="To"
						class="w-full text-sm px-2 py-1 rounded bg-brand-700 text-neutral-100 border border-neutral-600"
						value={rangeValues[filter.filtername]?.to ?? ''}
						on:change={(e) => updateRange(filter.filtername, rangeValues[filter.filtername]?.from ?? '', inputValue(e))}
					/>
				</div>
			</div>
		{/if}
	{/each}

	{#if activeFilters.length > 0}
		<button
			class="w-full text-xs text-neutral-300 hover:text-accent-400 mt-2 underline"
			on:click={() => dispatch('change', [])}
		>
			Clear all filters
		</button>
	{/if}
</aside>
