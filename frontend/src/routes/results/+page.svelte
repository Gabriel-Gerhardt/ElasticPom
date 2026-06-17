<script lang="ts">
	import { page } from '$app/stores';
	import { onMount } from 'svelte';
	import { getMostRelevant, searchByQuery } from '$lib/api/papers';
	import type { PaperDto } from '$lib/types/paper';
	import type { FilterRequest } from '$lib/types/api';

	import PaperList from '$lib/components/PaperList.svelte';
	import FilterSidebar from '$lib/components/FilterSidebar.svelte';
	import ActiveFilterBar from '$lib/components/ActiveFilterBar.svelte';
	import Pagination from '$lib/components/Pagination.svelte';
	import LoadingSpinner from '$lib/components/LoadingSpinner.svelte';
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';

	const PAGE_SIZE = 10;

	let papers: PaperDto[] = [];
	let loading = true;
	let error = '';
	let currentPage = 0;
	let activeFilters: FilterRequest[] = [];

	$: query = $page.url.searchParams.get('q') ?? '';

	// Re-fetch whenever the URL query changes
	$: if (query !== undefined) {
		currentPage = 0;
		activeFilters = [];
		fetchPapers();
	}

	async function fetchPapers() {
		loading = true;
		error = '';
		try {
			if (query) {
				papers = await searchByQuery(query, PAGE_SIZE, currentPage, activeFilters);
			} else {
				papers = await getMostRelevant(PAGE_SIZE, currentPage);
			}
		} catch (e: unknown) {
			error = (e as { message?: string }).message ?? 'An unexpected error occurred.';
			papers = [];
		} finally {
			loading = false;
		}
	}

	function handleFiltersChange(e: CustomEvent<FilterRequest[]>) {
		activeFilters = e.detail;
		currentPage = 0;
		fetchPapers();
	}

	function handleFilterRemove(e: CustomEvent<FilterRequest>) {
		activeFilters = activeFilters.filter(
			f => !(f.filter_name === e.detail.filter_name && f.filter_option === e.detail.filter_option)
		);
		currentPage = 0;
		fetchPapers();
	}

	function handleFilterClear() {
		activeFilters = [];
		currentPage = 0;
		fetchPapers();
	}

	function handlePageChange(e: CustomEvent<number>) {
		currentPage = e.detail;
		fetchPapers();
	}

	onMount(() => {
		fetchPapers();
	});
</script>

<svelte:head>
	<title>{query ? `"${query}" — ElasticPom` : 'Browse — ElasticPom'}</title>
</svelte:head>

<div class="max-w-7xl mx-auto px-4 py-6">
	<div class="mb-4">
		{#if query}
			<h1 class="text-xl font-semibold text-neutral-100">
				Results for <span class="text-accent-400">"{query}"</span>
			</h1>
		{:else}
			<h1 class="text-xl font-semibold text-neutral-100">Most Relevant Papers</h1>
		{/if}
	</div>

	{#if error}
		<ErrorBanner message={error} />
	{/if}

	<div class="flex gap-6 items-start">
		<!-- Sidebar -->
		<div class="shrink-0 w-52 sticky top-4">
			<FilterSidebar
				{query}
				{activeFilters}
				on:change={handleFiltersChange}
			/>
		</div>

		<!-- Main content -->
		<div class="flex-1 min-w-0">
			<div class="mb-4">
				<ActiveFilterBar
					filters={activeFilters}
					on:remove={handleFilterRemove}
					on:clear={handleFilterClear}
				/>
			</div>

			{#if loading}
				<LoadingSpinner />
			{:else}
				<PaperList {papers} />
				<Pagination
					page={currentPage}
					pageSize={PAGE_SIZE}
					total={papers.length}
					on:change={handlePageChange}
				/>
			{/if}
		</div>
	</div>
</div>
