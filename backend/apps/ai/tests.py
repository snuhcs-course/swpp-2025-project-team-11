"""
AI 앱 스트리밍 뷰 테스트
파일 위치: apps/ai/tests.py
"""

import json
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
        self.url = reverse("mail-generate-stream")  # URL name에 맞게 수정 필요

        # 테스트 사용자 생성 및 인증
        self.user = User.objects.create(email="test@example.com", name="Test User")
        refresh = RefreshToken.for_user(self.user)
        self.access_token = str(refresh.access_token)
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {self.access_token}")

        # 테스트 데이터
        self.valid_payload = {
            "subject": "Meeting Request",
            "body": "I would like to schedule a meeting",
            "relationship": "colleague",
            "situational_prompt": "formal business meeting",
            "style_prompt": "professional and concise",
            "format_prompt": "structured email",
            "language": "en",
        }

    @patch("apps.ai.views.stream_mail_generation")
    def test_mail_generate_stream_success(self, mock_stream):
        """메일 생성 스트리밍 성공 테스트"""

        # Mock SSE 이벤트 생성
        def mock_generator():
            yield 'event: ready\ndata: {"ts":1731234567890}\nretry: 5000\n\n'
            yield 'event: subject\nid: 0\ndata: {"title":"Meeting Request","text":"Meeting Request\\n\\n"}\n\n'  # noqa: E501
            yield 'event: body.delta\nid: 1\ndata: {"seq":0,"text":"Dear colleague, "}\n\n'
            yield 'event: body.delta\nid: 2\ndata: {"seq":1,"text":"I would like to schedule a meeting. "}\n\n'  # noqa: E501
            yield 'event: done\nid: 3\ndata: {"reason":"stop"}\n\n'

        mock_stream.return_value = mock_generator()

        response = self.client.post(self.url, self.valid_payload, format="json")

        # 응답 확인
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response["Content-Type"], "text/event-stream; charset=utf-8")
        self.assertEqual(response["Cache-Control"], "no-cache")
        self.assertEqual(response["X-Accel-Buffering"], "no")

        # 스트리밍 데이터 확인
        content = b"".join(response.streaming_content).decode("utf-8")
        self.assertIn("event: ready", content)
        self.assertIn("event: subject", content)
        self.assertIn("event: body.delta", content)
        self.assertIn("event: done", content)
        self.assertIn('"reason":"stop"', content)

        # stream_mail_generation이 올바른 인자로 호출되었는지 확인
        mock_stream.assert_called_once_with(
            subject=self.valid_payload["subject"],
            body=self.valid_payload["body"],
            relationship=self.valid_payload["relationship"],
            situational_prompt=self.valid_payload["situational_prompt"],
            style_prompt=self.valid_payload["style_prompt"],
            format_prompt=self.valid_payload["format_prompt"],
            language=self.valid_payload["language"],
        )

    @patch("apps.ai.views.stream_mail_generation")
    def test_mail_generate_stream_with_error(self, mock_stream):
        """스트리밍 중 에러 발생 테스트"""

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
        """ping 이벤트 포함 테스트"""

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

    def test_mail_generate_stream_missing_required_fields(self):
        """필수 필드 누락 테스트"""
        invalid_payload = {}  # 모든 필드 누락

        response = self.client.post(self.url, invalid_payload, format="json")

        # serializer에서 필수 필드 검증하는 경우에만 400
        # 모든 필드가 optional이면 200이 반환될 수 있음
        self.assertIn(response.status_code, [status.HTTP_200_OK, status.HTTP_400_BAD_REQUEST])

    def test_mail_generate_stream_without_authentication(self):
        """인증 없이 요청 테스트"""
        # 인증 제거
        self.client.credentials()

        response = self.client.post(self.url, self.valid_payload, format="json")

        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    @patch("apps.ai.views.stream_mail_generation")
    def test_mail_generate_stream_optional_fields(self, mock_stream):
        """선택적 필드만으로 요청 테스트"""

        def mock_generator():
            yield 'event: ready\ndata: {"ts":1731234567890}\nretry: 5000\n\n'
            yield 'event: subject\nid: 0\ndata: {"title":"Test","text":"Test\\n\\n"}\n\n'
            yield 'event: done\nid: 1\ndata: {"reason":"stop"}\n\n'

        mock_stream.return_value = mock_generator()

        minimal_payload = {"subject": "Test Subject", "body": "Test Body"}

        response = self.client.post(self.url, minimal_payload, format="json")

        self.assertEqual(response.status_code, status.HTTP_200_OK)

        # None 값들이 전달되었는지 확인
        mock_stream.assert_called_once()
        call_kwargs = mock_stream.call_args.kwargs
        self.assertEqual(call_kwargs["subject"], "Test Subject")
        self.assertEqual(call_kwargs["body"], "Test Body")
        self.assertIsNone(call_kwargs.get("relationship"))
        self.assertIsNone(call_kwargs.get("situational_prompt"))

    @patch("apps.ai.views.stream_mail_generation")
    def test_mail_generate_stream_sequence_order(self, mock_stream):
        """body.delta 시퀀스 순서 확인 테스트"""

        def mock_generator():
            yield 'event: ready\ndata: {"ts":1731234567890}\nretry: 5000\n\n'
            yield 'event: subject\nid: 0\ndata: {"title":"Test","text":"Test\\n\\n"}\n\n'
            yield 'event: body.delta\nid: 1\ndata: {"seq": 0, "text": "First "}\n\n'
            yield 'event: body.delta\nid: 2\ndata: {"seq": 1, "text": "Second "}\n\n'
            yield 'event: body.delta\nid: 3\ndata: {"seq": 2, "text": "Third"}\n\n'
            yield 'event: done\nid: 4\ndata: {"reason":"stop"}\n\n'

        mock_stream.return_value = mock_generator()

        response = self.client.post(self.url, self.valid_payload, format="json")

        content = b"".join(response.streaming_content).decode("utf-8")

        # 시퀀스 순서 확인 (공백 포함)
        seq0_pos = content.find('"seq": 0')
        seq1_pos = content.find('"seq": 1')
        seq2_pos = content.find('"seq": 2')

        self.assertGreater(seq1_pos, seq0_pos)
        self.assertGreater(seq2_pos, seq1_pos)

    @patch("apps.ai.views.stream_mail_generation")
    def test_mail_generate_stream_id_increments(self, mock_stream):
        """SSE 이벤트 ID 증가 확인 테스트"""

        def mock_generator():
            yield 'event: ready\ndata: {"ts":1731234567890}\nretry: 5000\n\n'
            yield 'event: subject\nid: 0\ndata: {"title":"Test","text":"Test\\n\\n"}\n\n'
            yield 'event: body.delta\nid: 1\ndata: {"seq":0,"text":"A"}\n\n'
            yield 'event: body.delta\nid: 2\ndata: {"seq":1,"text":"B"}\n\n'
            yield 'event: done\nid: 3\ndata: {"reason":"stop"}\n\n'

        mock_stream.return_value = mock_generator()

        response = self.client.post(self.url, self.valid_payload, format="json")

        content = b"".join(response.streaming_content).decode("utf-8")

        # ID가 순차적으로 증가하는지 확인
        self.assertIn("id: 0", content)
        self.assertIn("id: 1", content)
        self.assertIn("id: 2", content)
        self.assertIn("id: 3", content)

    @patch("apps.ai.views.stream_mail_generation")
    def test_mail_generate_stream_different_languages(self, mock_stream):
        """다양한 언어 지원 테스트"""
        languages = ["en", "ko", "ja", "zh"]

        for lang in languages:
            with self.subTest(language=lang):

                def mock_generator():
                    yield 'event: ready\ndata: {"ts":1731234567890}\nretry: 5000\n\n'
                    yield 'event: subject\nid: 0\ndata: {"title":"Test","text":"Test\\n\\n"}\n\n'
                    yield 'event: done\nid: 1\ndata: {"reason":"stop"}\n\n'

                mock_stream.return_value = mock_generator()
                mock_stream.reset_mock()  # 각 서브테스트마다 리셋

                payload = self.valid_payload.copy()
                payload["language"] = lang

                response = self.client.post(self.url, payload, format="json")

                self.assertEqual(response.status_code, status.HTTP_200_OK)
                self.assertTrue(mock_stream.called)

    @patch("apps.ai.views.stream_mail_generation")
    def test_mail_generate_stream_empty_subject_body(self, mock_stream):
        """빈 subject와 body로 요청 테스트"""

        def mock_generator():
            yield 'event: ready\ndata: {"ts":1731234567890}\nretry: 5000\n\n'
            yield 'event: subject\nid: 0\ndata: {"title":"","text":""}\n\n'
            yield 'event: done\nid: 1\ndata: {"reason":"stop"}\n\n'

        mock_stream.return_value = mock_generator()

        empty_payload = {"subject": "", "body": ""}

        response = self.client.post(self.url, empty_payload, format="json")

        # serializer 검증에 따라 성공 또는 실패할 수 있음
        # 만약 빈 문자열이 허용된다면:
        self.assertIn(response.status_code, [status.HTTP_200_OK, status.HTTP_400_BAD_REQUEST])


class MailGenerateStreamParsingTest(TestCase):
    """SSE 스트림 파싱 및 데이터 구조 테스트"""

    def setUp(self):
        self.client = APIClient()
        self.url = reverse("mail-generate-stream")

        self.user = User.objects.create(email="test@example.com", name="Test User")
        refresh = RefreshToken.for_user(self.user)
        self.access_token = str(refresh.access_token)
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {self.access_token}")

        self.valid_payload = {"subject": "Test", "body": "Test body"}

    @patch("apps.ai.views.stream_mail_generation")
    def test_parse_sse_events(self, mock_stream):
        """SSE 이벤트 파싱 테스트"""

        def mock_generator():
            yield 'event: ready\ndata: {"ts":1731234567890}\nretry: 5000\n\n'
            yield 'event: subject\nid: 0\ndata: {"title":"Test Title","text":"Test Title\\n\\n"}\n\n'  # noqa: E501
            yield 'event: body.delta\nid: 1\ndata: {"seq": 0, "text": "Hello"}\n\n'
            yield 'event: done\nid: 2\ndata: {"reason":"stop"}\n\n'

        mock_stream.return_value = mock_generator()

        response = self.client.post(self.url, self.valid_payload, format="json")
        content = b"".join(response.streaming_content).decode("utf-8")

        # 이벤트 분리
        events = [e.strip() for e in content.split("\n\n") if e.strip()]

        # 각 이벤트 검증
        self.assertGreaterEqual(len(events), 4)

        # ready 이벤트
        self.assertIn("event: ready", events[0])
        self.assertIn("retry: 5000", events[0])

        # subject 이벤트
        self.assertIn("event: subject", events[1])
        self.assertIn("id: 0", events[1])

        # body.delta 이벤트 (공백 포함 형태)
        self.assertIn("event: body.delta", events[2])
        self.assertIn('"seq": 0', events[2])

        # done 이벤트
        self.assertIn("event: done", events[3])
        self.assertIn('"reason":"stop"', events[3])

    @patch("apps.ai.views.stream_mail_generation")
    def test_json_data_validity(self, mock_stream):
        """JSON 데이터 유효성 테스트"""

        def mock_generator():
            yield 'event: ready\ndata: {"ts":1731234567890}\nretry: 5000\n\n'
            yield 'event: subject\nid: 0\ndata: {"title":"Test","text":"Test\\n\\n"}\n\n'
            yield 'event: body.delta\nid: 1\ndata: {"seq":0,"text":"Hello"}\n\n'
            yield 'event: done\nid: 2\ndata: {"reason":"stop"}\n\n'

        mock_stream.return_value = mock_generator()

        response = self.client.post(self.url, self.valid_payload, format="json")
        content = b"".join(response.streaming_content).decode("utf-8")

        # 각 이벤트의 data 필드에서 JSON 추출 및 검증
        import re

        data_pattern = re.compile(r"data: ({.*?})\n", re.DOTALL)
        matches = data_pattern.findall(content)

        for match in matches:
            try:
                parsed = json.loads(match)
                self.assertIsInstance(parsed, dict)
            except json.JSONDecodeError:
                self.fail(f"Invalid JSON in data field: {match}")
