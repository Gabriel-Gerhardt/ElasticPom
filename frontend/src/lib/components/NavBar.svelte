<script lang="ts">
	import { goto } from '$app/navigation';

	let searchVal = '';

	function handleSearch() {
		if (searchVal.trim()) {
			goto(`/results?q=${encodeURIComponent(searchVal.trim())}`);
		}
	}

	function handleKey(e: KeyboardEvent) {
		if (e.key === 'Enter') handleSearch();
	}
</script>

<nav class="bg-brand-700 px-6 py-3 flex items-center gap-4 shadow-lg">
	<a href="/" class="text-xl font-bold text-white shrink-0 hover:text-accent-400 transition-colors">
		ElasticPom
	</a>

	<div class="flex-1 max-w-xl flex gap-2">
		<input
			class="input-base text-sm"
			type="text"
			placeholder="Search papers..."
			bind:value={searchVal}
			on:keydown={handleKey}
		/>
		<button class="btn-primary text-sm shrink-0" on:click={handleSearch}>Search</button>
	</div>
</nav>
