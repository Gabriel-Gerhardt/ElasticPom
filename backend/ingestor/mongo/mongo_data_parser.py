from utils.paper_parser import PaperParser
class MongoDataParser:
    def __init__(self, paper_parser: PaperParser):
        self.paper_parser = paper_parser
    def generate_mongo(self, chunk):
        for _, row in chunk.iterrows():
            document = self.parse_document(row)
            yield document

    def parse_document(self, row):
        doc = {
            "paper_id": self.paper_parser.normalize_paper_id(str(row["id"])),
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
            parts_by_point = category.strip().split(".")
            parts_by_trace = category.split("-")
            if len(parts_by_point) == 2:
                result.append({
                    "main_topic": parts_by_point[0],
                    "secondary_topic": parts_by_point[1]
                })
            if len(parts_by_trace) == 2:
                result.append({
                    "main_topic": parts_by_trace[0],
                    "secondary_topic": parts_by_trace[1]
                })

            else:
                result.append({
                    "main_topic": category,
                    "secondary_topic": None
                })
        return result

    def build_versions(self, versions):
        if not versions:
            return []

        return [
            {
                "version": v["version"],
                "created_at": v["created"]
            }
            for v in versions
        ]