export interface FilterRequest {
	filter_name: string;
	filter_option: string;
}

export type FilterType = 'option' | 'range';

export interface FilterDefinition {
	filtername: string;
	order: number;
	type: FilterType;
}

export interface SearchRequest {
	query: string;
	pageSize: number;
	page: number;
	filters: FilterRequest[];
}

export interface ApiError {
	message: string;
	status?: number;
}
