from utils.time_converter import TimeConverter
from utils.paper_parser import PaperParser

class ElasticDataParser:
    def __init__(self, index_name, time_converter: TimeConverter, paper_parser: PaperParser):
        self.index_name = index_name
        self.time_converter = time_converter
        self.paper_parser = paper_parser

    def generate_actions(self, chunk):
        for paper in chunk:
            source = self.mount_source(paper)
            yield {
                "_id": paper["identifier"],
                "_index": self.index_name,
                "_source": source,
            }

    def mount_source(self, paper: dict) -> dict:
        source = {
            k: v for k, v in paper.items()
            if v is not None and v != [] and k != "paper_id"
        }

        if "datestamp" in source:
            source["datestamp"] = self.time_converter.to_iso(source["datestamp"])
        if "date" in source:
            source["date"] = self.time_converter.to_iso(source["date"])

        source["id"] = paper["identifier"]
        return source