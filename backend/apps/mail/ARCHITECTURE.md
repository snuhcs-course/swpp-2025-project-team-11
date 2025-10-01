Mail 앱 구조 설명
1. models.py - 데이터베이스 모델
Read models.py
models.py 설명:
class Email(models.Model):
    # 이 모델은 선택사항입니다 - Gmail에서 가져온 메일을 DB에 캐싱할 때 사용
    
    user = models.ForeignKey("my_auth.Users", ...)
    # ↑ 어떤 사용자의 메일인지 (Users 모델과 연결)
    
    gmail_id = models.CharField(max_length=255, unique=True)
    # ↑ Gmail API에서 받은 고유 메시지 ID (예: "18f2a3b4c5d6e7f8")
    
    thread_id = models.CharField(max_length=255)
    # ↑ Gmail 스레드 ID (답장들이 같은 thread_id를 가짐)
    
    subject = models.TextField(blank=True)  # 제목
    sender = models.CharField(max_length=255)  # 보낸 사람
    recipient = models.CharField(max_length=255)  # 받는 사람
    snippet = models.TextField(blank=True)  # 미리보기 (처음 몇 글자)
    body = models.TextField(blank=True)  # 본문 전체
    
    received_at = models.DateTimeField()  # 수신 시간
    is_read = models.BooleanField(default=False)  # 읽음 여부
    labels = models.JSONField(default=list)  # ['INBOX', 'UNREAD', 'IMPORTANT' 등]
    
    # Meta 클래스
    ordering = ["-received_at"]  # 최신 메일이 먼저 오도록
    indexes = [...]  # 검색 속도 향상을 위한 인덱스
2. services.py - Gmail API 통신
Read services.py
services.py 설명:
class GmailService:
    # Gmail API와 통신하는 핵심 클래스
    
    def __init__(self, access_token: str):
        # Google access token으로 Gmail API 클라이언트 생성
        credentials = Credentials(token=access_token)
        self.service = build("gmail", "v1", credentials=credentials)
        # ↑ 이게 실제 Gmail API를 호출하는 객체
    
    def list_messages(self, max_results=20, page_token=None, label_ids=None):
        # 메일 목록 가져오기
        # Gmail API: users.messages.list 호출
        # 반환: {'messages': [{'id': '...', 'threadId': '...'}], 'nextPageToken': '...'}
        
    def get_message(self, message_id: str):
        # 특정 메일의 상세 정보 가져오기
        # Gmail API: users.messages.get 호출
        # format="full" → 헤더, 본문 전부 가져옴
        
    def _parse_message(self, message: dict):
        # Gmail API 응답을 우리가 쓰기 좋은 형태로 변환
        # 예: headers 배열 → 딕셔너리로 변환
        #     base64 인코딩된 본문 → 일반 텍스트로 디코딩
        
    def _get_body(self, payload: dict):
        # 메일 본문 추출 (재귀적으로)
        # Gmail은 본문을 parts로 나눠서 보냄 (HTML, plain text 등)
        # 이걸 탐색해서 text/plain 찾아서 반환
        
    def _decode_body(self, data: str):
        # base64 URL-safe 디코딩
        # Gmail API는 본문을 base64로 인코딩해서 보냄
        
핵심 포인트:
Gmail API는 복잡한 JSON 구조로 데이터를 줌
이 클래스가 그걸 단순화해서 사용하기 쉽게 만듦

3. serializers.py - 데이터 직렬화
Read serializers.py
serializers.py 설명:
# Serializer = Python 객체 ↔ JSON 변환기

class EmailListSerializer(serializers.Serializer):
    # 메일 목록용 (가벼운 정보만)
    id = serializers.CharField()  # Gmail message ID
    subject = serializers.CharField()  # 제목
    from_email = serializers.CharField(source="from")  
    # ↑ "from"은 Python 예약어라서 "from_email"로 바꿈
    snippet = serializers.CharField()  # 미리보기
    is_unread = serializers.BooleanField()  # 안읽음 여부
    # body가 없음 → 목록에선 본문 안 보냄 (가볍게)

class EmailDetailSerializer(serializers.Serializer):
    # 메일 상세용 (모든 정보)
    body = serializers.CharField()  # ← 여기선 본문도 포함!
    # 나머지는 EmailListSerializer와 비슷

class EmailModelSerializer(serializers.ModelSerializer):
    # DB에 저장할 때 사용 (선택사항)
    # ModelSerializer = 자동으로 Email 모델의 필드를 직렬화
왜 3개나 있나?
EmailListSerializer: API 응답용 (Gmail API → JSON)
EmailDetailSerializer: API 응답용 (상세)
EmailModelSerializer: DB 저장용 (나중에 캐싱할 때)
4. views.py - API 엔드포인트
Read views.py
views.py 설명:
class EmailListView(APIView):
    # GET /api/mail/emails/ 엔드포인트
    
    authentication_classes = [JWTAuthentication]
    # ↑ JWT 토큰으로 인증 (Authorization: Bearer <token>)
    
    permission_classes = [IsAuthenticated]
    # ↑ 로그인한 사용자만 접근 가능
    
    def get(self, request):
        # 1단계: 사용자 확인
        user = request.user
        
        # 2단계: Google access token 갱신
        access_token = google_refresh(user)
        # ↑ DB에 저장된 refresh_token으로 새 access_token 받아옴
        
        # 3단계: 파라미터 파싱
        max_results = ...  # ?max_results=10
        page_token = ...   # ?page_token=abc123 (다음 페이지)
        labels = ...       # ?labels=INBOX,UNREAD
        
        # 4단계: Gmail API 호출
        gmail_service = GmailService(access_token)
        result = gmail_service.list_messages(...)
        
        # 5단계: 각 메시지의 상세 정보 가져오기
        for msg in result['messages']:
            message_detail = gmail_service.get_message(msg['id'])
            messages.append(message_detail)
        
        # 6단계: JSON으로 변환해서 응답
        serializer = EmailListSerializer(messages, many=True)
        return Response({
            "messages": serializer.data,
            "next_page_token": ...,
            "result_size_estimate": ...
        })
흐름:
클라이언트
  ↓ (JWT 토큰 + 요청)
EmailListView
  ↓ (refresh_token)
google_refresh() → Google OAuth2 서버
  ↓ (새 access_token)
GmailService
  ↓ (access_token)
Gmail API
  ↓ (메일 데이터)
EmailListSerializer
  ↓ (JSON)
클라이언트

5. urls.py - URL 라우팅
Read urls.py
urls.py 설명:
urlpatterns = [
    # /api/mail/emails/
    path("emails/", EmailListView.as_view(), name="email_list"),
    # → GET: 메일 목록
    
    # /api/mail/emails/18f2a3b4c5d6e7f8/
    path("emails/<str:message_id>/", EmailDetailView.as_view(), name="email_detail"),
    # → GET: 특정 메일 상세
    
    # /api/mail/emails/18f2a3b4c5d6e7f8/read/
    path("emails/<str:message_id>/read/", EmailMarkReadView.as_view(), name="email_mark_read"),
    # → PATCH: 읽음/안읽음 표시
]
URL 매핑:
/api/mail/ (config/urls.py에서 설정)
+ emails/ → /api/mail/emails/
+ <message_id>/ → /api/mail/emails/abc123/

전체 흐름 정리
1. 사용자가 로그인 (my_auth 앱)
   → JWT 토큰 + Google refresh_token 받음

2. 클라이언트가 메일 목록 요청
   GET /api/mail/emails/
   Authorization: Bearer <JWT>
   
3. EmailListView
   - JWT 검증 ✓
   - refresh_token으로 access_token 갱신
   - Gmail API 호출
   - 메일 데이터 파싱
   - JSON 응답

4. 클라이언트가 특정 메일 조회
   GET /api/mail/emails/abc123/
   
5. EmailDetailView
   - 같은 과정
   - 본문 포함해서 응답


Gmail API가 왜 이렇게 복잡하게 데이터를 보내는지 예시로 보여드릴게요.
Gmail API의 실제 응답 예시
1. _parse_message - 헤더 배열을 딕셔너리로 변환
Gmail API가 보내는 원본:
{
  "id": "18f2a3b4c5d6e7f8",
  "payload": {
    "headers": [
      {"name": "From", "value": "sender@gmail.com"},
      {"name": "To", "value": "you@gmail.com"},
      {"name": "Subject", "value": "Meeting Tomorrow"},
      {"name": "Date", "value": "Mon, 1 Oct 2025 09:30:00 +0900"}
    ]
  }
}
문제:
헤더가 배열로 와서 "Subject" 찾으려면 반복문 돌려야 함
매번 for h in headers if h['name'] == 'Subject' 해야 함
우리 코드:
headers = message["payload"]["headers"]
headers_dict = {h["name"].lower(): h["value"] for h in headers}
# 결과: {'from': 'sender@gmail.com', 'subject': 'Meeting Tomorrow', ...}

# 이제 쉽게 접근 가능
subject = headers_dict.get("subject")  # "Meeting Tomorrow"
2. _get_body - 복잡한 parts 구조 탐색
Gmail API가 보내는 원본:
{
  "payload": {
    "mimeType": "multipart/alternative",
    "parts": [
      {
        "mimeType": "text/plain",
        "body": {
          "data": "SGVsbG8sIGxldCdzIG1lZXQgdG9tb3Jyb3cgYXQgMTBhbQ=="
        }
      },
      {
        "mimeType": "text/html",
        "body": {
          "data": "PGh0bWw+PGJvZHk+SGVsbG8sIGxldCdzIG1lZXQ8L2JvZHk+PC9odG1sPg=="
        }
      }
    ]
  }
}
왜 이렇게 복잡한가?
이메일은 보통 plain text와 HTML 두 버전을 동시에 보냄
첨부파일이 있으면 더 복잡해짐
우리 코드:
def _get_body(self, payload: dict):
    # 1. parts가 있으면 text/plain 찾기
    if "parts" in payload:
        for part in payload["parts"]:
            if part["mimeType"] == "text/plain":
                return self._decode_body(part["body"]["data"])
    
    # 2. parts가 없으면 body에서 직접 가져오기
    if "body" in payload and "data" in payload["body"]:
        return self._decode_body(payload["body"]["data"])
더 복잡한 실제 예시 (중첩된 parts):
{
  "payload": {
    "parts": [
      {
        "mimeType": "multipart/alternative",
        "parts": [
          {"mimeType": "text/plain", "body": {"data": "..."}},
          {"mimeType": "text/html", "body": {"data": "..."}}
        ]
      },
      {
        "mimeType": "image/png",
        "filename": "logo.png",
        "body": {"attachmentId": "..."}
      }
    ]
  }
}
재귀 호출이 필요한 이유:
elif part["mimeType"] == "multipart/alternative":
    body = self._get_body(part)  # 재귀!
    if body:
        return body
3. _decode_body - Base64 디코딩
Gmail API가 보내는 데이터:
data = "SGVsbG8sIGxldCdzIG1lZXQgdG9tb3Jyb3cgYXQgMTBhbQ=="
이게 뭔가?
Base64 URL-safe 인코딩된 텍스트
왜? 이메일 본문에 특수문자, 이모지, 줄바꿈 등이 있어서
디코딩 과정:
import base64

data = "SGVsbG8sIGxldCdzIG1lZXQgdG9tb3Jyb3cgYXQgMTBhbQ=="

# 1단계: base64 디코딩
decoded = base64.urlsafe_b64decode(data.encode("ASCII"))
# 결과: b"Hello, let's meet tomorrow at 10am"

# 2단계: UTF-8로 문자열 변환
text = decoded.decode("utf-8")
# 결과: "Hello, let's meet tomorrow at 10am"
실제 예시 (한글):
# Gmail API가 보내는 것
data = "7JWI64WV7ZWY7IS47JqULCDrgrTsnbwg66-46rO866as64uI64uk77yB"

# 디코딩 후
text = "안녕하세요, 내일 뵙겠습니다!"
왜 꼭 필요한가?
없으면 어떻게 되나?
# _parse_message 없으면
subject = None
for h in message["payload"]["headers"]:
    if h["name"] == "Subject":
        subject = h["value"]
        break
# ↑ 매번 이렇게 해야 함

# _get_body 없으면
if "parts" in payload:
    if payload["parts"][0]["mimeType"] == "text/plain":
        body = payload["parts"][0]["body"]["data"]
    # 하지만 parts가 더 중첩되어 있으면? → 에러!

# _decode_body 없으면
print(data)  
# 출력: "SGVsbG8sIGxldCdzIG1lZXQgdG9tb3Jyb3cgYXQgMTBhbQ=="
# 사용자가 읽을 수 없음!
정리
Gmail API 응답 (복잡함)
    ↓ _parse_message
깔끔한 딕셔너리
    ↓ _get_body (재귀 탐색)
Base64 인코딩된 본문
    ↓ _decode_body
읽을 수 있는 텍스트 ✓
한 줄 요약: Gmail API는 데이터를 복잡하고 인코딩된 형태로 보내서, 우리가 쓸 수 있게 변환하는 과정이 필요합니다! 이해되셨나요?