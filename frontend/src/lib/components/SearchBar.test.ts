import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, fireEvent } from '@testing-library/svelte';
import SearchBar from './SearchBar.svelte';

// The $app/navigation mock is provided via the alias in vite.config.ts
// We import the mocked goto so we can spy on it
import { goto } from '$app/navigation';

describe('SearchBar', () => {
	beforeEach(() => {
		vi.mocked(goto).mockClear();
	});

	it('renders a text input', () => {
		const { getByRole } = render(SearchBar);
		expect(getByRole('textbox')).toBeInTheDocument();
	});

	it('renders a Search button', () => {
		const { getByRole } = render(SearchBar);
		expect(getByRole('button', { name: 'Search' })).toBeInTheDocument();
	});

	it('has placeholder text on the input', () => {
		const { getByPlaceholderText } = render(SearchBar);
		expect(getByPlaceholderText('Search for papers, authors, topics...')).toBeInTheDocument();
	});

	it('navigates to /results?q=... when Search button is clicked with text', async () => {
		const { getByRole } = render(SearchBar);
		const input = getByRole('textbox');
		const button = getByRole('button', { name: 'Search' });

		await fireEvent.input(input, { target: { value: 'machine learning' } });
		await fireEvent.click(button);

		expect(goto).toHaveBeenCalledWith('/results?q=machine%20learning');
	});

	it('navigates with URL-encoded special characters', async () => {
		const { getByRole } = render(SearchBar);
		const input = getByRole('textbox');
		const button = getByRole('button', { name: 'Search' });

		await fireEvent.input(input, { target: { value: 'C++ algorithms' } });
		await fireEvent.click(button);

		expect(goto).toHaveBeenCalledWith('/results?q=C%2B%2B%20algorithms');
	});

	it('does NOT navigate when the query is empty', async () => {
		const { getByRole } = render(SearchBar);
		const button = getByRole('button', { name: 'Search' });

		await fireEvent.click(button);

		expect(goto).not.toHaveBeenCalled();
	});

	it('does NOT navigate when query is only whitespace', async () => {
		const { getByRole } = render(SearchBar);
		const input = getByRole('textbox');
		const button = getByRole('button', { name: 'Search' });

		await fireEvent.input(input, { target: { value: '   ' } });
		await fireEvent.click(button);

		expect(goto).not.toHaveBeenCalled();
	});

	it('navigates when Enter key is pressed in the input', async () => {
		const { getByRole } = render(SearchBar);
		const input = getByRole('textbox');

		await fireEvent.input(input, { target: { value: 'deep learning' } });
		await fireEvent.keyDown(input, { key: 'Enter', code: 'Enter' });

		expect(goto).toHaveBeenCalledWith('/results?q=deep%20learning');
	});

	it('does NOT navigate when a non-Enter key is pressed', async () => {
		const { getByRole } = render(SearchBar);
		const input = getByRole('textbox');

		await fireEvent.input(input, { target: { value: 'neural networks' } });
		await fireEvent.keyDown(input, { key: 'a', code: 'KeyA' });

		expect(goto).not.toHaveBeenCalled();
	});

	it('trims whitespace before navigating', async () => {
		const { getByRole } = render(SearchBar);
		const input = getByRole('textbox');
		const button = getByRole('button', { name: 'Search' });

		await fireEvent.input(input, { target: { value: '  transformer models  ' } });
		await fireEvent.click(button);

		expect(goto).toHaveBeenCalledWith('/results?q=transformer%20models');
	});
});
