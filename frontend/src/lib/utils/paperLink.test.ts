import { describe, it, expect } from 'vitest';
import { getPaperLink } from './paperLink';

describe('getPaperLink', () => {
	it('passes through a full https URL as-is', () => {
		expect(getPaperLink('https://example.com/paper/1')).toBe('https://example.com/paper/1');
	});

	it('passes through a full http URL as-is', () => {
		expect(getPaperLink('http://example.com/paper/1')).toBe('http://example.com/paper/1');
	});

	it('normalizes a "doi:" prefixed identifier', () => {
		expect(getPaperLink('doi:10.1234/test')).toBe('https://doi.org/10.1234/test');
	});

	it('normalizes a "doi:" prefix case-insensitively', () => {
		expect(getPaperLink('DOI:10.1234/test')).toBe('https://doi.org/10.1234/test');
	});

	it('normalizes a bare DOI', () => {
		expect(getPaperLink('10.1234/test')).toBe('https://doi.org/10.1234/test');
	});

	it('returns null for an empty string', () => {
		expect(getPaperLink('')).toBeNull();
	});

	it('returns null for null', () => {
		expect(getPaperLink(null)).toBeNull();
	});

	it('returns null for undefined', () => {
		expect(getPaperLink(undefined)).toBeNull();
	});

	it('returns null for unrecognizable garbage', () => {
		expect(getPaperLink('not-a-link-or-doi')).toBeNull();
	});

	it('trims leading/trailing whitespace around a full URL', () => {
		expect(getPaperLink('  https://example.com/paper/1  ')).toBe('https://example.com/paper/1');
	});

	it('trims leading/trailing whitespace around a bare DOI', () => {
		expect(getPaperLink('  10.1234/test  ')).toBe('https://doi.org/10.1234/test');
	});

	it('returns null for "doi:" with nothing after the prefix', () => {
		expect(getPaperLink('doi:')).toBeNull();
	});

	it('returns null for whitespace-only input', () => {
		expect(getPaperLink('   ')).toBeNull();
	});

	it('handles a "doi:" prefix followed by whitespace before the DOI', () => {
		expect(getPaperLink('doi: 10.1234/test')).toBe('https://doi.org/10.1234/test');
	});
});
