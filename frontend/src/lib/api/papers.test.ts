import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { getMostRelevant, searchByQuery, hybridSearch, getFilterOptions } from './papers';

const mockPapers = [
	{
		id: '1',
		paperId: 'paper-1',
		datestamp: '2023-01-01',
		title: 'Test Paper One',
		creators: ['Alice Smith', 'Bob Jones'],
		subjects: ['Computer Science', 'AI'],
		description: 'A test paper about AI.',
		publisher: 'MIT Press',
		contributors: [],
		date: '2023',
		type: 'article',
		format: 'pdf',
		identifier: 'doi:10.1234/test',
		source: 'arXiv',
		language: 'en',
		relations: [],
		coverage: '',
		rights: 'open',
		paperType: 'research',
		uniqueFields: []
	}
];

describe('getMostRelevant', () => {
	beforeEach(() => {
		vi.stubGlobal('fetch', vi.fn());
	});

	afterEach(() => {
		vi.unstubAllGlobals();
	});

	it('calls the correct URL with default parameters', async () => {
		const mockFetch = vi.mocked(fetch);
		mockFetch.mockResolvedValueOnce({
			ok: true,
			json: async () => mockPapers
		} as Response);

		await getMostRelevant();

		expect(mockFetch).toHaveBeenCalledOnce();
		expect(mockFetch).toHaveBeenCalledWith('/api/papers/most-relevant/?page-size=10&page=0');
	});

	it('calls the correct URL with custom pageSize and page', async () => {
		const mockFetch = vi.mocked(fetch);
		mockFetch.mockResolvedValueOnce({
			ok: true,
			json: async () => mockPapers
		} as Response);

		await getMostRelevant(20, 3);

		expect(mockFetch).toHaveBeenCalledWith('/api/papers/most-relevant/?page-size=20&page=3');
	});

	it('returns parsed JSON on success', async () => {
		const mockFetch = vi.mocked(fetch);
		mockFetch.mockResolvedValueOnce({
			ok: true,
			json: async () => mockPapers
		} as Response);

		const result = await getMostRelevant();

		expect(result).toEqual(mockPapers);
		expect(result).toHaveLength(1);
		expect(result[0].title).toBe('Test Paper One');
	});

	it('throws an error object with message and status on non-ok response', async () => {
		const mockFetch = vi.mocked(fetch);
		mockFetch.mockResolvedValueOnce({
			ok: false,
			status: 503,
			statusText: 'Service Unavailable'
		} as Response);

		await expect(getMostRelevant()).rejects.toMatchObject({
			message: 'Failed to fetch papers: Service Unavailable',
			status: 503
		});
	});

	it('throws with correct status code on 404', async () => {
		const mockFetch = vi.mocked(fetch);
		mockFetch.mockResolvedValueOnce({
			ok: false,
			status: 404,
			statusText: 'Not Found'
		} as Response);

		await expect(getMostRelevant()).rejects.toMatchObject({
			status: 404
		});
	});
});

describe('searchByQuery', () => {
	beforeEach(() => {
		vi.stubGlobal('fetch', vi.fn());
	});

	afterEach(() => {
		vi.unstubAllGlobals();
	});

	it('calls the correct endpoint with POST method', async () => {
		const mockFetch = vi.mocked(fetch);
		mockFetch.mockResolvedValueOnce({
			ok: true,
			json: async () => mockPapers
		} as Response);

		await searchByQuery('machine learning');

		expect(mockFetch).toHaveBeenCalledOnce();
		const [url, options] = mockFetch.mock.calls[0];
		expect(url).toBe('/api/papers/search-by-query');
		expect(options?.method).toBe('POST');
	});

	it('sends correct Content-Type header', async () => {
		const mockFetch = vi.mocked(fetch);
		mockFetch.mockResolvedValueOnce({
			ok: true,
			json: async () => []
		} as Response);

		await searchByQuery('test query');

		const [, options] = mockFetch.mock.calls[0];
		expect((options?.headers as Record<string, string>)?.['Content-Type']).toBe('application/json');
	});

	it('sends correct request body with query, pageSize, page, and filters', async () => {
		const mockFetch = vi.mocked(fetch);
		mockFetch.mockResolvedValueOnce({
			ok: true,
			json: async () => mockPapers
		} as Response);

		const filters = [{ filter_name: 'language', filter_option: 'en' }];
		await searchByQuery('neural networks', 5, 2, filters);

		const [, options] = mockFetch.mock.calls[0];
		const body = JSON.parse(options?.body as string);
		expect(body).toEqual({
			query: 'neural networks',
			pageSize: 5,
			page: 2,
			filters: [{ filter_name: 'language', filter_option: 'en' }]
		});
	});

	it('sends empty filters array by default', async () => {
		const mockFetch = vi.mocked(fetch);
		mockFetch.mockResolvedValueOnce({
			ok: true,
			json: async () => []
		} as Response);

		await searchByQuery('deep learning');

		const [, options] = mockFetch.mock.calls[0];
		const body = JSON.parse(options?.body as string);
		expect(body.filters).toEqual([]);
	});

	it('returns parsed JSON on success', async () => {
		const mockFetch = vi.mocked(fetch);
		mockFetch.mockResolvedValueOnce({
			ok: true,
			json: async () => mockPapers
		} as Response);

		const result = await searchByQuery('AI');
		expect(result).toEqual(mockPapers);
	});

	it('throws an error object with message and status on non-ok response', async () => {
		const mockFetch = vi.mocked(fetch);
		mockFetch.mockResolvedValueOnce({
			ok: false,
			status: 500,
			statusText: 'Internal Server Error'
		} as Response);

		await expect(searchByQuery('error test')).rejects.toMatchObject({
			message: 'Search failed: Internal Server Error',
			status: 500
		});
	});

	it('defaults to pageSize=10 and page=0', async () => {
		const mockFetch = vi.mocked(fetch);
		mockFetch.mockResolvedValueOnce({
			ok: true,
			json: async () => []
		} as Response);

		await searchByQuery('default params');

		const [, options] = mockFetch.mock.calls[0];
		const body = JSON.parse(options?.body as string);
		expect(body.pageSize).toBe(10);
		expect(body.page).toBe(0);
	});

	it('propagates a network failure (rejected fetch) rather than swallowing it', async () => {
		const mockFetch = vi.mocked(fetch);
		mockFetch.mockRejectedValueOnce(new TypeError('Failed to fetch'));

		await expect(searchByQuery('network failure test')).rejects.toThrow('Failed to fetch');
	});
});

describe('hybridSearch', () => {
	beforeEach(() => {
		vi.stubGlobal('fetch', vi.fn());
	});

	afterEach(() => {
		vi.unstubAllGlobals();
	});

	it('calls the correct endpoint with POST method', async () => {
		const mockFetch = vi.mocked(fetch);
		mockFetch.mockResolvedValueOnce({
			ok: true,
			json: async () => mockPapers
		} as Response);

		await hybridSearch('machine learning');

		expect(mockFetch).toHaveBeenCalledOnce();
		const [url, options] = mockFetch.mock.calls[0];
		expect(url).toBe('/api/papers/hybrid-search');
		expect(options?.method).toBe('POST');
	});

	it('sends correct Content-Type header', async () => {
		const mockFetch = vi.mocked(fetch);
		mockFetch.mockResolvedValueOnce({
			ok: true,
			json: async () => []
		} as Response);

		await hybridSearch('test query');

		const [, options] = mockFetch.mock.calls[0];
		expect((options?.headers as Record<string, string>)?.['Content-Type']).toBe('application/json');
	});

	it('sends correct request body with query, pageSize, page, and filters', async () => {
		const mockFetch = vi.mocked(fetch);
		mockFetch.mockResolvedValueOnce({
			ok: true,
			json: async () => mockPapers
		} as Response);

		const filters = [{ filter_name: 'language', filter_option: 'en' }];
		await hybridSearch('neural networks', 5, 2, filters);

		const [, options] = mockFetch.mock.calls[0];
		const body = JSON.parse(options?.body as string);
		expect(body).toEqual({
			query: 'neural networks',
			pageSize: 5,
			page: 2,
			filters: [{ filter_name: 'language', filter_option: 'en' }]
		});
	});

	it('sends empty filters array by default', async () => {
		const mockFetch = vi.mocked(fetch);
		mockFetch.mockResolvedValueOnce({
			ok: true,
			json: async () => []
		} as Response);

		await hybridSearch('deep learning');

		const [, options] = mockFetch.mock.calls[0];
		const body = JSON.parse(options?.body as string);
		expect(body.filters).toEqual([]);
	});

	it('returns parsed JSON on success', async () => {
		const mockFetch = vi.mocked(fetch);
		mockFetch.mockResolvedValueOnce({
			ok: true,
			json: async () => mockPapers
		} as Response);

		const result = await hybridSearch('AI');
		expect(result).toEqual(mockPapers);
	});

	it('throws an error object with message and status on non-ok response', async () => {
		const mockFetch = vi.mocked(fetch);
		mockFetch.mockResolvedValueOnce({
			ok: false,
			status: 500,
			statusText: 'Internal Server Error'
		} as Response);

		await expect(hybridSearch('error test')).rejects.toMatchObject({
			message: 'Search failed: Internal Server Error',
			status: 500
		});
	});

	it('defaults to pageSize=10 and page=0', async () => {
		const mockFetch = vi.mocked(fetch);
		mockFetch.mockResolvedValueOnce({
			ok: true,
			json: async () => []
		} as Response);

		await hybridSearch('default params');

		const [, options] = mockFetch.mock.calls[0];
		const body = JSON.parse(options?.body as string);
		expect(body.pageSize).toBe(10);
		expect(body.page).toBe(0);
	});

	it('propagates a network failure (rejected fetch) rather than swallowing it', async () => {
		const mockFetch = vi.mocked(fetch);
		mockFetch.mockRejectedValueOnce(new TypeError('Failed to fetch'));

		await expect(hybridSearch('network failure test')).rejects.toThrow('Failed to fetch');
	});

	it('mirrors searchByQuery error handling exactly: same error object shape on non-ok response', async () => {
		const mockFetch = vi.mocked(fetch);
		mockFetch.mockResolvedValueOnce({
			ok: false,
			status: 503,
			statusText: 'Service Unavailable'
		} as Response);

		await expect(hybridSearch('mirrors error test')).rejects.toMatchObject({
			message: 'Search failed: Service Unavailable',
			status: 503
		});
	});
});

describe('getFilterOptions', () => {
	beforeEach(() => {
		vi.stubGlobal('fetch', vi.fn());
	});

	afterEach(() => {
		vi.unstubAllGlobals();
	});

	it('calls the correct endpoint with POST method', async () => {
		const mockFetch = vi.mocked(fetch);
		mockFetch.mockResolvedValueOnce({
			ok: true,
			json: async () => ['en', 'fr']
		} as Response);

		await getFilterOptions('machine learning', 'language', []);

		expect(mockFetch).toHaveBeenCalledOnce();
		const [url, options] = mockFetch.mock.calls[0];
		expect(url).toBe('/api/papers/filter-options');
		expect(options?.method).toBe('POST');
	});

	it('sends query, filter_name, and filters in the request body', async () => {
		const mockFetch = vi.mocked(fetch);
		mockFetch.mockResolvedValueOnce({
			ok: true,
			json: async () => ['en']
		} as Response);

		const filters = [{ filter_name: 'subjects', filter_option: 'ai' }];
		await getFilterOptions('neural networks', 'language', filters);

		const [, options] = mockFetch.mock.calls[0];
		const body = JSON.parse(options?.body as string);
		expect(body).toEqual({
			query: 'neural networks',
			filter_name: 'language',
			filters: [{ filter_name: 'subjects', filter_option: 'ai' }]
		});
	});

	it('defaults filters to an empty array', async () => {
		const mockFetch = vi.mocked(fetch);
		mockFetch.mockResolvedValueOnce({
			ok: true,
			json: async () => []
		} as Response);

		await getFilterOptions('', 'language');

		const [, options] = mockFetch.mock.calls[0];
		const body = JSON.parse(options?.body as string);
		expect(body.filters).toEqual([]);
	});

	it('returns parsed JSON on success', async () => {
		const mockFetch = vi.mocked(fetch);
		mockFetch.mockResolvedValueOnce({
			ok: true,
			json: async () => ['en', 'fr']
		} as Response);

		const result = await getFilterOptions('query', 'language', []);
		expect(result).toEqual(['en', 'fr']);
	});

	it('throws an error object with message and status on non-ok response', async () => {
		const mockFetch = vi.mocked(fetch);
		mockFetch.mockResolvedValueOnce({
			ok: false,
			status: 404,
			statusText: 'Not Found'
		} as Response);

		await expect(getFilterOptions('query', 'bad_field', [])).rejects.toMatchObject({
			message: 'Failed to fetch filter options: Not Found',
			status: 404
		});
	});
});
