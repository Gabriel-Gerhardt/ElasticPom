<script lang="ts">
	import type { FilterDefinition, FilterRequest } from '$lib/types/api';
	import { getFilters } from '$lib/api/filters';
	import { createEventDispatcher, onMount } from 'svelte';

	export let papers: import('$lib/types/paper').PaperDto[];
	export let activeFilters: FilterRequest[];

	const dispatch = createEventDispatcher<{ change: FilterRequest[] }>();

	let filterDefs: FilterDefinition[] = [];

	onMount(async () => {
		try {
			filterDefs = await getFilters();
		} catch {
			filterDefs = [];
		}
	});

	// Strip ".keyword" suffix to find the DTO field name
	function dtoKey(filtername: string): string {
		return filtername.endsWith('.keyword') ? filtername.slice(0, -'.keyword'.length) : filtername;
	}

	function labelFor(filtername: string): string {
		const key = dtoKey(filtername);
		return key.charAt(0).toUpperCase() + key.slice(1);
	}

	type OptionMap = Record<string, string[]>;

	$: optionMap = buildOptions(papers, filterDefs);

	function buildOptions(papers: import('$lib/types/paper').PaperDto[], defs: FilterDefinition[]): OptionMap {
		const map: OptionMap = {};
		for (const def of defs) {
			if (def.type !== 'option') continue;
			const key = dtoKey(def.filtername);
			const vals = new Set<string>();
			for (const p of papers) {
				const raw = (p as unknown as Record<string, unknown>)[key];
				if (Array.isArray(raw)) {
					for (const v of raw) {
						if (typeof v === 'string' && v) vals.add(v);
					}
				} else if (typeof raw === 'string' && raw) {
					vals.add(raw);
				}
			}
			map[def.filtername] = [...vals].sort();
		}
		return map;
	}

	function isActive(filterName: string, option: string): boolean {
		return activeFilters.some(f => f.filter_name === filterName && f.filter_option === option);
	}

	function toggle(filterName: string, option: string) {
		if (isActive(filterName, option)) {
			dispatch('change', activeFilters.filter(f => !(f.filter_name === filterName && f.filter_option === option)));
		} else {
			dispatch('change', [...activeFilters, { filter_name: filterName, filter_option: option }]);
		}
	}

	function getRangeValue(filterName: string): string {
		return activeFilters.find(f => f.filter_name === filterName)?.filter_option ?? '';
	}

	function handleRangeChange(filterName: string, value: string) {
		const rest = activeFilters.filter(f => f.filter_name !== filterName);
		if (value) {
			dispatch('change', [...rest, { filter_name: filterName, filter_option: value }]);
		} else {
			dispatch('change', rest);
		}
	}
</script>

<aside class="bg-brand-800 rounded-xl border border-neutral-600 p-4 min-w-[200px]">
	<h2 class="text-sm font-semibold text-neutral-300 uppercase tracking-wider mb-4">Filters</h2>

	{#each filterDefs as def (def.filtername)}
		{#if def.type === 'option'}
			{#if optionMap[def.filtername]?.length > 0}
				<div class="mb-5">
					<h3 class="text-xs font-semibold text-neutral-300 uppercase mb-2">{labelFor(def.filtername)}</h3>
					<ul class="flex flex-col gap-1">
						{#each optionMap[def.filtername] as opt}
							<li>
								<button
									class="w-full text-left text-sm px-2 py-1 rounded transition-colors duration-150
										{isActive(def.filtername, opt)
											? 'bg-accent-500 text-white'
											: 'text-neutral-100 hover:bg-brand-700'}"
									on:click={() => toggle(def.filtername, opt)}
								>
									{opt}
								</button>
							</li>
						{/each}
					</ul>
				</div>
			{/if}
		{:else if def.type === 'range'}
			<div class="mb-5">
				<h3 class="text-xs font-semibold text-neutral-300 uppercase mb-2">{labelFor(def.filtername)}</h3>
				<input
					type="date"
					class="w-full text-sm px-2 py-1 rounded bg-brand-700 text-neutral-100 border border-neutral-600"
					value={getRangeValue(def.filtername)}
					on:change={(e) => handleRangeChange(def.filtername, e.currentTarget.value)}
				/>
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
