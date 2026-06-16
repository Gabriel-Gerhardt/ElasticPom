export interface FilterRequest {
	filter_name: string;
	filter_option: string;
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
