import html
import re
from datetime import datetime


def html_to_text(html_str: str) -> str:
    s = re.sub(r"(?i)<\s*br\s*/?>", "\n", html_str)
    s = re.sub(r"(?i)</\s*p\s*>", "\n\n", s)
    s = re.sub(r"(?s)<[^>]+>", "", s)
    return html.unescape(s).strip()


def text_to_html(text_str: str) -> str:
    esc = html.escape(text_str)
    return esc.replace("\n", "<br>")


def compare_iso_datetimes(s1, s2) -> int:
    """
    Compare two ISO8601 datetime strings.

    Args:
        s1, s2: datetime.datetime or str
            - e.g. "2025-10-31T20:24:02+09:00"
            - or datetime(2025, 10, 31, 20, 24, 2, tzinfo=timezone(timedelta(hours=9)))

    Returns:
        int:
            -1 if s1 < s2
             0 if s1 == s2
             1 if s1 > s2
    """

    def to_datetime(val):
        if isinstance(val, datetime):
            return val
        elif isinstance(val, str):
            return datetime.fromisoformat(val.replace(" ", "T"))
        else:
            raise TypeError("Unsupported type")

    d1 = to_datetime(s1)
    d2 = to_datetime(s2)

    if d1.tzinfo is None or d2.tzinfo is None:
        raise ValueError("Both datetimes must include timezone information")

    if d1 > d2:
        return 1
    elif d1 < d2:
        return -1
    return 0
