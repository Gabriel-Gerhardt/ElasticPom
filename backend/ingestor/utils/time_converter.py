from datetime import datetime, timezone

class TimeConverter:
    def rfc1123_to_iso(self, date_str: str) -> str | None:
        if not date_str:
            return None

        dt = datetime.strptime(date_str.strip(), "%a, %d %b %Y %H:%M:%S %Z")
        return dt.replace(tzinfo=timezone.utc).isoformat()
