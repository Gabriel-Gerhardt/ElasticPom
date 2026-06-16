from embedding.embedding_service import EmbeddingService
from utils.time_converter import TimeConverter
from utils.paper_parser import PaperParser


class ElasticDataParser:
    def __init__(self, index_name, time_converter: TimeConverter, paper_parser: PaperParser, embedding_service: EmbeddingService):
        self.index_name = index_name
        self.time_converter = time_converter
        self.paper_parser = paper_parser
        self.embedding_service = embedding_service

    def generate_actions(self, chunk):
        texts = [self._build_embed_text(p) for p in chunk]
        vectors = self.embedding_service.embed_batch(texts)

        for i, (paper, vector) in enumerate(zip(chunk, vectors)):
            source = self.mount_source(paper)
            if texts[i]:
                source["embed_paper"] = vector
            yield {
                "_id": paper["paper_id"],
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

        source["id"] = paper["paper_id"]
        return source

    @staticmethod
    def _build_embed_text(paper: dict) -> str:
        parts = [
            paper.get("title"),
            paper.get("description"),
            " ".join(paper.get("subjects") or []),
            " ".join(paper.get("creators") or []),
            paper.get("publisher"),
        ]
        return " ".join(p for p in parts if p)
