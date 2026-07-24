from datetime import datetime


DC_KNOWN_FIELDS = {
    "title", "creator", "subject", "description", "publisher",
    "contributor", "date", "type", "format", "identifier",
    "source", "language", "relation", "coverage", "rights",
}


class PaperParser:
    def __init__(self):
        pass

    def parse(self, chunk):
        return [self._record_to_dict(r) for r in chunk if not r.deleted]

    def _record_to_dict(self, record) -> dict:
        meta = record.metadata or {}

        def first(key):
            vals = meta.get(key) or []
            return vals[0] if vals else None

        def all_of(key):
            return meta.get(key) or []

        unique_fields = [
            {"name": k, "data": v}
            for k, vals in meta.items()
            if k not in DC_KNOWN_FIELDS
            for v in vals
        ]

        return {
            "paper_id": record.header.identifier,
            "datestamp": self._parse_date(record.header.datestamp),
            "title": first("title"),
            "creators": all_of("creator"),
            "subjects": all_of("subject"),
            "description": first("description"),
            "publisher": first("publisher"),
            "contributors": all_of("contributor"),
            "date": self._parse_date(first("date")),
            "type": first("type"),
            "identifier": first("identifier"),
            "source": first("source"),
            "language": first("language"),
            "relations": all_of("relation"),
            "coverage": first("coverage"),
            "rights": first("rights"),
            "unique_fields": unique_fields,
        }

    @staticmethod
    def _parse_date(value): ##This make sure it does not break in java if it comes with hours and seconds
        if not value:
            return None
        try:
            return datetime.fromisoformat(value.replace("Z", "+00:00")).date().isoformat()
        except (ValueError, AttributeError):
            return value