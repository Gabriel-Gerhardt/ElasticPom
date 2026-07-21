from embedding.embedding_service import EmbeddingService
import time
from ingestor import Ingestor
from serializer.JsonKafkaSerializer import JsonKafkaSerializer
from serializer.StringKafkaSerializer import StringKafkaSerializer
from utils.paper_parser import PaperParser
from oaipmh_scythe import Scythe
from kafka import KafkaProducer


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
                    "fields": {
                        "keyword": { "type": "keyword" }
                    }
                },
                "subjects": {
                    "type": "text",
                    "fields": {
                        "keyword": { "type": "keyword" }
                    }
                },
                "description": {
                    "type": "text",
                },
                "publisher": {
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
    topic = "paper"
    paper_parser = PaperParser()
    embedding_service = EmbeddingService()
    producer = KafkaProducer(
        bootstrap_servers="localhost:9094",
        value_serializer = JsonKafkaSerializer(),
        key_serializer=StringKafkaSerializer(),
    )

    # mongo_parser = MongoDataParser(paper_parser)
    # mongo_integration = MongoIntegration(
    #     uri="mongodb://admin:password@localhost:27017",
    #     database="elasticpom",
    #     collection="Paper"
    # # )
    # elastic_parser = ElasticDataParser(index, time_converter, paper_parser, embedding_service)
    # elastic_integration = ElasticIntegration(elasticsearch_host="http://localhost:9200")
    # elastic_integration.put_mapping(index, mapping)
    #
    # mongo_integration.save_filters(FILTERS)

    ingestor = Ingestor("https://oaipmh.arxiv.org/oai","oai_dc","cs:cs:IR")
    total_start = time.time()

    for chunk in ingestor.run():
        chunk_start = time.time()
        paper_list = paper_parser.parse(chunk)
        for paper in paper_list:
            producer.send(topic=topic, key=paper["paper_id"], value = paper)

        producer.flush()
        chunk_end = time.time()
        print(f"Chunk time: {chunk_end - chunk_start:.2f}s")

    total_end = time.time()
    print(f"Total ingestion time: {total_end - total_start:.2f}s")

main()