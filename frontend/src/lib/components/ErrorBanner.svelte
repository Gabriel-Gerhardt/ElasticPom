<script lang="ts">
	export let message: string;
</script>

<div class="bg-red-900/40 border border-red-500 text-red-300 rounded-lg px-4 py-3 text-sm" role="alert">
	<span class="font-semibold">Error:</span> {message}
</div>
