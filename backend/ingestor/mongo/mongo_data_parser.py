from utils.paper_parser import PaperParser

class MongoDataParser:
    def __init__(self, paper_parser: PaperParser):
        self.paper_parser = paper_parser

    def generate_mongo(self, chunk):
        for paper in chunk:
            yield self.parse_document(paper)

    def parse_document(self, paper: dict) -> dict:
        return {
            "paper_id": paper["paper_id"],
            "datestamp": paper["datestamp"],
            "title": paper.get("title"),
            "creators": paper.get("creators", []),
            "subjects": paper.get("subjects", []),
            "description": paper.get("description"),
            "publisher": paper.get("publisher"),
            "contributors": paper.get("contributors", []),
            "date": paper.get("date"),
            "type": paper.get("type"),
            "identifier": paper.get("identifier"),
            "source": paper.get("source"),
            "language": paper.get("language"),
            "relations": paper.get("relations", []),
            "coverage": paper.get("coverage"),
            "rights": paper.get("rights"),
            "unique_fields": paper.get("unique_fields", []),
        }