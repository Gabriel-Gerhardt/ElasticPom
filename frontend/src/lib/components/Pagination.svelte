<script lang="ts">
	import { createEventDispatcher } from 'svelte';

	export let page: number;
	export let pageSize: number;
	export let total: number; // total results in current page (we use this to detect last page)

	const dispatch = createEventDispatcher<{ change: number }>();

	$: isFirst = page === 0;
	$: isLast = total < pageSize;
</script>

<div class="flex items-center gap-4 justify-center mt-6">
	<button
		class="btn-primary disabled:opacity-40 disabled:cursor-not-allowed"
		disabled={isFirst}
		on:click={() => dispatch('change', page - 1)}
	>
		&larr; Prev
	</button>

	<span class="text-neutral-300 text-sm">Page {page + 1}</span>

	<button
		class="btn-primary disabled:opacity-40 disabled:cursor-not-allowed"
		disabled={isLast}
		on:click={() => dispatch('change', page + 1)}
	>
		Next &rarr;
	</button>
</div>
