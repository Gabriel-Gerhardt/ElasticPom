<script lang="ts">
	import type { FilterRequest } from '$lib/types/api';
	import { createEventDispatcher } from 'svelte';

	export let filters: FilterRequest[];

	const dispatch = createEventDispatcher<{ remove: FilterRequest; clear: void }>();
</script>

{#if filters.length > 0}
	<div class="flex flex-wrap gap-2 items-center">
		<span class="text-xs text-neutral-300">Active filters:</span>
		{#each filters as f}
			<span class="inline-flex items-center gap-1 bg-accent-500 text-white text-xs rounded-full px-3 py-1">
				<span class="font-medium">{f.filter_name}:</span>
				<span>{f.filter_option}</span>
				<button
					class="ml-1 hover:text-neutral-100 text-white opacity-80 hover:opacity-100 leading-none"
					on:click={() => dispatch('remove', f)}
					aria-label="Remove filter"
				>
					&times;
				</button>
			</span>
		{/each}
		<button
			class="text-xs text-neutral-300 hover:text-accent-400 underline"
			on:click={() => dispatch('clear')}
		>
			Clear all
		</button>
	</div>
{/if}
