from elasticsearch import Elasticsearch, helpers

class ElasticIntegration:
    elasticsearch_host = "http://localhost:9200"
    client = None
    def __init__(self, elasticsearch_host):
        self.elasticsearch_host = elasticsearch_host
        self.client = Elasticsearch(self.elasticsearch_host)

    def save_data(self, actions):
        helpers.bulk(self.client, actions, chunk_size=1000)