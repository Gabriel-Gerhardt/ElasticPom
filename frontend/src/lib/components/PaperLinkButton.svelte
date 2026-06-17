<script lang="ts">
	export let link: string | null;
</script>

{#if link}
	<a class="btn-primary text-sm inline-block" href={link} target="_blank" rel="noopener noreferrer">
		View official page
	</a>
{/if}
