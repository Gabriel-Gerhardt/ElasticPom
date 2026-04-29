class ElasticDataParser:
    def __init__(self, index_name):
        self.index_name = index_name

    def parse_row(self, row):
        return row.dropna().to_dict()

    def generate_actions(self, chunk):
        for _, row in chunk.iterrows():
            authors = row.get("authors_parsed")
            authors = self.append_authors_name(authors)
            source = self.parse_row(row)
            source["authors_parsed"] = authors
            yield {
                "_id": row.get("id"),
                "_index": self.index_name,
                "_source": source,
            }

    def append_authors_name(self, authors):
        new_authors = []
        if authors is None or len(authors) == 0:
            return None
        for author in authors:
            full_name =  author[0] + " " + author[1] + author[2]
            new_authors.append(full_name)
        return new_authors
