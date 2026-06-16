from elastic.elastic_data_parser import ElasticDataParser
from elastic.elastic_integration import ElasticIntegration
from embedding.embedding_service import EmbeddingService
import time
from ingestor import Ingestor
from mongo.mongo_data_parser import MongoDataParser
from mongo.mongo_integration import MongoIntegration
from utils.paper_parser import PaperParser
from utils.time_converter import TimeConverter
from oaipmh_scythe import Scythe


def main():
    mapping = {
        "settings": {
            "index.mapping.coerce": False
        },
        "mappings": {
            "properties": {
                "id": {
                    "type": "keyword"
                },
                "datestamp": {
                    "type": "date",
                },
                "title": {
                    "type": "text",
                },
                "creators": {
                    "type": "text",
                },
                "subjects": {
                    "type": "text",
                },
                "description": {
                    "type": "text",
                },
                "publisher": {
                    "type": "text",
                },
                "contributors": {
                    "type": "text",
                },
                "date": {
                    "type": "date",
                },
                "type": {
                    "type": "keyword"
                },
                "format": {
                    "type": "keyword"
                },
                "identifier": {
                    "type": "keyword"
                },
                "source": {
                    "type": "text"
                },
                "language": {
                    "type": "keyword"
                },
                "relations": {
                    "type": "keyword"
                },
                "coverage": {
                    "type": "text"
                },

                "rights": {
                    "type": "text"
                },
                "embed_paper": {
                    "type": "dense_vector",
                    "dims": 384,
                    "index": True,
                    "similarity": "cosine",
                },
            }
        }
    }
    index = "paper"

    paper_parser = PaperParser()
    time_converter = TimeConverter()
    embedding_service = EmbeddingService()

    mongo_parser = MongoDataParser(paper_parser)
    mongo_integration = MongoIntegration(
        uri="mongodb://admin:password@localhost:27017",
        database="elasticpom",
        collection="Paper"
    )
    elastic_parser = ElasticDataParser(index, time_converter, paper_parser, embedding_service)
    elastic_integration = ElasticIntegration(elasticsearch_host="http://localhost:9200")
    elastic_integration.put_mapping(index, mapping)

    ingestor = Ingestor("https://oaipmh.arxiv.org/oai","oai_dc","cs:cs:IR")
    total_start = time.time()

    for chunk in ingestor.run():
        chunk_start = time.time()
        paper_list = paper_parser.parse(chunk)

        documents = mongo_parser.generate_mongo(paper_list)
        mongo_integration.bulk_save(documents)

        actions = elastic_parser.generate_actions(chunk=paper_list)
        elastic_integration.save_data(actions)

        chunk_end = time.time()
        print(f"Chunk time: {chunk_end - chunk_start:.2f}s")

    total_end = time.time()
    print(f"Total ingestion time: {total_end - total_start:.2f}s")

main()