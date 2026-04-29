import requests
from elasticsearch import Elasticsearch, helpers

class ElasticIntegration:
    elasticsearch_host = "http://localhost:9200"
    client = None
    def __init__(self, elasticsearch_host):
        self.elasticsearch_host = elasticsearch_host
        self.client = Elasticsearch(self.elasticsearch_host)

    def save_data(self, actions):
        helpers.bulk(self.client, actions, chunk_size=1000)

    def put_mapping(self, index, mapping):
        r = requests.head(self.elasticsearch_host + "/" + index)
        if r.status_code != 200:
            self.client.indices.create(index=index, body=mapping)
            return True
        return True