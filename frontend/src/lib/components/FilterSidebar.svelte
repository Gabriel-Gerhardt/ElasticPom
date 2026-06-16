<script lang="ts">
	import type { FilterRequest } from '$lib/types/api';
	import { createEventDispatcher } from 'svelte';

	export let papers: import('$lib/types/paper').PaperDto[];
	export let activeFilters: FilterRequest[];

	const dispatch = createEventDispatcher<{ change: FilterRequest[] }>();

	const FILTER_FIELDS: { key: string; label: string }[] = [
		{ key: 'language', label: 'Language' },
		{ key: 'type', label: 'Type' },
		{ key: 'paperType', label: 'Paper Type' },
		{ key: 'publisher', label: 'Publisher' }
	];

	type OptionMap = Record<string, string[]>;

	$: options = buildOptions(papers);

	function buildOptions(papers: import('$lib/types/paper').PaperDto[]): OptionMap {
		const map: OptionMap = {};
		for (const field of FILTER_FIELDS) {
			const vals = new Set<string>();
			for (const p of papers) {
				const raw = (p as unknown as Record<string, unknown>)[field.key];
				if (typeof raw === 'string' && raw) vals.add(raw);
			}
			map[field.key] = [...vals].sort();
		}
		return map;
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
</script>

<aside class="bg-brand-800 rounded-xl border border-neutral-600 p-4 min-w-[200px]">
	<h2 class="text-sm font-semibold text-neutral-300 uppercase tracking-wider mb-4">Filters</h2>

	{#each FILTER_FIELDS as field}
		{#if options[field.key]?.length > 0}
			<div class="mb-5">
				<h3 class="text-xs font-semibold text-neutral-300 uppercase mb-2">{field.label}</h3>
				<ul class="flex flex-col gap-1">
					{#each options[field.key] as opt}
						<li>
							<button
								class="w-full text-left text-sm px-2 py-1 rounded transition-colors duration-150
									{isActive(field.key, opt)
										? 'bg-accent-500 text-white'
										: 'text-neutral-100 hover:bg-brand-700'}"
								on:click={() => toggle(field.key, opt)}
							>
								{opt}
							</button>
						</li>
					{/each}
				</ul>
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
