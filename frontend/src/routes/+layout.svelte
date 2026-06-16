<script lang="ts">
	import '../app.css';
	import NavBar from '$lib/components/NavBar.svelte';
	import { page } from '$app/stores';

	$: isHome = $page.url.pathname === '/';
</script>

<div class="min-h-screen flex flex-col">
	{#if !isHome}
		<NavBar />
	{/if}
	<main class="flex-1">
		<slot />
	</main>
</div>
