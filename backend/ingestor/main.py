from elastic.elastic_data_parser import ElasticDataParser
from elastic.elastic_integration import ElasticIntegration
import time
from ingestor import Ingestor
from mongo.mongo_data_parser import MongoDataParser
from mongo.mongo_integration import MongoIntegration


def main():
    mapping = {
        "settings": {
            "index.mapping.coerce": False
        },
        "mappings": {
            "dynamic": "strict",
            "properties": {
                "id": {
                    "type": "keyword"
                },
                "submitter": {
                    "type": "text",
                },
                "authors": {
                    "type": "text",
                },
                "comments": {
                    "type": "text",
                },
                "journal_ref": {
                    "type": "text",
                },
                "doi": {
                    "type": "text"
                },
                "categories": {
                    "type": "text",
                },
                "license": {
                    "type": "text",
                },
                "abstract": {
                    "type": "text",
                },
                "version": {
                    "type": "nested",
                    "properties": {
                        "version": {
                            "type": "text",
                        },
                        "created":{
                            "type": "text",
                        }
                    }
                },
                "update_date": {
                    "type": "text",
                },
                "authors_parsed": {
                    "type": "keyword",
                        
                },
                "categories_parsed": {
                    "type": "keyword",
                }
            }
        }
    }
    index = "arxiv"
    start = time.time()
    dataset_path = "datasets/arxiv-dataset.json"

    mongo_parser = MongoDataParser()
    mongo_integration = MongoIntegration(
        uri="mongodb://admin:password@localhost:27017",
        database="elasticpom",
        collection="Paper"
    )

    elastic_parser = ElasticDataParser(index)
    elastic_integration = ElasticIntegration(elasticsearch_host="http://localhost:9200")
    elastic_integration.put_mapping(index, mapping)

    ingestor = Ingestor()

    total_start = time.time()

    for chunk in ingestor.run(dataset_path):
        chunk_start = time.time()

        actions = elastic_parser.generate_actions(chunk=chunk)
        elastic_integration.save_data(actions)

        documents = mongo_parser.generate_mongo(chunk)
        mongo_integration.bulk_save(documents)

        chunk_end = time.time()
        print(f"Chunk time: {chunk_end - chunk_start:.2f}s")

    total_end = time.time()
    print(f"Total ingestion time: {total_end - total_start:.2f}s")

main()