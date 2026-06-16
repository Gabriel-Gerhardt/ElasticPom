export interface UniqueField {
	name: string;
	data: string;
}

export interface PaperDto {
	id: string;
	paperId: string;
	datestamp: string | null;
	title: string;
	creators: string[];
	subjects: string[];
	description: string;
	publisher: string;
	contributors: string[];
	date: string | null;
	type: string;
	format: string;
	identifier: string;
	source: string;
	language: string;
	relations: string[];
	coverage: string;
	rights: string;
	paperType: string;
	uniqueFields: UniqueField[];
}
