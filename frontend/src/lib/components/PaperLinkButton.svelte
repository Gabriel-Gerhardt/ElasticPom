<script lang="ts">
	export let link: string | null;
</script>

{#if link}
	<a
		class="btn-link"
		href={link}
		target="_blank"
		rel="noopener noreferrer"
		aria-label="View official paper page"
	>
		<svg
			class="w-3.5 h-3.5"
			viewBox="0 0 24 24"
			fill="none"
			stroke="currentColor"
			stroke-width="2"
			aria-hidden="true"
		>
			<path
				stroke-linecap="round"
				stroke-linejoin="round"
				d="M10 14 21 3m0 0h-6m6 0v6M19 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h6"
			/>
		</svg>
		<span>View paper</span>
	</a>
{/if}
