from elastic.elastic_data_parser import ElasticDataParser
from elastic.elastic_integration import ElasticIntegration
import time
from ingestor import Ingestor
from mongo.mongo_data_parser import MongoDataParser
from mongo.mongo_integration import MongoIntegration
from utils.paper_parser import PaperParser
from utils.time_converter import TimeConverter


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
            }
        }
    }
    index = "paper"
    dataset_path = "datasets/arxiv-dataset.json"

    paper_parser = PaperParser()
    time_converter = TimeConverter()

    mongo_parser = MongoDataParser(paper_parser)
    mongo_integration = MongoIntegration(
        uri="mongodb://admin:password@localhost:27017",
        database="elasticpom",
        collection="Paper"
    )
    elastic_parser = ElasticDataParser(index, time_converter, paper_parser)
    elastic_integration = ElasticIntegration(elasticsearch_host="http://localhost:9200")
    elastic_integration.put_mapping(index, mapping)

    ingestor = Ingestor()
    total_start = time.time()

    for chunk in ingestor.run(dataset_path):
        chunk_start = time.time()

        documents = mongo_parser.generate_mongo(chunk)
        mongo_integration.bulk_save(documents)

        actions = elastic_parser.generate_actions(chunk=chunk)
        elastic_integration.save_data(actions)


        chunk_end = time.time()
        print(f"Chunk time: {chunk_end - chunk_start:.2f}s")

    total_end = time.time()
    print(f"Total ingestion time: {total_end - total_start:.2f}s")

main()