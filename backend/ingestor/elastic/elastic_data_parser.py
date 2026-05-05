class ElasticDataParser:
    def __init__(self, index_name):
        self.index_name = index_name

    def parse_row(self, row):
        return row.dropna().to_dict()

    def generate_actions(self, chunk):
        for _, row in chunk.iterrows():
            source = self.mount_source(row)
            yield {
                "_id": row.get("id"),
                "_index": self.index_name,
                "_source": source,
            }

    def mount_source(self, row):
        authors = self.append_authors_name(row.get("authors_parsed"))
        categories_parsed = self.break_categories(row.get("categories"))


        source = self.parse_row(row)
        source["categories_parsed"] = categories_parsed
        source["authors_parsed"] = authors

        return source

    def append_authors_name(self, authors):
        new_authors = []
        if authors is None or len(authors) == 0:
            return None
        for author in authors:
            full_name =  author[0] + " " + author[1] + author[2]
            new_authors.append(full_name)
        return new_authors

    def break_categories(self, categories):
        categories = categories.split(" ")
        return categories