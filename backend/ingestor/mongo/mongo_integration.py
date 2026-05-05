from pymongo import MongoClient

class MongoIntegration:
    def __init__(self, uri, database, collection):
        self.client = MongoClient(uri)
        self.db = self.client[database]
        self.collection = self.db[collection]

    def bulk_save(self, documents):
        docs = list(documents)
        if docs:
            self.collection.insert_many(docs)