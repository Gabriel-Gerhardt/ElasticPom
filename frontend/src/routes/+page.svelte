<script lang="ts">
	import SearchBar from '$lib/components/SearchBar.svelte';
</script>

<svelte:head>
	<title>ElasticPom — Academic Paper Search</title>
</svelte:head>

<div class="min-h-screen flex flex-col items-center justify-center px-4 bg-brand-900">
	<!-- Hero -->
	<div class="text-center mb-10">
		<h1 class="text-5xl font-bold text-white mb-3 tracking-tight">
			Elastic<span class="text-accent-500">Pom</span>
		</h1>
		<p class="text-neutral-300 text-lg max-w-md mx-auto">
			Discover academic papers with powerful full-text search and smart filtering.
		</p>
	</div>

	<!-- Search bar -->
	<SearchBar />

	<p class="mt-6 text-neutral-300 text-sm">
		Or <a href="/results" class="text-accent-400 hover:underline">browse most relevant papers</a>
	</p>
</div>
