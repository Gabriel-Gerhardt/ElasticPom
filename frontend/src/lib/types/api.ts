export interface FilterRequest {
	filter_name: string;
	filter_option: string;
	filter_option_end?: string;
}

export interface FilterDto {
	filtername: string;
	order: number;
	type: 'option' | 'range';
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
