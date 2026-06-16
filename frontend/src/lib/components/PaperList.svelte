<script lang="ts">
	import type { PaperDto } from '$lib/types/paper';
	import PaperCard from './PaperCard.svelte';

	export let papers: PaperDto[];
</script>

{#if papers.length === 0}
	<p class="text-neutral-300 text-center py-12">No papers found.</p>
{:else}
	<ul class="flex flex-col gap-4">
		{#each papers as paper (paper.id ?? paper.paperId)}
			<li>
				<PaperCard {paper} />
			</li>
		{/each}
	</ul>
{/if}
