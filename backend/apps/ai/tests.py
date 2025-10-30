"""
AI 앱 스트리밍 뷰 테스트
파일 위치: apps/ai/tests.py
"""

import json
import re
from unittest.mock import patch

from django.contrib.auth import get_user_model
from django.test import TestCase
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APIClient
from rest_framework_simplejwt.tokens import RefreshToken

User = get_user_model()


class MailGenerateStreamViewTest(TestCase):
    """메일 생성 스트리밍 뷰 테스트"""

    def setUp(self):
        self.client = APIClient()
        self.url = reverse("mail-generate-stream")

        self.user = User.objects.create(email="test@example.com", name="Test User")
        refresh = RefreshToken.for_user(self.user)
        self.access_token = str(refresh.access_token)
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {self.access_token}")

        self.valid_payload = {
            "subject": "Meeting Request",
            "body": "I would like to schedule a meeting",
            "to_emails": ["alice@example.com", "bob@example.com"],
        }

    @patch("apps.ai.views.stream_mail_generation")
    def test_mail_generate_stream_success(self, mock_stream):
        def mock_generator():
            yield 'event: ready\ndata: {"ts":1731234567890}\nretry: 5000\n\n'
            yield 'event: subject\nid: 0\ndata: {"title":"Meeting Request","text":"Meeting Request\\n\\n"}\n\n'
            yield 'event: body.delta\nid: 1\ndata: {"seq":0,"text":"Dear colleague, "}\n\n'
            yield 'event: body.delta\nid: 2\ndata: {"seq":1,"text":"I would like to schedule a meeting. "}\n\n'
            yield 'event: done\nid: 3\ndata: {"reason":"stop"}\n\n'

        mock_stream.return_value = mock_generator()

        response = self.client.post(self.url, self.valid_payload, format="json")

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response["Content-Type"], "text/event-stream; charset=utf-8")
        self.assertEqual(response["Cache-Control"], "no-cache")
        self.assertEqual(response["X-Accel-Buffering"], "no")

        content = b"".join(response.streaming_content).decode("utf-8")
        self.assertIn("event: ready", content)
        self.assertIn("event: subject", content)
        self.assertIn("event: body.delta", content)
        self.assertIn("event: done", content)

        # 호출 인자 검증
        kwargs = mock_stream.call_args.kwargs
        self.assertEqual(kwargs.get("user"), self.user)
        self.assertEqual(kwargs.get("subject"), self.valid_payload["subject"])
        self.assertEqual(kwargs.get("body"), self.valid_payload["body"])
        self.assertEqual(kwargs.get("to_emails"), self.valid_payload["to_emails"])

    @patch("apps.ai.views.stream_mail_generation")
    def test_mail_generate_stream_with_error(self, mock_stream):
        def mock_error_generator():
            yield 'event: ready\ndata: {"ts":1731234567890}\nretry: 5000\n\n'
            yield 'event: subject\nid: 0\ndata: {"title":"","text":""}\n\n'
            yield 'event: error\nid: 1\ndata: {"message":"Upstream LLM timeout"}\n\n'
            yield 'event: done\nid: 2\ndata: {"reason":"stop"}\n\n'

        mock_stream.return_value = mock_error_generator()

        response = self.client.post(self.url, self.valid_payload, format="json")
        self.assertEqual(response.status_code, status.HTTP_200_OK)

        content = b"".join(response.streaming_content).decode("utf-8")
        self.assertIn("event: error", content)
        self.assertIn("Upstream LLM timeout", content)

    @patch("apps.ai.views.stream_mail_generation")
    def test_mail_generate_stream_with_ping(self, mock_stream):
        def mock_generator_with_ping():
            yield 'event: ready\ndata: {"ts":1731234567890}\nretry: 5000\n\n'
            yield 'event: subject\nid: 0\ndata: {"title":"Test","text":"Test\\n\\n"}\n\n'
            yield 'event: body.delta\nid: 1\ndata: {"seq":0,"text":"Hello "}\n\n'
            yield "event: ping\ndata: {}\n\n"
            yield 'event: body.delta\nid: 2\ndata: {"seq":1,"text":"World"}\n\n'
            yield 'event: done\nid: 3\ndata: {"reason":"stop"}\n\n'

        mock_stream.return_value = mock_generator_with_ping()

        response = self.client.post(self.url, self.valid_payload, format="json")
        content = b"".join(response.streaming_content).decode("utf-8")
        self.assertIn("event: ping", content)

    def test_mail_generate_stream_missing_to_emails(self):
        """to_emails 누락 → 400"""
        payload = {"subject": "S", "body": "B"}  # to_emails 없음
        response = self.client.post(self.url, payload, format="json")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_mail_generate_stream_empty_to_emails(self):
        """to_emails 빈 배열 → 400"""
        payload = {"subject": "S", "body": "B", "to_emails": []}
        response = self.client.post(self.url, payload, format="json")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    @patch("apps.ai.views.stream_mail_generation")
    def test_mail_generate_stream_blank_subject_body_allowed(self, mock_stream):
        """subject/body는 optional + blank 허용 → 200"""

        def mock_generator():
            yield 'event: ready\ndata: {"ts":1731234567890}\nretry: 5000\n\n'
            yield 'event: subject\nid: 0\ndata: {"title":"","text":""}\n\n'
            yield 'event: done\nid: 1\ndata: {"reason":"stop"}\n\n'

        mock_stream.return_value = mock_generator()

        payload = {"subject": "", "body": "", "to_emails": ["x@example.com"]}
        response = self.client.post(self.url, payload, format="json")
        self.assertEqual(response.status_code, status.HTTP_200_OK)

        kwargs = mock_stream.call_args.kwargs
        self.assertEqual(kwargs["subject"], "")
        self.assertEqual(kwargs["body"], "")
        self.assertEqual(kwargs["to_emails"], ["x@example.com"])

    def test_mail_generate_stream_without_authentication(self):
        self.client.credentials()
        response = self.client.post(self.url, self.valid_payload, format="json")
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    @patch("apps.ai.views.stream_mail_generation")
    def test_mail_generate_stream_id_increments(self, mock_stream):
        def mock_generator():
            yield 'event: ready\ndata: {"ts":1731234567890}\nretry: 5000\n\n'
            yield 'event: subject\nid: 0\ndata: {"title":"Test","text":"Test\\n\\n"}\n\n'
            yield 'event: body.delta\nid: 1\ndata: {"seq":0,"text":"A"}\n\n'
            yield 'event: body.delta\nid: 2\ndata: {"seq":1,"text":"B"}\n\n'
            yield 'event: done\nid: 3\ndata: {"reason":"stop"}\n\n'

        mock_stream.return_value = mock_generator()

        response = self.client.post(self.url, self.valid_payload, format="json")
        content = b"".join(response.streaming_content).decode("utf-8")
        self.assertIn("id: 0", content)
        self.assertIn("id: 1", content)
        self.assertIn("id: 2", content)
        self.assertIn("id: 3", content)


class MailGenerateStreamParsingTest(TestCase):
    """SSE 스트림 파싱 및 데이터 구조 테스트"""

    def setUp(self):
        self.client = APIClient()
        self.url = reverse("mail-generate-stream")

        self.user = User.objects.create(email="test@example.com", name="Test User")
        refresh = RefreshToken.for_user(self.user)
        self.access_token = str(refresh.access_token)
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {self.access_token}")

        self.valid_payload = {"subject": "Test", "body": "Test body", "to_emails": ["x@example.com"]}

    @patch("apps.ai.views.stream_mail_generation")
    def test_parse_sse_events(self, mock_stream):
        def mock_generator():
            yield 'event: ready\ndata: {"ts":1731234567890}\nretry: 5000\n\n'
            yield 'event: subject\nid: 0\ndata: {"title":"Test Title","text":"Test Title\\n\\n"}\n\n'
            yield 'event: body.delta\nid: 1\ndata: {"seq": 0, "text": "Hello"}\n\n'
            yield 'event: done\nid: 2\ndata: {"reason":"stop"}\n\n'

        mock_stream.return_value = mock_generator()

        response = self.client.post(self.url, self.valid_payload, format="json")
        content = b"".join(response.streaming_content).decode("utf-8")

        events = [e.strip() for e in content.split("\n\n") if e.strip()]
        self.assertGreaterEqual(len(events), 4)
        self.assertIn("event: ready", events[0])
        self.assertIn("retry: 5000", events[0])
        self.assertIn("event: subject", events[1])
        self.assertIn("id: 0", events[1])
        self.assertIn("event: body.delta", events[2])
        self.assertIn('"seq": 0', events[2])
        self.assertIn("event: done", events[3])
        self.assertIn('"reason":"stop"', events[3])

    @patch("apps.ai.views.stream_mail_generation")
    def test_json_data_validity(self, mock_stream):
        def mock_generator():
            yield 'event: ready\ndata: {"ts":1731234567890}\nretry: 5000\n\n'
            yield 'event: subject\nid: 0\ndata: {"title":"Test","text":"Test\\n\\n"}\n\n'
            yield 'event: body.delta\nid: 1\ndata: {"seq":0,"text":"Hello"}\n\n'
            yield 'event: done\nid: 2\ndata: {"reason":"stop"}\n\n'

        mock_stream.return_value = mock_generator()

        response = self.client.post(self.url, self.valid_payload, format="json")
        content = b"".join(response.streaming_content).decode("utf-8")

        data_pattern = re.compile(r"data: ({.*?})\n", re.DOTALL)
        matches = data_pattern.findall(content)

        for match in matches:
            try:
                parsed = json.loads(match)
                self.assertIsInstance(parsed, dict)
            except json.JSONDecodeError:
                self.fail(f"Invalid JSON in data field: {match}")


class ReplyOptionsStreamViewTest(TestCase):
    """답장 선택지 스트리밍 뷰 테스트"""

    def setUp(self):
        self.client = APIClient()
        self.url = reverse("mail-reply-stream")

        self.user = User.objects.create(email="test@example.com", name="Test User")
        refresh = RefreshToken.for_user(self.user)
        self.access_token = str(refresh.access_token)
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {self.access_token}")

        self.valid_payload = {
            "subject": "Re: Meeting",
            "body": "Thanks for reaching out. Let's coordinate.",
            "to_email": "origin@example.com",
        }

    @patch("apps.ai.views.stream_reply_options_llm")
    def test_reply_options_stream_success(self, mock_stream):
        def mock_generator():
            yield 'event: ready\ndata: {"ts":1731234567890}\nretry: 5000\n\n'
            yield (
                "event: options\nid: 1\n"
                'data: {"count":2,"items":[{"id":0,"type":"긍정형","title":"네, 가능합니다"},'
                '{"id":1,"type":"일정조율형","title":"대체 시간 제안"}]}\n\n'
            )
            yield 'event: option.delta\nid: 2\ndata: {"id":0,"seq":0,"text":"안녕하세요, "} \n\n'
            yield 'event: option.delta\nid: 3\ndata: {"id":1,"seq":0,"text":"안녕하세요. 메일 확인했습니다. "} \n\n'
            yield 'event: option.done\nid: 4\ndata: {"id":0,"total_seq":10}\n\n'
            yield 'event: option.done\nid: 5\ndata: {"id":1,"total_seq":12}\n\n'
            yield 'event: done\nid: 6\ndata: {"reason":"all_options_finished"}\n\n'

        mock_stream.return_value = mock_generator()

        response = self.client.post(self.url, self.valid_payload, format="json")

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response["Content-Type"], "text/event-stream; charset=utf-8")
        self.assertEqual(response["Cache-Control"], "no-cache")
        self.assertEqual(response["X-Accel-Buffering"], "no")

        content = b"".join(response.streaming_content).decode("utf-8")
        self.assertIn("event: options", content)
        self.assertIn("event: option.delta", content)
        self.assertIn("event: option.done", content)
        self.assertIn("event: done", content)

        kwargs = mock_stream.call_args.kwargs
        self.assertEqual(kwargs.get("user"), self.user)
        self.assertEqual(kwargs.get("subject"), self.valid_payload["subject"])
        self.assertEqual(kwargs.get("body"), self.valid_payload["body"])
        self.assertEqual(kwargs.get("to_email"), self.valid_payload["to_email"])

    @patch("apps.ai.views.stream_reply_options_llm")
    def test_reply_options_stream_with_ping(self, mock_stream):
        def mock_generator():
            yield 'event: ready\ndata: {"ts":1731234567890}\nretry: 5000\n\n'
            yield 'event: options\nid: 1\ndata: {"count":1,"items":[{"id":0,"type":"정보요청","title":"정보 요청"}]}\n\n'
            yield 'event: option.delta\nid: 2\ndata: {"id":0,"seq":0,"text":"문의 주셔서 감사합니다. "} \n\n'
            yield "event: ping\nid: 3\ndata: {}\n\n"
            yield 'event: option.done\nid: 4\ndata: {"id":0,"total_seq":5}\n\n'
            yield 'event: done\nid: 5\ndata: {"reason":"all_options_finished"}\n\n'

        mock_stream.return_value = mock_generator()

        response = self.client.post(self.url, self.valid_payload, format="json")
        content = b"".join(response.streaming_content).decode("utf-8")
        self.assertIn("event: ping", content)

    def test_reply_options_stream_without_authentication(self):
        self.client.credentials()
        response = self.client.post(self.url, self.valid_payload, format="json")
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_reply_options_stream_missing_required_fields(self):
        """subject/body/to_email 모두 required → 누락 시 400"""
        invalid_payload = {}
        response = self.client.post(self.url, invalid_payload, format="json")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    @patch("apps.ai.views.stream_reply_options_llm")
    def test_reply_options_stream_id_increments(self, mock_stream):
        def mock_generator():
            yield 'event: ready\nid: 0\ndata: {"ts":1731234567890}\nretry: 5000\n\n'
            yield 'event: options\nid: 1\ndata: {"count":1,"items":[{"id":0,"type":"긍정형","title":"수락"}]}\n\n'
            yield 'event: option.delta\nid: 2\ndata: {"id":0,"seq":0,"text":"네, 가능합니다."}\n\n'
            yield 'event: option.done\nid: 3\ndata: {"id":0,"total_seq":1}\n\n'
            yield 'event: done\nid: 4\ndata: {"reason":"all_options_finished"}\n\n'

        mock_stream.return_value = mock_generator()

        response = self.client.post(self.url, self.valid_payload, format="json")
        content = b"".join(response.streaming_content).decode("utf-8")
        self.assertIn("id: 1", content)
        self.assertIn("id: 2", content)
        self.assertIn("id: 3", content)
        self.assertIn("id: 4", content)
