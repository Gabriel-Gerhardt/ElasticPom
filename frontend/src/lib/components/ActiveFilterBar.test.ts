import { describe, it, expect, vi } from 'vitest';
import { render, fireEvent } from '@testing-library/svelte';
import ActiveFilterBar from './ActiveFilterBar.svelte';
import type { FilterRequest } from '$lib/types/api';

describe('ActiveFilterBar', () => {
	const filters: FilterRequest[] = [
		{ filter_name: 'language', filter_option: 'en' },
		{ filter_name: 'type', filter_option: 'article' }
	];

	it('renders nothing visible when filters array is empty', () => {
		const { container, queryByText } = render(ActiveFilterBar, { props: { filters: [] } });
		// When filters is empty, the {#if} block renders nothing — no "Active filters:" label
		expect(queryByText('Active filters:')).not.toBeInTheDocument();
		// And no buttons or chips
		expect(container.querySelector('button')).toBeNull();
		expect(container.querySelector('span')).toBeNull();
	});

	it('renders "Active filters:" label when filters exist', () => {
		const { getByText } = render(ActiveFilterBar, { props: { filters } });
		expect(getByText('Active filters:')).toBeInTheDocument();
	});

	it('renders a chip for each filter', () => {
		const { getByText } = render(ActiveFilterBar, { props: { filters } });
		expect(getByText('language:')).toBeInTheDocument();
		expect(getByText('en')).toBeInTheDocument();
		expect(getByText('type:')).toBeInTheDocument();
		expect(getByText('article')).toBeInTheDocument();
	});

	it('renders remove buttons for each filter chip', () => {
		const { getAllByLabelText } = render(ActiveFilterBar, { props: { filters } });
		const removeBtns = getAllByLabelText('Remove filter');
		expect(removeBtns).toHaveLength(2);
	});

	it('renders "Clear all" button', () => {
		const { getByText } = render(ActiveFilterBar, { props: { filters } });
		expect(getByText('Clear all')).toBeInTheDocument();
	});

	it('emits "remove" event with the correct filter when × button is clicked', async () => {
		const { getAllByLabelText, component } = render(ActiveFilterBar, { props: { filters } });
		const removeHandler = vi.fn();
		component.$on('remove', removeHandler);

		const removeBtns = getAllByLabelText('Remove filter');
		await fireEvent.click(removeBtns[0]);

		expect(removeHandler).toHaveBeenCalledOnce();
		expect(removeHandler.mock.calls[0][0].detail).toEqual({
			filter_name: 'language',
			filter_option: 'en'
		});
	});

	it('emits "remove" event with the second filter when second × is clicked', async () => {
		const { getAllByLabelText, component } = render(ActiveFilterBar, { props: { filters } });
		const removeHandler = vi.fn();
		component.$on('remove', removeHandler);

		const removeBtns = getAllByLabelText('Remove filter');
		await fireEvent.click(removeBtns[1]);

		expect(removeHandler).toHaveBeenCalledOnce();
		expect(removeHandler.mock.calls[0][0].detail).toEqual({
			filter_name: 'type',
			filter_option: 'article'
		});
	});

	it('emits "clear" event when "Clear all" button is clicked', async () => {
		const { getByText, component } = render(ActiveFilterBar, { props: { filters } });
		const clearHandler = vi.fn();
		component.$on('clear', clearHandler);

		await fireEvent.click(getByText('Clear all'));

		expect(clearHandler).toHaveBeenCalledOnce();
	});

	it('renders a single filter correctly', () => {
		const singleFilter: FilterRequest[] = [{ filter_name: 'publisher', filter_option: 'MIT Press' }];
		const { getByText, getAllByLabelText } = render(ActiveFilterBar, {
			props: { filters: singleFilter }
		});
		expect(getByText('publisher:')).toBeInTheDocument();
		expect(getByText('MIT Press')).toBeInTheDocument();
		expect(getAllByLabelText('Remove filter')).toHaveLength(1);
	});
});
