from elastic.elastic_data_parser import ElasticDataParser
from elastic.elastic_integration import ElasticIntegration
import time
from ingestor import Ingestor

def main():
    mapping = {
        "mappings": {
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
    parser = ElasticDataParser(index)
    elastic_integration = ElasticIntegration(elasticsearch_host="http://localhost:9200")
    elastic_integration.put_mapping(index, mapping)
    ingestor = Ingestor()

    for chunk in ingestor.run(dataset_path):
        start = time.time()
        actions = parser.generate_actions(chunk = chunk)
        elastic_integration.save_data(actions)
        end = time.time()
        print(f"Total chunk time: {end - start:.2f}s")
    end = time.time()
    print(f"Total ingestion time: {end - start:.2f}s")

main()