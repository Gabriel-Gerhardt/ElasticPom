from utils.time_converter import TimeConverter
from utils.paper_parser import PaperParser

class ElasticDataParser:
    def __init__(self, index_name, time_converter: TimeConverter, paper_parser: PaperParser):
        self.index_name = index_name
        self.time_converter = time_converter
        self.paper_parser = paper_parser


    def parse_row(self, row):
        return row.dropna().to_dict()

    def generate_actions(self, chunk):
        for _, row in chunk.iterrows():
            paper_id = self.paper_parser.normalize_paper_id(str(row.get("id")))
            source = self.mount_source(row, paper_id)
            yield {
                "_id": paper_id,
                "_index": self.index_name,
                "_source": source,
            }

    def mount_source(self, row, paper_id):
        authors = self.append_authors_name(row.get("authors_parsed"))
        categories_parsed = self.break_categories(row.get("categories"))
        created_date = self.get_creation_date(row.get("versions"))
        source = self.parse_row(row)
        source["categories_parsed"] = categories_parsed
        source["authors_parsed"] = authors
        source["created_date"] = created_date
        source["id"] = paper_id

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

    def get_creation_date(self, versions):
        if not versions:
            return None
        return self.time_converter.rfc1123_to_iso(versions[0]["created"])
