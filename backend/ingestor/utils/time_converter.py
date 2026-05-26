from datetime import datetime, timezone

class TimeConverter:
    def rfc1123_to_iso(self, date_str: str) -> str | None:
        if not date_str:
            return None

        dt = datetime.strptime(date_str.strip(), "%a, %d %b %Y %H:%M:%S %Z")
        return dt.replace(tzinfo=timezone.utc).isoformat()

    def to_iso(self, date_str: str) -> str | None:
        if not date_str:
            return None

        s = date_str.strip().replace("Z", "+00:00")
        for parser in (datetime.fromisoformat,
                       lambda x: datetime.strptime(x, "%Y-%m-%d")):
            try:
                dt = parser(s)

                if dt.tzinfo is None:
                    dt = dt.replace(tzinfo=timezone.utc)
                return dt.isoformat()
            except ValueError:
                continue

        return None