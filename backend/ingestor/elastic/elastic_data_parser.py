class ElasticDataParser:
    def __init__(self, index_name):
        self.index_name = index_name

    def parse_row(self, row):
        return row.dropna().to_dict()

    def generate_actions(self, chunk):
        for _, row in chunk.iterrows():
            yield {
                "_id": row.get("id"),
                "_index": self.index_name,
                "_source": self.parse_row(row)
            }