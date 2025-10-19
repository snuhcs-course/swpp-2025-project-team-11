import base64
import hashlib
import hmac
import re
import uuid
from collections.abc import Generator, Iterable
from dataclasses import dataclass

from django.conf import settings

SERVER_SECRET = settings.PII_MASKING_SECRET

OPEN_TOKENS = ("{{PII:", "{PII:")
CLOSE_FOR = {"{{PII:": "}}", "{PII:": "}"}
LONGEST_OPEN = max(len(t) for t in OPEN_TOKENS)

PLACEHOLDER_RX = re.compile(
    r"(?P<open>\{\{?)"
    r"PII:"
    r"(?P<rid>[a-z0-9]{12}):"
    r"(?P<n>\d+):"
    r"(?P<tag>[A-Za-z0-9_-]{4,16})"
    r"(?P<close>\}\}?)"
)

MAX_PLACEHOLDER_LEN = 96


def make_req_id() -> str:
    return uuid.uuid4().hex[:12]


def _make_tag(req_id: str, n: int, original: str, size: int = 8) -> str:
    sig = hmac.new(SERVER_SECRET, f"{req_id}:{n}:{original}".encode(), hashlib.sha256).digest()
    return base64.urlsafe_b64encode(sig)[:size].decode()


@dataclass
class PatternSpec:
    name: str
    regex: re.Pattern
    group: int


PATTERNS: list[PatternSpec] = [
    PatternSpec(
        "EMAIL",
        re.compile(
            r"(?P<email>[A-Za-z0-9._%+\-]+@(?:[A-Za-z0-9\-]+\.)+[A-Za-z]{2,24})(?![A-Za-z0-9._%+\-@])"
        ),
        0,
    ),
    PatternSpec(
        "PHONE",
        re.compile(r"(?P<phone>(?:\+?\d{1,3}[-.\s]?)?(?:\d{2,4}[-.\s]?){2,4}\d)(?!\d)"),
        0,
    ),
    PatternSpec("CARD", re.compile(r"(?P<card>(?:\d[ -]*?){13,19})(?![0-9\-])"), 0),
    PatternSpec("CVV_CVC", re.compile(r"\b(?:CVV|CVC)[:\s]*\d{3,4}\b", re.I), 0),
    PatternSpec("CARD_EXP", re.compile(r"\b(0[1-9]|1[0-2])/(?:\d{2}|\d{4})\b"), 0),
    PatternSpec("RRN", re.compile(r"(?P<rrn>\d{6}[-\s]?\d{7})(?![0-9])"), 0),
    PatternSpec("ACCOUNT_SIMPLE", re.compile(r"\b(?:\d{2,6}[-\s]?\d{2,6}[-\s]?\d{2,8})\b"), 0),
    PatternSpec("IBAN", re.compile(r"\b[A-Z]{2}\d{2}[A-Z0-9]{10,30}\b"), 0),
    PatternSpec("SWIFT_BIC", re.compile(r"\b[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?\b"), 0),
    PatternSpec("SSN_US", re.compile(r"\b\d{3}-\d{2}-\d{4}\b"), 0),
    PatternSpec(
        "JWT", re.compile(r"\beyJ[A-Za-z0-9_\-]+?\.[A-Za-z0-9_\-]+?\.[A-Za-z0-9_\-]*\b"), 0
    ),
    PatternSpec("BEARER", re.compile(r"\bBearer\s+[A-Za-z0-9\-_.=]{20,}\b", re.I), 0),
    PatternSpec("AWS", re.compile(r"\bAKIA[0-9A-Z]{16}\b"), 0),
    PatternSpec("GITHUB_PAT", re.compile(r"\bgh[pousr]_[A-Za-z0-9]{36,}\b"), 0),
    PatternSpec(
        "UUID",
        re.compile(
            r"\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}\b"
        ),
        0,
    ),
    PatternSpec("IPV4", re.compile(r"\b(?:(?:25[0-5]|2[0-4]\d|[01]?\d\d?)(?:\.(?!$)|$)){4}\b"), 0),
    PatternSpec("MAC", re.compile(r"\b(?:[0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}\b"), 0),
    PatternSpec("VIN", re.compile(r"\b[A-HJ-NPR-Za-hj-npr-z0-9]{17}\b"), 0),
    PatternSpec(
        "PASS", re.compile(r"(?i)(?:password|pwd|pass|비밀번호)\s*[:=]\s*([^\s,;]{4,64})"), 1
    ),
    PatternSpec("LONG_DIGITS", re.compile(r"\b\d{6,20}\b"), 0),
    PatternSpec("URL", re.compile(r"https?://[^\s]+"), 0),
]


class PiiMasker:
    def __init__(self, req_id: str):
        self.req_id = req_id
        self._n = 0
        self._map: dict[int, str] = {}

    def _next_ph(self, original: str) -> str:
        self._n += 1
        tag = _make_tag(self.req_id, self._n, original)
        self._map[self._n] = original
        return f"{{{{PII:{self.req_id}:{self._n}:{tag}}}}}"

    def _placeholder_spans(self, s: str) -> list[tuple[int, int]]:
        return [m.span() for m in PLACEHOLDER_RX.finditer(s)]

    def _is_inside(self, idx: int, spans: list[tuple[int, int]]) -> bool:
        return any(a <= idx < b for a, b in spans)

    def mask_text(self, text: str) -> tuple[str, dict[int, str]]:
        if not text:
            return text, {}

        ph_spans = self._placeholder_spans(text)
        candidates: list[tuple[int, int, str]] = []

        # 모든 패턴에 대해 탐색
        for spec in PATTERNS:
            for m in spec.regex.finditer(text):
                if spec.group == 0:
                    start, end = m.span()
                    original = text[start:end]
                else:
                    start, end = m.span(spec.group)
                    original = m.group(spec.group)

                if self._is_inside(start, ph_spans):
                    continue

                candidates.append((start, end, original))

        if not candidates:
            return text, {}

        candidates.sort(key=lambda x: (x[0], -(x[1] - x[0])))
        non_overlap = []
        cur_end = -1
        for s, e, val in candidates:
            if s >= cur_end:
                non_overlap.append((s, e, val))
                cur_end = e

        out = text
        for s, e, val in sorted(non_overlap, key=lambda x: x[0], reverse=True):
            ph = self._next_ph(val)
            out = out[:s] + ph + out[e:]

        return out, dict(self._map)

    def mask_inputs(
        self,
        inputs: dict[str, str],
        keys=(
            "subject",
            "body",
        ),
    ):
        out = dict(inputs)
        for k in keys:
            v = out.get(k)
            if isinstance(v, str) and v:
                masked, _ = self.mask_text(v)
                out[k] = masked
        return out, dict(self._map)


def unmask_once(text: str, req_id: str, mapping: dict[int, str]) -> str:
    """
    - {{PII:...}} 또는 {PII:...} 둘 다 매칭 시도
    - 단, 여는/닫는 중괄호 수가 같아야 복원 허용 (예: '{{...}}' 또는 '{...}')
    - req_id 일치 + tag(HMAC) 검증 통과 시에만 원문으로 교체
    """

    def _repl(m: re.Match) -> str:
        open_seq = m.group("open")
        close_seq = m.group("close")
        if len(open_seq) != len(close_seq):
            return m.group(0)

        rid = m.group("rid")
        if rid != req_id:
            return m.group(0)

        try:
            n = int(m.group("n"))
        except ValueError:
            return m.group(0)

        original = mapping.get(n)
        if not original:
            return m.group(0)

        # 태그 검증
        if _make_tag(req_id, n, original) != m.group("tag"):
            return m.group(0)

        return original

    return PLACEHOLDER_RX.sub(_repl, text)


def _verify_and_unmask_token(token: str, req_id: str, mapping: dict[int, str]) -> str | None:
    m = PLACEHOLDER_RX.fullmatch(token)
    if not m:
        return None
    # 여닫는 중괄호 수 일치 필수
    if len(m.group("open")) != len(m.group("close")):
        return None
    if m.group("rid") != req_id:
        return None
    try:
        n = int(m.group("n"))
    except ValueError:
        return None
    original = mapping.get(n)
    if not original:
        return None
    if _make_tag(req_id, n, original) != m.group("tag"):
        return None
    return original


def unmask_stream(
    chunks: Iterable[str],
    req_id: str,
    mapping: dict[int, str],
) -> Generator[str, None, None]:
    """
    최소 버퍼 DFA:
    - PLAIN 모드: 즉시 방출
    - OPEN 모드: '{'부터 OPEN_TOKENS prefix를 한 글자씩 검사
    - TOKEN 모드: 닫힘 토큰까지 수집 → 검증/복원 → 방출
    PPI 마스킹된 부분은 버퍼로 모아서 마스킹 해제, 그렇지 않은 평문은 즉시 flush함.
    """

    mode = "PLAIN"  # PLAIN | OPEN | TOKEN
    open_buf = ""  # OPEN 모드에서 오픈 토큰 부분일치 버퍼
    token_buf = ""  # TOKEN 모드에서 전체 토큰 버퍼
    open_tok = ""  # 오픈 토큰
    close_tok = ""  # 닫힘 토큰
    out_buf: list[str] = []

    def flush():
        if out_buf:
            yield "".join(out_buf)
            out_buf.clear()

    def is_prefix_of_open(s: str) -> bool:
        return any(t.startswith(s) for t in OPEN_TOKENS)

    for chunk in chunks:
        for ch in chunk:
            if mode == "PLAIN":
                if ch == "{":
                    mode = "OPEN"
                    open_buf = "{"
                else:
                    out_buf.append(ch)

            elif mode == "OPEN":
                open_buf += ch
                if any(open_buf == t for t in OPEN_TOKENS):
                    mode = "TOKEN"
                    open_tok = open_buf
                    close_tok = CLOSE_FOR[open_tok]
                    token_buf = open_buf
                    open_buf = ""
                    continue
                if is_prefix_of_open(open_buf):
                    continue
                while open_buf and not is_prefix_of_open(open_buf):
                    out_buf.append(open_buf[0])
                    open_buf = open_buf[1:]
                if not open_buf:
                    mode = "PLAIN"

            else:  # mode == "TOKEN"
                token_buf += ch
                if len(token_buf) > MAX_PLACEHOLDER_LEN:
                    out_buf.append(token_buf)
                    token_buf = ""
                    open_tok = close_tok = ""
                    mode = "PLAIN"
                    continue
                if token_buf.endswith(close_tok):
                    replaced = _verify_and_unmask_token(token_buf, req_id, mapping)
                    out_buf.append(replaced if replaced is not None else token_buf)
                    token_buf = ""
                    open_tok = close_tok = ""
                    mode = "PLAIN"

        yield from flush()

    if mode == "OPEN" and open_buf:
        out_buf.append(open_buf)
    elif mode == "TOKEN" and token_buf:
        out_buf.append(token_buf)
    yield from flush()
