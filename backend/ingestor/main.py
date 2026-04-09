from elastic.elastic_data_parser import ElasticDataParser
from elastic.elastic_integration import ElasticIntegration
import time
from ingestor import Ingestor

def main():
    start = time.time()
    dataset_path = "datasets/arxiv-dataset.json"
    print("Loading dataset...")
    parser = ElasticDataParser("arxiv")
    elastic_integration = ElasticIntegration(elasticsearch_host="http://localhost:9200")
    ingestor = Ingestor()

    for chunk in ingestor.run(dataset_path):
        start = time.time()
        actions = parser.generate_actions(chunk = chunk)
        elastic_integration.save_data(actions)
        end = time.time()
        print(f"Tempo total de chunk: {end - start:.2f}s")
    end = time.time()
    print(f"Tempo total: {end - start:.2f}s")

main()