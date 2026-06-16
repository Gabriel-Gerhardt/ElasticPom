import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/svelte';
import ErrorBanner from './ErrorBanner.svelte';

describe('ErrorBanner', () => {
	it('renders the error message', () => {
		const { getByText } = render(ErrorBanner, {
			props: { message: 'Something went wrong.' }
		});
		expect(getByText('Something went wrong.')).toBeInTheDocument();
	});

	it('renders the "Error:" prefix label', () => {
		const { getByText } = render(ErrorBanner, {
			props: { message: 'Network error.' }
		});
		expect(getByText('Error:')).toBeInTheDocument();
	});

	it('has role="alert" for accessibility', () => {
		const { getByRole } = render(ErrorBanner, {
			props: { message: 'Access denied.' }
		});
		expect(getByRole('alert')).toBeInTheDocument();
	});

	it('renders a different error message', () => {
		const { getByText } = render(ErrorBanner, {
			props: { message: 'Failed to fetch papers: Service Unavailable' }
		});
		expect(getByText('Failed to fetch papers: Service Unavailable')).toBeInTheDocument();
	});

	it('displays error in a styled container', () => {
		const { getByRole } = render(ErrorBanner, {
			props: { message: 'Test error' }
		});
		const alert = getByRole('alert');
		expect(alert).toBeInTheDocument();
		// Verify it has the error-related Tailwind classes
		expect(alert.className).toContain('red');
	});
});
