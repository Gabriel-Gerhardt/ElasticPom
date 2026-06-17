from pymongo import MongoClient, UpdateOne
from pymongo.errors import BulkWriteError

class MongoIntegration:
    def __init__(self, uri, database, collection, unique_key="paper_id"):
        self.client = MongoClient(uri)
        self.db = self.client[database]
        self.collection = self.db[collection]
        self.unique_key = unique_key
        self.ensure_indexes()

    def bulk_save(self, documents):
        ops = [
            UpdateOne(
                {self.unique_key: doc[self.unique_key]},
                {"$set": doc},
                upsert=True
            )
            for doc in documents
            if doc.get(self.unique_key) is not None
        ]
        if not ops:
            return None
        try:
            return self.collection.bulk_write(ops, ordered=False)
        except BulkWriteError as e:
            print(f"Bulk write parcial: {e.details}")
            return e.details

    def ensure_indexes(self):
        self.collection.create_index(self.unique_key, unique=True)

    def save_filters(self, filters: list[dict]):
        ops = [
            UpdateOne(
                {"_id": f["filtername"]},
                {"$set": {**f, "_id": f["filtername"]}},
                upsert=True
            )
            for f in filters
            if f.get("filtername") is not None
        ]
        if not ops:
            return None
        try:
            return self.db["filters"].bulk_write(ops, ordered=False)
        except BulkWriteError as e:
            print(f"Bulk write parcial: {e.details}")
            return e.details

    def close(self):
        self.client.close()