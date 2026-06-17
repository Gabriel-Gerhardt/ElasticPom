<script lang="ts">
	import type { PaperDto } from '$lib/types/paper';
	import { getPaperLink } from '$lib/utils/paperLink';
	import PaperLinkButton from './PaperLinkButton.svelte';

	export let paper: PaperDto;

	$: displayDate = paper.date ?? paper.datestamp ?? null;
	$: creatorsText = paper.creators?.join(', ') || '';
	$: topSubjects = paper.subjects?.slice(0, 3) ?? [];
	$: paperLink = getPaperLink(paper.identifier);
</script>

<article class="card hover:border-neutral-300 transition-colors duration-200">
	<div class="flex items-start justify-between gap-3 mb-2">
		<h2 class="text-base font-semibold text-neutral-100 leading-snug line-clamp-2 flex-1">
			{paper.title || 'Untitled'}
		</h2>
		{#if paper.paperType}
			<span class="shrink-0 text-xs font-medium bg-accent-500 text-white rounded-full px-2.5 py-0.5">
				{paper.paperType}
			</span>
		{/if}
	</div>

	{#if creatorsText}
		<p class="text-sm text-neutral-300 mb-1">{creatorsText}</p>
	{/if}

	{#if displayDate}
		<p class="text-xs text-neutral-300 mb-2">{displayDate}</p>
	{/if}

	{#if paper.description}
		<p class="text-sm text-neutral-100 line-clamp-3 mb-3">{paper.description}</p>
	{/if}

	{#if topSubjects.length > 0}
		<div class="flex flex-wrap gap-1.5">
			{#each topSubjects as subject}
				<span class="text-xs bg-brand-700 text-neutral-300 rounded px-2 py-0.5 border border-neutral-600">
					{subject}
				</span>
			{/each}
		</div>
	{/if}

	{#if paperLink}
		<div class="mt-3">
			<PaperLinkButton link={paperLink} />
		</div>
	{/if}
</article>
