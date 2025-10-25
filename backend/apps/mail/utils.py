import html
import re


def html_to_text(html_str: str) -> str:
    s = re.sub(r"(?i)<\s*br\s*/?>", "\n", html_str)
    s = re.sub(r"(?i)</\s*p\s*>", "\n\n", s)
    s = re.sub(r"(?s)<[^>]+>", "", s)
    return html.unescape(s).strip()


def text_to_html(text_str: str) -> str:
    esc = html.escape(text_str)
    return esc.replace("\n", "<br>")
