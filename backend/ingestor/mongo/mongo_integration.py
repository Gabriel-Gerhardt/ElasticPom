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
        valid_filters = [f for f in filters if f.get("filtername")]
        ops = [
            UpdateOne(
                {"_id": f["filtername"]},
                {"$set": {**f, "_id": f["filtername"]}},
                upsert=True
            )
            for f in valid_filters
        ]
        if not ops:
            return None
        try:
            result = self.db["filters"].bulk_write(ops, ordered=False)
        except BulkWriteError as e:
            print(f"Bulk write parcial: {e.details}")
            return e.details

        # Drop stale filter docs left over from previous runs (e.g. old
        # "contributors.keyword"/"subjects.keyword" entries) that are no
        # longer part of the current FILTERS list.
        current_ids = [f["filtername"] for f in valid_filters]
        self.db["filters"].delete_many({"_id": {"$nin": current_ids}})

        return result

    def close(self):
        self.client.close()