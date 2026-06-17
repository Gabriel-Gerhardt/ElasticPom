# GAB-26 — Paper Link in Search Results: Design Document

## Audience & Tone

**Evidence gathered:**
- `README.md`: ElasticPom is described plainly as a "unified full-stack application for semantic search over scientific papers," ingesting OAI-PMH metadata into MongoDB/Elasticsearch. No marketing language, no persona section, no brand guide exists in the repo.
- Tech stack (`package.json`): SvelteKit + TypeScript + Tailwind, vanilla `vitest`/`@testing-library/svelte` for tests — a lean, conventional engineering stack with no UI component library (no Material/Radix/shadcn). Every visual element so far is hand-rolled with Tailwind utility classes.
- `tailwind.config.js` defines a small, deliberate palette: `brand` (900/800/700 — dark navy tones used for backgrounds/surfaces), `accent` (500/400 — a coral/red `#E94560` / `#FF6B81` used for emphasis and interactive elements), `neutral` (100/300/600 — text and border tones). There is no "success/info/warning" semantic color set, and no light-mode variant — the app is dark-themed only.
- `app.css` `@layer components` defines exactly three reusable primitives: `.btn-primary` (solid accent-colored pill/rounded button used for all primary actions — Search button in `SearchBar.svelte`/`NavBar.svelte`, Prev/Next in `Pagination.svelte`), `.card` (dark surface with rounded corners and a neutral border, used by `PaperCard.svelte`), and `.input-base`. There is no `.btn-secondary` or outline/ghost button defined anywhere.
- Copy tone across components is terse, functional, and lower-key: "Search papers...", "No papers found.", "Active filters:", "Clear all filters", "Prev"/"Next". No exclamation marks, no playful microcopy, no emoji. This reads as a utilitarian research/data tool, not a consumer/marketing product.
- `FilterSidebar.svelte` and `ActiveFilterBar.svelte` show the established interaction pattern for "chips"/tags: small pill-shaped elements (`rounded-full`, `text-xs`), accent-colored when "active"/selected, neutral/bordered otherwise.
- `PaperCard.svelte` (existing, untouched layout) is a dense metadata card: title, paperType badge (top-right pill, `bg-accent-500`), creators, date, description (clamped to 3 lines), and up to 3 subject tags (bordered neutral pills). It is information-dense and compact — designed for scanning many results quickly, not for showcasing a single item.
- The previous (disliked) `PaperLinkButton.svelte` reused `.btn-primary` directly — a large, solid accent-colored pill ("View official page") stacked below the card's tag row. Given `.btn-primary` is also used for the page's *primary* high-commitment actions (global Search, pagination Prev/Next), reusing it verbatim for a per-card secondary affordance is the most likely source of the "I don't like it" reaction: visually it competes with the card content and overstates the importance of an outbound link inside a dense, scannable list of many cards.

**Conclusion:** Audience is researchers/engineers scanning many compact result cards quickly (confidence: high, based on the dense card layout, plain copy tone, and lack of any consumer-facing chrome). Tone is plain, functional, utilitarian — no playful or marketing language anywhere in the app (confidence: high). The existing visual language is "dark navy surface + one accent color used sparingly for emphasis/selection + small text," and large solid-accent buttons are reserved for primary, page-level actions, not per-item links (confidence: medium-high, inferred from where `.btn-primary` is actually used today).

## Goals & Scope

- Replace the old `PaperLinkButton.svelte` with a new component that reads as a **secondary, per-item affordance** — not a primary CTA — consistent with the card's information density and the app's existing chip/tag visual vocabulary.
- Preserve all acceptance criteria from GAB-26:
  - A button exists in every paper card.
  - The button only renders when the paper has a resolvable link (reuse existing `getPaperLink` logic — backend/derivation untouched).
  - Implemented as its own component (not inlined into `PaperCard.svelte`).
- Use only the existing Tailwind color tokens (`brand`, `accent`, `neutral`) — no new colors introduced.
- No new dependencies (no icon library). Use a small inline SVG or Unicode glyph consistent with how `Pagination.svelte` uses Unicode arrows (`&larr;`, `&rarr;`) for affordances — this matches an established low-dependency pattern in the codebase.
- Out of scope: backend, `paperLink.ts` derivation logic (already correct and tested), `PaperCard.svelte`'s existing fields (title/creators/date/description/subjects).

## Architecture & Components

```
PaperList.svelte
  └─ PaperCard.svelte (for each paper)
        ├─ existing fields (title, badge, creators, date, description, subjects)
        └─ PaperLinkButton.svelte   (new design; only rendered if paperLink is non-null)
```

No change to the data flow: `PaperCard.svelte` still computes `paperLink = getPaperLink(paper.identifier)` and conditionally renders `<PaperLinkButton link={paperLink} />`. The only changes are:

1. **`PaperLinkButton.svelte`** — visual redesign (described below). Props (`link: string | null`) and the "render nothing if link is null" contract stay the same, since that contract is correct and well tested.
2. **`app.css`** — add one new reusable primitive, `.btn-link` (see below), following the existing `@layer components` pattern (`.btn-primary`, `.card`, `.input-base`).
3. **`PaperCard.svelte`** — move the link button from a full-width block below the tags into the card's top-right header row, next to the `paperType` badge, OR keep it bottom-aligned but restyle — see Interactions & Flows for the final placement decision and rationale.

### New Tailwind primitive: `.btn-link`

Added to `app.css` `@layer components`, alongside the existing three primitives:

```css
.btn-link {
	@apply inline-flex items-center gap-1.5 text-xs font-medium text-neutral-300
		border border-neutral-600 rounded-full px-3 py-1
		hover:border-accent-500 hover:text-accent-400
		transition-colors duration-200;
}
```

Rationale: this mirrors the exact visual recipe already used for subject tags in `PaperCard.svelte` (`text-xs bg-brand-700 text-neutral-300 rounded px-2 py-0.5 border border-neutral-600`) and filter chips in `FilterSidebar.svelte`/`ActiveFilterBar.svelte` (`rounded-full`, accent on interaction) — so the link button reads as "part of the card's metadata chip row" rather than a competing primary CTA. On hover/focus it shifts to the accent color, signalling interactivity without using a solid accent fill (which is reserved, by existing convention, for primary actions and "selected" states).

### `PaperLinkButton.svelte` (new implementation)

```svelte
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
```

Notes:
- Keeps the existing prop contract (`link: string | null`) and the existing accessibility-relevant attributes (`target="_blank"`, `rel="noopener noreferrer"`) from the old implementation — those were correct.
- Adds an `aria-label` for clarity (the icon is decorative/`aria-hidden`, and the visible text "View paper" is shorter/lower-key than "View official page," matching the terse copy tone elsewhere — "No papers found.", "Clear all filters").
- Uses an inline "external link" SVG glyph (no icon library dependency) sized to `text-xs` line height, consistent with the codebase's existing zero-icon-dependency approach (`Pagination.svelte` uses HTML entities for arrows).
- Copy label text is kept as a separate `<span>` so tests can target by accessible name; visible text "View paper" is shorter than the old "View official page" to fit comfortably in the smaller chip-style button.

### `PaperCard.svelte` placement change

Old: the button was rendered full-width-ish in its own `<div class="mt-3">` block below the subject tags — visually a separate "section," heavy relative to the rest of the card.

New: render it **inline in the tag row**, after the subject chips, as the last chip-like element — reinforcing that it's metadata about the paper (like a subject tag), not a standalone call-to-action:

```svelte
{#if topSubjects.length > 0 || paperLink}
	<div class="flex flex-wrap gap-1.5 items-center mt-3">
		{#each topSubjects as subject}
			<span class="text-xs bg-brand-700 text-neutral-300 rounded px-2 py-0.5 border border-neutral-600">
				{subject}
			</span>
		{/each}
		{#if paperLink}
			<PaperLinkButton link={paperLink} />
		{/if}
	</div>
{/if}
```

This removes the old separate `mt-3` wrapper entirely and merges the link button into the same flex row as the subject tags, so it visually belongs to the card's metadata rather than floating as an isolated CTA block.

## Interactions & Flows

```mermaid
flowchart TD
    A[PaperCard receives paper] --> B{getPaperLink(paper.identifier)}
    B -->|null| C[Tag row renders only subjects, or nothing if none]
    B -->|url string| D[Tag row renders subjects + PaperLinkButton chip]
    D --> E[User clicks chip]
    E --> F[Opens link in new tab, rel=noopener noreferrer]
```

Hover/focus state (chip): default = neutral border + neutral-300 text → hover/focus = accent-500 border + accent-400 text. This single state transition is the only interactive feedback needed; no loading/disabled state applies since it's a plain anchor tag.

No new routes, no new API calls, no new state management — this is a pure presentational change confined to two files (`app.css`, `PaperLinkButton.svelte`) plus a small layout tweak in `PaperCard.svelte`.

## Open Questions / Assumptions

1. **Assumption (medium confidence):** "View paper" is used as the new accessible/visible name instead of the old "View official page." This shortens the label to fit a compact chip and matches the terse tone elsewhere, but it is a copy change beyond pure visual redesign. If the user wants to preserve the exact original wording, the label can be reverted to "View official page" without any other change to the design.
2. **Assumption (medium confidence):** Placing the link chip inline with subject tags (rather than as a separate row) is preferred for visual consistency, but if subjects are absent and only the link chip would render, it will sit alone in that row — this is intentional and consistent (same as a card with one subject tag and nothing else), not treated as a special case.
3. No icon library exists in the project; the design uses a small inline SVG rather than introducing `lucide-svelte` or similar, to avoid adding a new dependency for one icon. If the team has a preferred icon set in mind, this should be flagged before implementation.
4. No dark/light theme toggle exists in the app, so no alternate palette was designed for a light mode.
