class MongoDataParser:
    def generate_mongo(self, chunk):
        for _, row in chunk.iterrows():
            document = self.parse_document(row)
            yield document

    def parse_document(self, row):
        doc = {
            "arxiv_id": str(row["id"]),
            "authors": row["authors"],
            "title": row["title"],
            "comments": row["comments"],
            "journal_ref": row["journal-ref"],
            "doi": row["doi"],
            "report_no": row["report-no"],
            "categories": self.build_categories(row["categories"]),
            "license": row["license"],
            "description": row["abstract"],
            "versions": self.build_versions(row["versions"]),
            "update_date": row["update_date"]
        }
        return doc

    def build_categories(self, categories_str):
        result = []
        for category in categories_str.split(" "):
            parts = category.strip().split(".")
            if len(parts) == 2:
                result.append({
                    "mainTopic": parts[0],
                    "secondaryTopic": parts[1]
                })
            else:
                result.append({
                    "mainTopic": parts[0],
                    "secondaryTopic": None
                })
        return result

    def build_versions(self, versions):
        if not versions:
            return []

        return [
            {
                "version": v["version"],
                "createdAt": v["created"]
            }
            for v in versions
        ]