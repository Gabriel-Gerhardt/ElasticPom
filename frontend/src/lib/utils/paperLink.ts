// Bare DOI pattern, e.g. "10.1234/some.suffix" (DOI registrant codes are numeric, suffix is free-form).
const BARE_DOI_PATTERN = /^10\.\d{4,9}\/\S+$/;

/**
 * Resolves a paper's Dublin Core `identifier` into a clickable link, or null if it
 * isn't a recognizable URL/DOI. Recognizes: full http(s) URLs, "doi:<doi>", and bare DOIs.
 */
export function getPaperLink(identifier: string | null | undefined): string | null {
	const trimmed = identifier?.trim();
	if (!trimmed) return null;

	if (/^https?:\/\//i.test(trimmed)) return trimmed;

	const doi = trimmed.replace(/^doi:/i, '').trim();
	if (BARE_DOI_PATTERN.test(doi)) return `https://doi.org/${doi}`;

	return null;
}
