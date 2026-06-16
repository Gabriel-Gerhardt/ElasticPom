<script lang="ts">
	import { goto } from '$app/navigation';

	let query = '';

	function handleSubmit() {
		const q = query.trim();
		if (q) goto(`/results?q=${encodeURIComponent(q)}`);
	}

	function handleKey(e: KeyboardEvent) {
		if (e.key === 'Enter') handleSubmit();
	}
</script>

<div class="flex gap-3 w-full max-w-2xl">
	<input
		class="input-base text-lg py-3"
		type="text"
		placeholder="Search for papers, authors, topics..."
		bind:value={query}
		on:keydown={handleKey}
		autofocus
	/>
	<button class="btn-primary text-lg py-3 px-8 shrink-0" on:click={handleSubmit}>Search</button>
</div>
