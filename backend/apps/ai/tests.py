import json
import re
import unittest
from unittest.mock import MagicMock, patch

from django.contrib.auth import get_user_model
from django.test import SimpleTestCase, TestCase
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APIClient
from rest_framework_simplejwt.tokens import RefreshToken

from apps.ai.models import ContactAnalysisResult, GroupAnalysisResult
from apps.ai.services import pii_masker as pm
from apps.ai.services.attachment_analysis import analyze_gmail_attachment, analyze_uploaded_file
from apps.ai.services.mail_generation import (
    debug_mail_generation_analysis,
    stream_mail_generation,
    stream_mail_generation_test,
    stream_mail_generation_with_plan,
    stream_mail_generation_with_timestamp,
)
from apps.ai.services.utils import (
    _fetch_analysis_for_group,
    _fetch_analysis_for_single,
    build_prompt_inputs,
    collect_prompt_context,
    heartbeat,
    sse_event,
)
from apps.contact.models import Contact, ContactContext, Group, PromptOption

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


def _drain(gen):
    return list(gen)


class TestStreamMailGeneration(unittest.IsolatedAsyncioTestCase):

    @patch("apps.ai.services.mail_generation.sse_event")
    @patch("apps.ai.services.mail_generation.heartbeat")
    @patch("apps.ai.services.mail_generation.unmask_stream")
    @patch("apps.ai.services.mail_generation.body_chain")
    @patch("apps.ai.services.mail_generation.subject_chain")
    @patch("apps.ai.services.mail_generation.PiiMasker")
    @patch("apps.ai.services.mail_generation.make_req_id", return_value="REQ1")
    @patch("apps.ai.services.mail_generation.build_prompt_inputs")
    @patch("apps.ai.services.mail_generation.collect_prompt_context")
    async def test_stream_mail_generation(
        self,
        mock_collect,
        mock_build_inputs,
        mock_req_id,
        MockMasker,
        mock_subject_chain,
        mock_body_chain,
        mock_unmask,
        mock_heartbeat,
        mock_sse,
    ):
        mock_collect.return_value = {"user": "u"}
        mock_build_inputs.return_value = {"language": "ko", "recipients": ["a@a.com"]}
        masker_instance = MockMasker.return_value
        masker_instance.mask_inputs.return_value = ({"body": "MASKED_BODY"}, {"a": "b"})
        mock_subject_chain.invoke.return_value = "MASKED_TITLE"
        mock_body_chain.stream.return_value = iter(["BODY1", "BODY2"])
        mock_unmask.side_effect = lambda x, *_: x
        mock_sse.side_effect = lambda evt, data, eid=None, retry_ms=None: f"SSE:{evt}"

        gen = stream_mail_generation(
            user={"id": 1},
            subject="hello",
            body="world",
            to_emails=["a@a.com"],
        )

        result = []
        async for ev in gen:
            result.append(ev)

        self.assertIn("SSE:ready", result[0])
        self.assertIn("SSE:subject", result[1])
        self.assertIn("SSE:body.delta", result[2])
        self.assertIn("SSE:body.delta", result[3])
        self.assertIn("SSE:done", result[-1])
        mock_collect.assert_called_once()
        mock_build_inputs.assert_called_once()
        mock_subject_chain.invoke.assert_called_once()
        mock_body_chain.stream.assert_called_once()


class StreamMailGenerationTestCase(TestCase):

    def test_stream_mail_generation_test(self):
        gen = stream_mail_generation_test()

        events = list(gen)
        self.assertGreater(len(events), 5, "스트림 이벤트가 너무 적음 — 제대로 작동하지 않는 것 같음")

        first = events[0]
        self.assertIn("event: ready", first)
        self.assertIn("data:", first)
        self.assertIn("ts", first)

        second = events[1]
        self.assertIn("event: subject", second)
        self.assertIn("Important Update", second)

        body_events = [ev for ev in events if "event: body.delta" in ev]
        self.assertGreater(len(body_events), 3, "body.delta 이벤트가 충분히 출력되지 않음")

        sample_body = body_events[0]
        self.assertIn("data:", sample_body)
        self.assertIn("seq", sample_body)
        self.assertIn("text", sample_body)

        last = events[-1]
        self.assertIn("event: done", last)
        self.assertIn("reason", last)

        order = []
        for ev in events:
            for line in ev.split("\n"):
                if line.startswith("event:"):
                    order.append(line)
                    break

        self.assertIn("event: ready", order[0])
        self.assertIn("event: subject", order[1])
        self.assertIn("event: done", order[-1])


class TestStreamMailGenerationWithTimestamp(unittest.IsolatedAsyncioTestCase):

    @patch("apps.ai.services.mail_generation.sse_event")
    @patch("apps.ai.services.mail_generation.heartbeat")
    @patch("apps.ai.services.mail_generation.unmask_stream")
    @patch("apps.ai.services.mail_generation.body_chain")
    @patch("apps.ai.services.mail_generation.subject_chain")
    @patch("apps.ai.services.mail_generation.PiiMasker")
    @patch("apps.ai.services.mail_generation.make_req_id", return_value="REQ_TS")
    @patch("apps.ai.services.mail_generation.build_prompt_inputs")
    @patch("apps.ai.services.mail_generation.collect_prompt_context")
    async def test_stream_mail_generation_with_timestamp(
        self,
        mock_collect,
        mock_build_inputs,
        mock_req_id,
        MockMasker,
        mock_subject_chain,
        mock_body_chain,
        mock_unmask,
        mock_heartbeat,
        mock_sse,
    ):
        mock_collect.return_value = {"user": "u"}
        mock_build_inputs.return_value = {"language": "ko", "recipients": ["a@a.com"]}
        masker_instance = MockMasker.return_value
        masker_instance.mask_inputs.return_value = ({"body": "MASKED_BODY"}, {"a": "b"})
        mock_subject_chain.invoke.return_value = "MASKED_TITLE"
        mock_body_chain.stream.return_value = iter(["BODY1", "BODY2"])
        mock_unmask.side_effect = lambda x, *_: x
        mock_sse.side_effect = lambda evt, data, eid=None, retry_ms=None: f"SSE:{evt}"

        gen = stream_mail_generation_with_timestamp(
            user={"id": 1},
            subject="hello",
            body="world",
            to_emails=["a@a.com"],
        )

        result = []
        async for ev in gen:
            result.append(ev)

        self.assertIn("SSE:ready", result[0])
        self.assertIn("SSE:subject", result[1])
        self.assertIn("SSE:body.delta", result[2])
        self.assertIn("SSE:body.delta", result[3])
        self.assertIn("SSE:done", result[-1])


class TestStreamMailGenerationWithPlan(unittest.IsolatedAsyncioTestCase):

    @patch("apps.ai.services.mail_generation.sse_event")
    @patch("apps.ai.services.mail_generation.heartbeat")
    @patch("apps.ai.services.mail_generation.unmask_stream")
    @patch("apps.ai.services.mail_generation.body_chain")
    @patch("apps.ai.services.mail_generation.plan_chain")
    @patch("apps.ai.services.mail_generation.validator_chain")
    @patch("apps.ai.services.mail_generation.mail_graph")
    async def test_stream_mail_generation_with_plan(
        self,
        mock_graph,
        mock_validator,
        mock_plan_chain,
        mock_body_chain,
        mock_unmask,
        mock_heartbeat,
        mock_sse,
    ):
        mock_graph.invoke.return_value = {
            "req_id": "REQ_PLAN",
            "mask_mapping": {"a": "b"},
            "masked_inputs": {"body": "MASKED_BODY"},
            "body_inputs": {"body": "MASKED_BODY"},
            "locked_title": "MASKED_TITLE",
        }
        mock_plan_chain.stream.return_value = iter(["PLAN1", "PLAN2"])
        mock_body_chain.stream.return_value = iter(["BODY1", "BODY2"])
        mock_unmask.side_effect = lambda x, *_: x
        mock_validator.invoke.return_value = MagicMock(passed=True)
        mock_sse.side_effect = lambda evt, data, eid=None, retry_ms=None: f"SSE:{evt}"

        gen = stream_mail_generation_with_plan(
            user={"id": 1},
            subject="hello",
            body="world",
            to_emails=["a@a.com"],
        )

        result = []
        async for ev in gen:
            result.append(ev)

        self.assertIn("SSE:ready", result[0])
        self.assertIn("SSE:plan.start", result[1])
        self.assertIn("SSE:plan.delta", result[2])
        self.assertIn("SSE:plan.done", [r for r in result if "SSE:plan.done" in r][0])
        self.assertIn("SSE:subject", [r for r in result if "SSE:subject" in r][0])
        self.assertIn("SSE:body.start", [r for r in result if "SSE:body.start" in r][0])
        self.assertIn("SSE:body.delta", [r for r in result if "SSE:body.delta" in r][0])
        self.assertIn("SSE:done", result[-1])


class TestDebugMailGenerationAnalysis(unittest.TestCase):

    @patch("apps.ai.services.mail_generation.subject_chain")
    @patch("apps.ai.services.mail_generation.body_chain")
    @patch("apps.ai.services.mail_generation.PiiMasker")
    @patch("apps.ai.services.mail_generation.make_req_id", return_value="REQ_DBG")
    @patch("apps.ai.services.mail_generation.build_prompt_inputs")
    @patch("apps.ai.services.mail_generation.collect_prompt_context")
    def test_debug_mail_generation_analysis(
        self,
        mock_collect,
        mock_build_inputs,
        mock_req_id,
        MockMasker,
        mock_body_chain,
        mock_subject_chain,
    ):
        mock_collect.return_value = {"analysis": "ANALYSIS", "fewshots": "FEWSHOTS"}
        mock_build_inputs.return_value = {"language": "ko", "recipients": ["a@a.com"]}
        masker_instance = MockMasker.return_value
        masker_instance.mask_inputs.return_value = ({"body": "MASKED_BODY"}, {"a": "b"})
        mock_subject_chain.invoke.return_value = "MASKED_TITLE"
        mock_body_chain.invoke.return_value = "BODY_CONTENT"

        result = debug_mail_generation_analysis(
            user={"id": 1},
            subject="hello",
            body="world",
            to_emails=["a@a.com"],
        )

        self.assertIn("analysis", result)
        self.assertIn("fewshots", result)
        self.assertIn("without_analysis", result)
        self.assertIn("with_analysis", result)
        self.assertIn("with_fewshots", result)
        self.assertEqual(result["without_analysis"]["subject"], "MASKED_TITLE")
        self.assertEqual(result["without_analysis"]["body"], "BODY_CONTENT")


class PiiMaskerAndUnmaskTest(SimpleTestCase):
    """
    pii_masker.py 동작 테스트
    - PiiMasker.mask_text() 가 이메일/번호 등 민감정보를 placeholder 로 치환하는지
    - 그 placeholder 가 unmask_once()/unmask_stream() 으로 원복되는지
    """

    def test_mask_and_unmask_roundtrip_single_text(self):
        req_id = "abcdef123456"
        masker = pm.PiiMasker(req_id)

        original = "Contact me at hello@example.com or +82-10-1234-5678"
        masked, mapping = masker.mask_text(original)

        # placeholder가 들어갔는지
        self.assertIn("PII:", masked)
        self.assertNotEqual(masked, original)
        self.assertTrue(mapping)

        restored = pm.unmask_once(masked, req_id, mapping)
        self.assertEqual(restored, original)

    def test_unmask_stream_reassembles_chunks(self):
        req_id = "req999888777"
        masker = pm.PiiMasker(req_id)

        text = "My token is secret@example.com thanks."
        masked, mapping = masker.mask_text(text)

        # 마스크된 텍스트를 조각내서 스트림처럼 넣어보기
        chunks = [masked[:8], masked[8:16], masked[16:]]
        out_chunks = list(pm.unmask_stream(chunks, req_id, mapping))
        reconstructed = "".join(out_chunks)

        self.assertEqual(reconstructed, text)

    def test_make_req_id_length(self):
        rid = pm.make_req_id()
        self.assertEqual(len(rid), 12)  # uuid.uuid4().hex[:12] 보장


# =========================
# sse_event / heartbeat
# =========================
class UtilsSSEEventTest(SimpleTestCase):
    def test_sse_event_basic(self):
        out = sse_event("ready", {"ts": 123})
        # 줄바꿈 규격 확인
        self.assertTrue(out.endswith("\n\n"))
        # event / data 라인이 포함돼야 함
        self.assertIn("event: ready", out)
        self.assertIn('data: {"ts": 123}', out)
        # id / retry 가 없으면 나오면 안 됨
        self.assertNotIn("id:", out)
        self.assertNotIn("retry:", out)

    def test_sse_event_with_id_and_retry(self):
        out = sse_event("body.delta", {"seq": 2, "text": "hello"}, eid="7", retry_ms=5000)
        # 순서: id -> retry -> event -> data
        lines = [line for line in out.strip().split("\n")]
        self.assertEqual(lines[0], "id: 7")
        self.assertEqual(lines[1], "retry: 5000")
        self.assertEqual(lines[2], "event: body.delta")
        self.assertIn('"seq": 2', lines[3])
        self.assertIn('"text": "hello"', lines[3])

    def test_heartbeat_returns_comment_frame(self):
        hb = heartbeat()
        # SSE comment frame 은 ":" 로 시작하고 빈 줄로 끝나는 패턴 사용
        self.assertEqual(hb, ":\n\n")


# =========================
# build_prompt_inputs
# =========================
class BuildPromptInputsTest(SimpleTestCase):
    def test_build_prompt_inputs_basic_merge(self):
        ctx = {
            "recipients": ["Alice", "Bob"],
            "group_name": "Team A ",
            "group_description": "  internal            ",
            "prompt_options": [
                " Please be polite. ",
                "Use formal tone.",
                "   ",  # 빈 줄은 무시
            ],
            "personal_prompt": "  Add some gratitude. ",
            "sender_role": "  Manager ",
            "recipient_role": "  Client ",
            "language": "ko-KR",
            "analysis": dict(
                lexical_style={
                    "summary": "정중하고 격식 있는 표현을 주로 사용하며, 연결어와 완곡한 어휘를 적절히 사용해 안정적인 어조를 유지하는 편입니다.",
                    "top_connectives": "‘그리고’, ‘또한’, ‘따라서’와 같은 기본 연결어를 활용해 문장을 자연스럽게 이어갑니다.",
                    "frequent_phrases": "‘부탁드립니다’, ‘검토 후 회신 요청드립니다’ 등이 반복적으로 등장합니다.",
                    "slang_or_chat_markers": "구어체나 이모지는 거의 사용되지 않습니다.",
                    "politeness_lexemes": "‘부탁드립니다’, ‘감사합니다’ 등의 공손 표현을 자주 사용합니다.",
                },
                grammar_patterns={
                    "summary": "존댓말 중심으로 간결하게 구성됩니다.",
                    "ender_distribution": "‘-습니다’, ‘-드립니다’ 중심.",
                    "sentence_length": "짧고 명료한 문장이 많음.",
                    "sentence_type_ratio": "서술문 위주, 간접 명령형 일부 포함.",
                    "structure_pattern": "인사 → 요청 → 확인 요청 → 마무리 구조.",
                    "paragraph_stats": "짧은 단락 위주.",
                },
                emotional_tone={
                    "summary": "중립적이고 전문적인 톤.",
                    "overall": "neutral_formal",
                    "formality_level": "높음",
                    "politeness_level": "높음",
                    "directness_score": "중간",
                    "warmth_score": "중간",
                    "speech_act_distribution": "요청·정보 제공 중심",
                    "request_style": "간접적 요청",
                    "notes": "정중하고 실무적인 문체.",
                },
                representative_sentences=[
                    "안녕하세요, 회의 일정 조율 부탁드립니다.",
                    "검토 후 회신 부탁드립니다.",
                    "관련 자료를 확인하신 후 공유 요청드립니다.",
                ],
            ),
        }

        out = build_prompt_inputs(ctx)

        # recipients 그대로
        self.assertEqual(out["recipients"], ["Alice", "Bob"])

        # group_name/group_description은 strip 돼서 들어가야 함
        self.assertEqual(out["group_name"], "Team A")
        self.assertEqual(out["group_description"], "internal")

        # sender/recipient role strip
        self.assertEqual(out["sender_role"], "Manager")
        self.assertEqual(out["recipient_role"], "Client")

        # language 그대로
        self.assertEqual(out["language"], "ko-KR")

        # prompt_text는 prompt_options + "" + personal_prompt 가 \n 으로 join 된 형태
        # 공백 라인("   ")은 제거
        self.assertEqual(
            out["prompt_text"],
            "Please be polite.\nUse formal tone.\n\nAdd some gratitude.",
        )

        # fewshots 그대로 패스
        self.assertEqual(
            out["analysis"],
            dict(
                lexical_style={
                    "summary": "정중하고 격식 있는 표현을 주로 사용하며, 연결어와 완곡한 어휘를 적절히 사용해 안정적인 어조를 유지하는 편입니다.",
                    "top_connectives": "‘그리고’, ‘또한’, ‘따라서’와 같은 기본 연결어를 활용해 문장을 자연스럽게 이어갑니다.",
                    "frequent_phrases": "‘부탁드립니다’, ‘검토 후 회신 요청드립니다’ 등이 반복적으로 등장합니다.",
                    "slang_or_chat_markers": "구어체나 이모지는 거의 사용되지 않습니다.",
                    "politeness_lexemes": "‘부탁드립니다’, ‘감사합니다’ 등의 공손 표현을 자주 사용합니다.",
                },
                grammar_patterns={
                    "summary": "존댓말 중심으로 간결하게 구성됩니다.",
                    "ender_distribution": "‘-습니다’, ‘-드립니다’ 중심.",
                    "sentence_length": "짧고 명료한 문장이 많음.",
                    "sentence_type_ratio": "서술문 위주, 간접 명령형 일부 포함.",
                    "structure_pattern": "인사 → 요청 → 확인 요청 → 마무리 구조.",
                    "paragraph_stats": "짧은 단락 위주.",
                },
                emotional_tone={
                    "summary": "중립적이고 전문적인 톤.",
                    "overall": "neutral_formal",
                    "formality_level": "높음",
                    "politeness_level": "높음",
                    "directness_score": "중간",
                    "warmth_score": "중간",
                    "speech_act_distribution": "요청·정보 제공 중심",
                    "request_style": "간접적 요청",
                    "notes": "정중하고 실무적인 문체.",
                },
                representative_sentences=[
                    "안녕하세요, 회의 일정 조율 부탁드립니다.",
                    "검토 후 회신 부탁드립니다.",
                    "관련 자료를 확인하신 후 공유 요청드립니다.",
                ],
            ),
        )

    def test_build_prompt_inputs_no_language_uses_default(self):
        ctx = {
            "recipients": ["Alice"],
            "prompt_options": [],
            "personal_prompt": None,
            "group_name": None,
            "group_description": None,
            "sender_role": None,
            "recipient_role": None,
            # language 없음
            "fewshots": [],
        }

        out = build_prompt_inputs(ctx)
        self.assertEqual(out["language"], "user's original language")
        self.assertIsNone(out["prompt_text"])

    def test_build_prompt_inputs_personal_prompt_only(self):
        ctx = {
            "recipients": ["Alice"],
            "prompt_options": [],
            "personal_prompt": "   Keep it short.   ",
            "language": "en",
            "fewshots": [],
        }

        out = build_prompt_inputs(ctx)
        # personal_prompt만 있을 때도 prompt_text는 그것만 깔끔하게
        self.assertEqual(out["prompt_text"], "Keep it short.")
        self.assertEqual(out["language"], "en")


class FetchAnalysisForSingleTest(TestCase):
    """_fetch_analysis_for_single 함수 테스트"""

    def setUp(self):
        self.user = User.objects.create(email="u@example.com", name="U1")
        self.group = Group.objects.create(
            user=self.user,
            name="Partners",
            description="Long-term partners",
        )
        self.contact = Contact.objects.create(
            user=self.user,
            group=self.group,
            name="Alice",
            email="alice@example.com",
        )

    def test_fetch_analysis_exists(self):
        """ContactAnalysisResult가 존재할 때 정상 반환"""
        ContactAnalysisResult.objects.create(
            user=self.user,
            contact=self.contact,
            lexical_style={
                "summary": "정중하고 격식 있는 표현을 주로 사용하며, 연결어와 완곡한 어휘를 적절히 사용해 안정적인 어조를 유지하는 편입니다.",
                "top_connectives": "‘그리고’, ‘또한’, ‘따라서’와 같은 기본 연결어를 활용해 문장을 자연스럽게 이어갑니다.",
                "frequent_phrases": "‘부탁드립니다’, ‘검토 후 회신 요청드립니다’ 등 업무 요청 시 자주 사용되는 정중한 관용구가 반복적으로 등장합니다.",  # noqa: E501
                "slang_or_chat_markers": "이모지나 구어체, 인터넷식 표현은 거의 사용하지 않아 전체적으로 격식 있는 톤을 유지합니다.",
                "politeness_lexemes": "‘부탁드립니다’, ‘감사합니다’, ‘죄송하지만’과 같은 공손 표현을 활용해 완곡하고 예의를 갖춘 어조를 형성합니다.",
            },
            grammar_patterns={
                "summary": "문장은 존댓말 종결형이 중심이며 비교적 간결하고 목적 중심적으로 구성됩니다. 요청과 설명이 명확히 구분되어 있어 읽기 편합니다.",  # noqa: E501
                "ender_distribution": "‘-습니다’, ‘-드립니다’와 같은 격식체 종결형이 주로 사용됩니다.",
                "sentence_length": "짧고 명료한 문장을 선호하며, 한 문장 내 복잡한 종속절은 적은 편입니다.",
                "sentence_type_ratio": "서술문이 대부분이며, 요청성 문장(간접 명령형)이 일정 비중을 차지합니다.",
                "structure_pattern": "인사 → 요청 또는 정보 전달 → 필요 시 추가 설명 → 마무리 인사 순으로 안정적인 패턴을 유지합니다.",
                "paragraph_stats": "짧은 단문 중심의 단락으로 구성되며, 단락 간 흐름이 명확해 가독성이 높습니다.",
            },
            emotional_tone={
                "summary": "전체적으로 중립적이고 전문적인 톤이며, 공손함과 명료함을 균형 있게 유지합니다.",
                "overall": "neutral_formal",
                "formality_level": "높음 — 존댓말과 격식체 어휘 사용이 두드러짐",
                "politeness_level": "높음 — 완곡한 표현 사용과 공손한 요청 방식",
                "directness_score": "중간 — 요청은 분명하지만 형태는 간접적",
                "warmth_score": "중간 — 딱딱하지 않지만 감정 표현은 적음",
                "speech_act_distribution": "요청, 정보 제공, 확인 요청이 중심이며 감사 표현이 보조적으로 나타남",
                "request_style": "간접적 요청 방식으로, ‘~부탁드립니다’ 형태가 많음",
                "notes": "정중하고 절제된 문체를 유지하면서도 필요한 요청 사항을 명확하게 전달하는 실무형 소통 스타일입니다.",
            },
            representative_sentences=[
                "안녕하세요, 회의 일정 조율 부탁드립니다.",
                "검토 후 회신 부탁드립니다.",
                "관련 자료를 확인하신 후 공유 요청드립니다.",
            ],
        )

        result = _fetch_analysis_for_single(self.user, self.contact)

        self.assertIsNotNone(result)
        self.assertIsInstance(result, dict)

        # JSON 전체 구조 검증
        self.assertIn("summary", result["lexical_style"])
        self.assertEqual(
            result["lexical_style"]["summary"],
            "정중하고 격식 있는 표현을 주로 사용하며, 연결어와 완곡한 어휘를 적절히 사용해 안정적인 어조를 유지하는 편입니다.",
        )

        self.assertIn("ender_distribution", result["grammar_patterns"])
        self.assertEqual(result["grammar_patterns"]["sentence_length"], "짧고 명료한 문장을 선호하며, 한 문장 내 복잡한 종속절은 적은 편입니다.")

        self.assertIn("overall", result["emotional_tone"])
        self.assertEqual(result["emotional_tone"]["overall"], "neutral_formal")
        self.assertEqual(len(result["representative_sentences"]), 3)

    def test_fetch_analysis_not_exists_no_group(self):
        """ContactAnalysisResult 없고, group도 없을 때 None 반환"""
        # group이 없는 contact 생성
        contact_no_group = Contact.objects.create(
            user=self.user,
            name="Bob",
            email="bob@example.com",
        )

        result = _fetch_analysis_for_single(self.user, contact_no_group)
        self.assertIsNone(result)

    def test_fetch_analysis_not_exists_with_group(self):
        """ContactAnalysisResult 없지만 group이 있을 때 그룹 분석 반환"""
        GroupAnalysisResult.objects.create(
            user=self.user,
            group=self.group,
            lexical_style={
                "summary": "정중하고 격식 있는 표현을 주로 사용합니다.",
            },
            grammar_patterns={
                "summary": "격식체 중심 문장 구조입니다.",
            },
            emotional_tone={
                "summary": "중립적이며 전문적인 톤입니다.",
                "overall": "neutral_formal",
            },
            representative_sentences=["안녕하세요.", "확인 부탁드립니다."],
        )

        result = _fetch_analysis_for_single(self.user, self.contact)

        self.assertIsNotNone(result)

        # JSON 구조 검증
        self.assertEqual(result["lexical_style"]["summary"], "정중하고 격식 있는 표현을 주로 사용합니다.")
        self.assertEqual(result["emotional_tone"]["overall"], "neutral_formal")

    def test_fetch_analysis_contact_priority_over_group(self):
        """Contact 분석이 있으면 Group 분석보다 우선"""

        GroupAnalysisResult.objects.create(
            user=self.user,
            group=self.group,
            lexical_style={"summary": "그룹 스타일"},
            grammar_patterns={"summary": "그룹 패턴"},
            emotional_tone={"summary": "그룹 톤"},
            representative_sentences=["그룹 문장"],
        )

        ContactAnalysisResult.objects.create(
            user=self.user,
            contact=self.contact,
            lexical_style={"summary": "개인 스타일"},
            grammar_patterns={"summary": "개인 패턴"},
            emotional_tone={"summary": "개인 톤"},
            representative_sentences=["개인 문장"],
        )

        result = _fetch_analysis_for_single(self.user, self.contact)

        self.assertEqual(result["lexical_style"]["summary"], "개인 스타일")
        self.assertEqual(result["grammar_patterns"]["summary"], "개인 패턴")
        self.assertEqual(result["emotional_tone"]["summary"], "개인 톤")

    def test_fetch_analysis_different_user(self):
        """다른 사용자의 분석 결과는 반환되지 않음"""
        other_user = User.objects.create(email="other@example.com", name="Other User")
        other_group = Group.objects.create(  # noqa: F841
            user=other_user,
            name="Other Group",
        )

        ContactAnalysisResult.objects.create(
            user=other_user,
            contact=self.contact,
            lexical_style={
                "summary": "정중하고 격식 있는 표현을 주로 사용하며, 연결어와 완곡한 어휘를 적절히 사용해 안정적인 어조를 유지하는 편입니다.",
                "top_connectives": "‘그리고’, ‘또한’, ‘따라서’와 같은 기본 연결어를 활용해 문장을 자연스럽게 이어갑니다.",
                "frequent_phrases": "‘부탁드립니다’, ‘검토 후 회신 요청드립니다’ 등이 반복적으로 등장합니다.",
                "slang_or_chat_markers": "구어체나 이모지는 거의 사용되지 않습니다.",
                "politeness_lexemes": "‘부탁드립니다’, ‘감사합니다’ 등의 공손 표현을 자주 사용합니다.",
            },
            grammar_patterns={
                "summary": "존댓말 중심으로 간결하게 구성됩니다.",
                "ender_distribution": "‘-습니다’, ‘-드립니다’ 중심.",
                "sentence_length": "짧고 명료한 문장이 많음.",
                "sentence_type_ratio": "서술문 위주, 간접 명령형 일부 포함.",
                "structure_pattern": "인사 → 요청 → 확인 요청 → 마무리 구조.",
                "paragraph_stats": "짧은 단락 위주.",
            },
            emotional_tone={
                "summary": "중립적이고 전문적인 톤.",
                "overall": "neutral_formal",
                "formality_level": "높음",
                "politeness_level": "높음",
                "directness_score": "중간",
                "warmth_score": "중간",
                "speech_act_distribution": "요청·정보 제공 중심",
                "request_style": "간접적 요청",
                "notes": "정중하고 실무적인 문체.",
            },
            representative_sentences=[
                "안녕하세요, 회의 일정 조율 부탁드립니다.",
                "검토 후 회신 부탁드립니다.",
                "관련 자료를 확인하신 후 공유 요청드립니다.",
            ],
        )

        result = _fetch_analysis_for_single(self.user, self.contact)
        self.assertIsNone(result)

    def test_fetch_analysis_with_empty_representative_sentences(self):
        """representative_sentences가 빈 리스트일 때"""
        ContactAnalysisResult.objects.create(
            user=self.user,
            contact=self.contact,
            lexical_style={
                "summary": "정중하고 격식 있는 표현 중심.",
                "top_connectives": "‘그리고’, ‘또한’ 등 기본 연결어 사용.",
                "frequent_phrases": "‘부탁드립니다’ 등이 반복됨.",
                "slang_or_chat_markers": "구어체 없음.",
                "politeness_lexemes": "공손 표현 다수 포함.",
            },
            grammar_patterns={
                "summary": "존댓말 중심의 간결한 문장.",
                "ender_distribution": "‘-습니다’ 중심.",
                "sentence_length": "짧고 명료함.",
                "sentence_type_ratio": "서술문 위주.",
                "structure_pattern": "인사 → 요청 → 마무리 구조.",
                "paragraph_stats": "짧은 단락 구성.",
            },
            emotional_tone={
                "summary": "중립적이고 전문적.",
                "overall": "neutral_formal",
                "formality_level": "높음",
                "politeness_level": "높음",
                "directness_score": "중간",
                "warmth_score": "중간",
                "speech_act_distribution": "요청 중심",
                "request_style": "간접적 요청",
                "notes": "정중하고 절제된 문체.",
            },
            representative_sentences=[],
        )

        result = _fetch_analysis_for_single(self.user, self.contact)

        self.assertIsNotNone(result)
        self.assertEqual(result["representative_sentences"], [])

    def test_fetch_analysis_multiple_contacts_same_group(self):
        """같은 그룹에 속한 여러 연락처가 있을 때,
        첫 번째 연락처는 개인 분석을, 두 번째 연락처는 그룹 분석을 반환해야 한다."""

        # 같은 그룹에 두 번째 연락처 생성
        contact2 = Contact.objects.create(
            user=self.user,
            group=self.group,
            name="Charlie",
            email="charlie@example.com",
        )

        # --- 개인 분석 (self.contact) 저장 ---
        personal_lexical = {
            "summary": "개인 분석 - 정중하고 격식 있는 표현 중심",
            "top_connectives": "개인: 그리고, 또한",
            "frequent_phrases": "개인: 부탁드립니다",
            "slang_or_chat_markers": "없음",
            "politeness_lexemes": "감사합니다, 부탁드립니다",
        }

        ContactAnalysisResult.objects.create(
            user=self.user,
            contact=self.contact,
            lexical_style=personal_lexical,
            grammar_patterns={
                "summary": "개인 분석 문장 구조",
                "ender_distribution": "습니다 중심",
                "sentence_length": "짧음",
                "sentence_type_ratio": "서술문 위주",
                "structure_pattern": "인사 → 요청 → 마무리",
                "paragraph_stats": "단락 짧음",
            },
            emotional_tone={
                "summary": "개인 분석 - 중립·정중",
                "overall": "neutral_formal",
                "formality_level": "높음",
                "politeness_level": "높음",
                "directness_score": "중간",
                "warmth_score": "중간",
                "speech_act_distribution": "요청, 확인",
                "request_style": "간접적",
                "notes": "개인 요약",
            },
            representative_sentences=["개인: 검토 후 회신 부탁드립니다."],
        )

        # --- 그룹 분석 저장 ---
        group_lexical = {
            "summary": "그룹 분석 - 공통적으로 정중한 표현 사용",
            "top_connectives": "그룹: 그리고, 따라서",
            "frequent_phrases": "그룹: 요청드립니다",
            "slang_or_chat_markers": "없음",
            "politeness_lexemes": "감사합니다 등",
        }

        GroupAnalysisResult.objects.create(
            user=self.user,
            group=self.group,
            lexical_style=group_lexical,
            grammar_patterns={
                "summary": "그룹 분석 문장 구조",
                "ender_distribution": "드립니다 중심",
                "sentence_length": "중간",
                "sentence_type_ratio": "서술문 위주",
                "structure_pattern": "인사 → 설명 → 요청",
                "paragraph_stats": "일반적인 길이",
            },
            emotional_tone={
                "summary": "그룹 분석 - 중립·전문적",
                "overall": "neutral_formal",
                "formality_level": "높음",
                "politeness_level": "높음",
                "directness_score": "중간",
                "warmth_score": "낮음",
                "speech_act_distribution": "요청, 정보 제공",
                "request_style": "간접적",
                "notes": "그룹 요약",
            },
            representative_sentences=["그룹: 검토 후 회신 요청드립니다."],
        )

        # --- 첫 번째 연락처는 개인 분석 반환 ---
        result1 = _fetch_analysis_for_single(self.user, self.contact)
        self.assertIsNotNone(result1)
        self.assertEqual(result1["lexical_style"]["summary"], personal_lexical["summary"])

        # --- 두 번째 연락처는 그룹 분석 반환 ---
        result2 = _fetch_analysis_for_single(self.user, contact2)
        self.assertIsNotNone(result2)
        self.assertEqual(result2["lexical_style"]["summary"], group_lexical["summary"])


class FetchAnalysisForGroupTest(TestCase):
    """_fetch_analysis_for_group 함수 테스트"""

    def setUp(self):
        self.user = User.objects.create(email="u@example.com", name="U1")
        self.group = Group.objects.create(
            user=self.user,
            name="Development Team",
            description="Dev team members",
        )
        # contact 필수 생성
        self.contact = Contact.objects.create(
            user=self.user,
            group=self.group,
            name="Alice",
            email="alice@example.com",
        )

    def test_fetch_group_analysis_exists(self):
        """GroupAnalysisResult가 존재할 때 정상 반환"""

        group_result = GroupAnalysisResult.objects.create(
            user=self.user,
            group=self.group,
            lexical_style={
                "summary": "정중하고 격식 있는 표현을 주로 사용하며, 연결어와 완곡한 어휘를 적절히 사용해 안정적인 어조를 유지하는 편입니다.",
                "top_connectives": "‘그리고’, ‘또한’, ‘따라서’와 같은 기본 연결어를 활용해 문장을 자연스럽게 이어갑니다.",
                "frequent_phrases": "‘부탁드립니다’, ‘검토 후 회신 요청드립니다’ 등 업무 요청 시 자주 사용되는 정중한 관용구가 반복적으로 등장합니다.",  # noqa: E501
                "slang_or_chat_markers": "이모지나 구어체, 인터넷식 표현은 거의 사용하지 않아 전체적으로 격식 있는 톤을 유지합니다.",
                "politeness_lexemes": "‘부탁드립니다’, ‘감사합니다’, ‘죄송하지만’과 같은 공손 표현을 활용해 완곡하고 예의를 갖춘 어조를 형성합니다.",
            },
            grammar_patterns={
                "summary": "문장은 존댓말 종결형이 중심이며 비교적 간결하고 목적 중심적으로 구성됩니다. 요청과 설명이 명확히 구분되어 있어 읽기 편합니다.",  # noqa: E501
                "ender_distribution": "‘-습니다’, ‘-드립니다’와 같은 격식체 종결형이 주로 사용됩니다.",
                "sentence_length": "짧고 명료한 문장을 선호하며, 한 문장 내 복잡한 종속절은 적은 편입니다.",
                "sentence_type_ratio": "서술문이 대부분이며, 요청성 문장(간접 명령형)이 일정 비중을 차지합니다.",
                "structure_pattern": "인사 → 요청 또는 정보 전달 → 필요 시 추가 설명 → 마무리 인사 순으로 안정적인 패턴을 유지합니다.",
                "paragraph_stats": "짧은 단문 중심의 단락으로 구성되며, 단락 간 흐름이 명확해 가독성이 높습니다.",
            },
            emotional_tone={
                "summary": "전체적으로 중립적이고 전문적인 톤이며, 공손함과 명료함을 균형 있게 유지합니다.",
                "overall": "neutral_formal",
                "formality_level": "높음 — 존댓말과 격식체 어휘 사용이 두드러짐",
                "politeness_level": "높음 — 완곡한 표현 사용과 공손한 요청 방식",
                "directness_score": "중간 — 요청은 분명하지만 형태는 간접적",
                "warmth_score": "중간 — 딱딱하지 않지만 감정 표현은 적음",
                "speech_act_distribution": "요청, 정보 제공, 확인 요청이 중심이며 감사 표현이 보조적으로 나타남",
                "request_style": "간접적 요청 방식으로, ‘~부탁드립니다’ 형태가 많음",
                "notes": "정중하고 절제된 문체를 유지하면서도 필요한 요청 사항을 명확하게 전달하는 실무형 소통 스타일입니다.",
            },
            representative_sentences=[
                "안녕하세요, 회의 일정 조율 부탁드립니다.",
                "검토 후 회신 부탁드립니다.",
                "관련 자료를 확인하신 후 공유 요청드립니다.",
            ],
        )

        result = _fetch_analysis_for_group(self.user, self.group)

        self.assertIsNotNone(result)
        self.assertIsInstance(result, dict)

        self.assertEqual(result["lexical_style"], group_result.lexical_style)
        self.assertEqual(result["grammar_patterns"], group_result.grammar_patterns)
        self.assertEqual(result["emotional_tone"], group_result.emotional_tone)
        self.assertEqual(result["representative_sentences"], group_result.representative_sentences)

    def test_fetch_group_analysis_not_exists(self):
        """GroupAnalysisResult가 존재하지 않을 때 None 반환"""
        result = _fetch_analysis_for_group(self.user, self.group)
        self.assertIsNone(result)

    def test_fetch_group_analysis_different_user(self):
        """다른 사용자의 그룹 분석 결과는 반환되지 않음"""

        other_user = User.objects.create(email="other@example.com", name="Other User")

        # 다른 사용자의 그룹 분석 생성
        GroupAnalysisResult.objects.create(
            user=other_user,  # ← 다른 유저
            group=self.group,  # group은 상관 없음
            lexical_style={
                "summary": "정중하고 격식 있는 표현을 주로 사용하며, 연결어와 완곡한 어휘를 적절히 사용해 안정적인 어조를 유지하는 편입니다.",
                "top_connectives": "‘그리고’, ‘또한’, ‘따라서’와 같은 기본 연결어를 활용해 문장을 자연스럽게 이어갑니다.",
                "frequent_phrases": "‘부탁드립니다’, ‘검토 후 회신 요청드립니다’ 등이 반복적으로 등장합니다.",
                "slang_or_chat_markers": "구어체나 이모지는 거의 사용되지 않습니다.",
                "politeness_lexemes": "‘부탁드립니다’, ‘감사합니다’ 등의 공손 표현을 자주 사용합니다.",
            },
            grammar_patterns={
                "summary": "존댓말 중심으로 간결하게 구성됩니다.",
                "ender_distribution": "‘-습니다’, ‘-드립니다’ 중심.",
                "sentence_length": "짧고 명료한 문장이 많음.",
                "sentence_type_ratio": "서술문 위주, 간접 명령형 일부 포함.",
                "structure_pattern": "인사 → 요청 → 확인 요청 → 마무리 구조.",
                "paragraph_stats": "짧은 단락 위주.",
            },
            emotional_tone={
                "summary": "중립적이고 전문적인 톤.",
                "overall": "neutral_formal",
                "formality_level": "높음",
                "politeness_level": "높음",
                "directness_score": "중간",
                "warmth_score": "중간",
                "speech_act_distribution": "요청·정보 제공 중심",
                "request_style": "간접적 요청",
                "notes": "정중하고 실무적인 문체.",
            },
            representative_sentences=[
                "안녕하세요, 회의 일정 조율 부탁드립니다.",
                "검토 후 회신 부탁드립니다.",
                "관련 자료를 확인하신 후 공유 요청드립니다.",
            ],
        )

        result = _fetch_analysis_for_group(self.user, self.group)

        # 현재 로그인한 self.user 의 분석 결과는 없으므로 None
        self.assertIsNone(result)

    def test_fetch_group_analysis_all_fields_present(self):
        """모든 필드가 딕셔너리에 포함되는지 검증"""
        GroupAnalysisResult.objects.create(
            user=self.user,
            group=self.group,
            lexical_style={
                "summary": "정중하고 격식 있는 표현을 주로 사용하며, 연결어와 완곡한 어휘를 적절히 사용해 안정적인 어조를 유지하는 편입니다.",
                "top_connectives": "‘그리고’, ‘또한’, ‘따라서’와 같은 기본 연결어를 활용해 문장을 자연스럽게 이어갑니다.",
                "frequent_phrases": "‘부탁드립니다’, ‘검토 후 회신 요청드립니다’ 등 업무 요청 시 자주 사용되는 정중한 관용구가 반복적으로 등장합니다.",  # noqa: E501
                "slang_or_chat_markers": "이모지나 구어체, 인터넷식 표현은 거의 사용하지 않아 전체적으로 격식 있는 톤을 유지합니다.",
                "politeness_lexemes": "‘부탁드립니다’, ‘감사합니다’, ‘죄송하지만’과 같은 공손 표현을 활용해 완곡하고 예의를 갖춘 어조를 형성합니다.",
            },
            grammar_patterns={
                "summary": "문장은 존댓말 종결형이 중심이며 비교적 간결하고 목적 중심적으로 구성됩니다. 요청과 설명이 명확히 구분되어 있어 읽기 편합니다.",  # noqa: E501
                "ender_distribution": "‘-습니다’, ‘-드립니다’와 같은 격식체 종결형이 주로 사용됩니다.",
                "sentence_length": "짧고 명료한 문장을 선호하며, 한 문장 내 복잡한 종속절은 적은 편입니다.",
                "sentence_type_ratio": "서술문이 대부분이며, 요청성 문장(간접 명령형)이 일정 비중을 차지합니다.",
                "structure_pattern": "인사 → 요청 또는 정보 전달 → 필요 시 추가 설명 → 마무리 인사 순으로 안정적인 패턴을 유지합니다.",
                "paragraph_stats": "짧은 단문 중심의 단락으로 구성되며, 단락 간 흐름이 명확해 가독성이 높습니다.",
            },
            emotional_tone={
                "summary": "전체적으로 중립적이고 전문적인 톤이며, 공손함과 명료함을 균형 있게 유지합니다.",
                "overall": "neutral_formal",
                "formality_level": "높음 — 존댓말과 격식체 어휘 사용이 두드러짐",
                "politeness_level": "높음 — 완곡한 표현 사용과 공손한 요청 방식",
                "directness_score": "중간 — 요청은 분명하지만 형태는 간접적",
                "warmth_score": "중간 — 딱딱하지 않지만 감정 표현은 적음",
                "speech_act_distribution": "요청, 정보 제공, 확인 요청이 중심이며 감사 표현이 보조적으로 나타남",
                "request_style": "간접적 요청 방식으로, ‘~부탁드립니다’ 형태가 많음",
                "notes": "정중하고 절제된 문체를 유지하면서도 필요한 요청 사항을 명확하게 전달하는 실무형 소통 스타일입니다.",
            },
            representative_sentences=[
                "안녕하세요, 회의 일정 조율 부탁드립니다.",
                "검토 후 회신 부탁드립니다.",
                "관련 자료를 확인하신 후 공유 요청드립니다.",
            ],
        )

        result = _fetch_analysis_for_group(self.user, self.group)

        # 모든 예상 키가 존재하는지 검증
        expected_keys = {"lexical_style", "grammar_patterns", "emotional_tone", "representative_sentences"}
        self.assertEqual(set(result.keys()), expected_keys)

    def test_fetch_group_analysis_multiple_groups(self):
        """여러 그룹이 있을 때 올바른 그룹 분석 반환"""
        # 두 번째 그룹 생성
        group2 = Group.objects.create(
            user=self.user,
            name="Marketing Team",
            description="Marketing members",
        )

        # 각 그룹에 분석 결과 생성
        GroupAnalysisResult.objects.create(
            user=self.user,
            group=self.group,
            lexical_style={
                "summary": "개발팀 스타일",
                "top_connectives": "‘그리고’, ‘또한’, ‘따라서’와 같은 기본 연결어를 활용해 문장을 자연스럽게 이어갑니다.",
                "frequent_phrases": "‘부탁드립니다’, ‘검토 후 회신 요청드립니다’ 등 업무 요청 시 자주 사용되는 정중한 관용구가 반복적으로 등장합니다.",  # noqa: E501
                "slang_or_chat_markers": "이모지나 구어체, 인터넷식 표현은 거의 사용하지 않아 전체적으로 격식 있는 톤을 유지합니다.",
                "politeness_lexemes": "‘부탁드립니다’, ‘감사합니다’, ‘죄송하지만’과 같은 공손 표현을 활용해 완곡하고 예의를 갖춘 어조를 형성합니다.",
            },
            grammar_patterns={
                "summary": "문장은 존댓말 종결형이 중심이며 비교적 간결하고 목적 중심적으로 구성됩니다. 요청과 설명이 명확히 구분되어 있어 읽기 편합니다.",  # noqa: E501
                "ender_distribution": "‘-습니다’, ‘-드립니다’와 같은 격식체 종결형이 주로 사용됩니다.",
                "sentence_length": "짧고 명료한 문장을 선호하며, 한 문장 내 복잡한 종속절은 적은 편입니다.",
                "sentence_type_ratio": "서술문이 대부분이며, 요청성 문장(간접 명령형)이 일정 비중을 차지합니다.",
                "structure_pattern": "인사 → 요청 또는 정보 전달 → 필요 시 추가 설명 → 마무리 인사 순으로 안정적인 패턴을 유지합니다.",
                "paragraph_stats": "짧은 단문 중심의 단락으로 구성되며, 단락 간 흐름이 명확해 가독성이 높습니다.",
            },
            emotional_tone={
                "summary": "전체적으로 중립적이고 전문적인 톤이며, 공손함과 명료함을 균형 있게 유지합니다.",
                "overall": "neutral_formal",
                "formality_level": "높음 — 존댓말과 격식체 어휘 사용이 두드러짐",
                "politeness_level": "높음 — 완곡한 표현 사용과 공손한 요청 방식",
                "directness_score": "중간 — 요청은 분명하지만 형태는 간접적",
                "warmth_score": "중간 — 딱딱하지 않지만 감정 표현은 적음",
                "speech_act_distribution": "요청, 정보 제공, 확인 요청이 중심이며 감사 표현이 보조적으로 나타남",
                "request_style": "간접적 요청 방식으로, ‘~부탁드립니다’ 형태가 많음",
                "notes": "정중하고 절제된 문체를 유지하면서도 필요한 요청 사항을 명확하게 전달하는 실무형 소통 스타일입니다.",
            },
            representative_sentences=[
                "안녕하세요, 회의 일정 조율 부탁드립니다.",
                "검토 후 회신 부탁드립니다.",
                "관련 자료를 확인하신 후 공유 요청드립니다.",
            ],
        )

        GroupAnalysisResult.objects.create(
            user=self.user,
            group=group2,
            lexical_style={
                "summary": "마케팅팀 스타일",
                "top_connectives": "‘그리고’, ‘또한’, ‘따라서’와 같은 기본 연결어를 활용해 문장을 자연스럽게 이어갑니다.",
                "frequent_phrases": "‘부탁드립니다’, ‘검토 후 회신 요청드립니다’ 등 업무 요청 시 자주 사용되는 정중한 관용구가 반복적으로 등장합니다.",  # noqa: E501
                "slang_or_chat_markers": "이모지나 구어체, 인터넷식 표현은 거의 사용하지 않아 전체적으로 격식 있는 톤을 유지합니다.",
                "politeness_lexemes": "‘부탁드립니다’, ‘감사합니다’, ‘죄송하지만’과 같은 공손 표현을 활용해 완곡하고 예의를 갖춘 어조를 형성합니다.",
            },
            grammar_patterns={
                "summary": "문장은 존댓말 종결형이 중심이며 비교적 간결하고 목적 중심적으로 구성됩니다. 요청과 설명이 명확히 구분되어 있어 읽기 편합니다.",  # noqa: E501
                "ender_distribution": "‘-습니다’, ‘-드립니다’와 같은 격식체 종결형이 주로 사용됩니다.",
                "sentence_length": "짧고 명료한 문장을 선호하며, 한 문장 내 복잡한 종속절은 적은 편입니다.",
                "sentence_type_ratio": "서술문이 대부분이며, 요청성 문장(간접 명령형)이 일정 비중을 차지합니다.",
                "structure_pattern": "인사 → 요청 또는 정보 전달 → 필요 시 추가 설명 → 마무리 인사 순으로 안정적인 패턴을 유지합니다.",
                "paragraph_stats": "짧은 단문 중심의 단락으로 구성되며, 단락 간 흐름이 명확해 가독성이 높습니다.",
            },
            emotional_tone={
                "summary": "전체적으로 중립적이고 전문적인 톤이며, 공손함과 명료함을 균형 있게 유지합니다.",
                "overall": "neutral_formal",
                "formality_level": "높음 — 존댓말과 격식체 어휘 사용이 두드러짐",
                "politeness_level": "높음 — 완곡한 표현 사용과 공손한 요청 방식",
                "directness_score": "중간 — 요청은 분명하지만 형태는 간접적",
                "warmth_score": "중간 — 딱딱하지 않지만 감정 표현은 적음",
                "speech_act_distribution": "요청, 정보 제공, 확인 요청이 중심이며 감사 표현이 보조적으로 나타남",
                "request_style": "간접적 요청 방식으로, ‘~부탁드립니다’ 형태가 많음",
                "notes": "정중하고 절제된 문체를 유지하면서도 필요한 요청 사항을 명확하게 전달하는 실무형 소통 스타일입니다.",
            },
            representative_sentences=[
                "안녕하세요, 회의 일정 조율 부탁드립니다.",
                "검토 후 회신 부탁드립니다.",
                "관련 자료를 확인하신 후 공유 요청드립니다.",
            ],
        )

        # 각 그룹의 분석 결과 확인
        result1 = _fetch_analysis_for_group(self.user, self.group)
        result2 = _fetch_analysis_for_group(self.user, group2)

        self.assertEqual(result1["lexical_style"]["summary"], "개발팀 스타일")
        self.assertEqual(result2["lexical_style"]["summary"], "마케팅팀 스타일")


class IntegrationTest(TestCase):
    """_fetch_analysis_for_single과 _fetch_analysis_for_group의 통합 테스트"""

    def setUp(self):
        self.user = User.objects.create(email="u@example.com", name="U1")
        self.group = Group.objects.create(
            user=self.user,
            name="Test Group",
            description="Test group description",
        )
        self.contact = Contact.objects.create(
            user=self.user,
            group=self.group,
            name="Alice",
            email="alice@example.com",
        )

    def test_fallback_chain_contact_to_group(self):
        """Contact 분석 없을 때 Group 분석으로 fallback"""
        # Group 분석만 생성
        GroupAnalysisResult.objects.create(
            user=self.user,
            group=self.group,
            lexical_style={
                "summary": "그룹 스타일",
                "top_connectives": "‘그리고’, ‘또한’, ‘따라서’와 같은 기본 연결어를 활용해 문장을 자연스럽게 이어갑니다.",
                "frequent_phrases": "‘부탁드립니다’, ‘검토 후 회신 요청드립니다’ 등 업무 요청 시 자주 사용되는 정중한 관용구가 반복적으로 등장합니다.",  # noqa: E501
                "slang_or_chat_markers": "이모지나 구어체, 인터넷식 표현은 거의 사용하지 않아 전체적으로 격식 있는 톤을 유지합니다.",
                "politeness_lexemes": "‘부탁드립니다’, ‘감사합니다’, ‘죄송하지만’과 같은 공손 표현을 활용해 완곡하고 예의를 갖춘 어조를 형성합니다.",
            },
            grammar_patterns={
                "summary": "문장은 존댓말 종결형이 중심이며 비교적 간결하고 목적 중심적으로 구성됩니다. 요청과 설명이 명확히 구분되어 있어 읽기 편합니다.",  # noqa: E501
                "ender_distribution": "‘-습니다’, ‘-드립니다’와 같은 격식체 종결형이 주로 사용됩니다.",
                "sentence_length": "짧고 명료한 문장을 선호하며, 한 문장 내 복잡한 종속절은 적은 편입니다.",
                "sentence_type_ratio": "서술문이 대부분이며, 요청성 문장(간접 명령형)이 일정 비중을 차지합니다.",
                "structure_pattern": "인사 → 요청 또는 정보 전달 → 필요 시 추가 설명 → 마무리 인사 순으로 안정적인 패턴을 유지합니다.",
                "paragraph_stats": "짧은 단문 중심의 단락으로 구성되며, 단락 간 흐름이 명확해 가독성이 높습니다.",
            },
            emotional_tone={
                "summary": "전체적으로 중립적이고 전문적인 톤이며, 공손함과 명료함을 균형 있게 유지합니다.",
                "overall": "neutral_formal",
                "formality_level": "높음 — 존댓말과 격식체 어휘 사용이 두드러짐",
                "politeness_level": "높음 — 완곡한 표현 사용과 공손한 요청 방식",
                "directness_score": "중간 — 요청은 분명하지만 형태는 간접적",
                "warmth_score": "중간 — 딱딱하지 않지만 감정 표현은 적음",
                "speech_act_distribution": "요청, 정보 제공, 확인 요청이 중심이며 감사 표현이 보조적으로 나타남",
                "request_style": "간접적 요청 방식으로, ‘~부탁드립니다’ 형태가 많음",
                "notes": "정중하고 절제된 문체를 유지하면서도 필요한 요청 사항을 명확하게 전달하는 실무형 소통 스타일입니다.",
            },
            representative_sentences=[
                "안녕하세요, 회의 일정 조율 부탁드립니다.",
                "검토 후 회신 부탁드립니다.",
                "관련 자료를 확인하신 후 공유 요청드립니다.",
            ],
        )

        result = _fetch_analysis_for_single(self.user, self.contact)

        self.assertIsNotNone(result)
        self.assertEqual(result["lexical_style"]["summary"], "그룹 스타일")

    def test_contact_without_group_id_attribute(self):
        """group_id 속성이 없는 contact 객체 처리"""
        # group이 없는 contact 생성
        contact_no_group = Contact.objects.create(
            user=self.user,
            name="Bob",
            email="bob@example.com",
        )

        result = _fetch_analysis_for_single(self.user, contact_no_group)

        self.assertIsNone(result)

    def test_consistency_of_returned_dict_structure(self):
        """Contact와 Group 분석 결과의 딕셔너리 구조 일관성 검증"""
        # Contact 분석 생성
        ContactAnalysisResult.objects.create(
            user=self.user,
            contact=self.contact,
            lexical_style={"summary": "contact"},
            grammar_patterns={"summary": "contact"},
            emotional_tone={"summary": "contact"},
            representative_sentences=["contact"],
        )

        # Group 분석 생성
        GroupAnalysisResult.objects.create(
            user=self.user,
            group=self.group,
            lexical_style={"summary": "group"},
            grammar_patterns={"summary": "group"},
            emotional_tone={"summary": "group"},
            representative_sentences=["group"],
        )

        contact_result = _fetch_analysis_for_single(self.user, self.contact)

        # 새로운 contact (group만 해당)
        other_contact = Contact.objects.create(
            user=self.user,
            group=self.group,
            name="Charlie",
            email="charlie@example.com",
        )
        group_result = _fetch_analysis_for_single(self.user, other_contact)

        # 두 결과의 키 구조가 동일한지 검증
        self.assertEqual(set(contact_result.keys()), set(group_result.keys()))

    def test_complete_scenario_with_multiple_users_and_groups(self):
        """복잡한 시나리오: 여러 사용자, 그룹, 연락처"""
        # 두 번째 사용자 생성
        user2 = User.objects.create(email="user2@example.com", name="U2")

        # 두 번째 사용자의 그룹
        group2 = Group.objects.create(
            user=user2,
            name="User2 Group",
            description="Second user's group",
        )

        # 두 번째 사용자의 연락처
        contact2 = Contact.objects.create(
            user=user2,
            group=group2,
            name="David",
            email="david@example.com",
        )

        # 각 사용자의 분석 결과 생성
        ContactAnalysisResult.objects.create(
            user=self.user,
            contact=self.contact,
            lexical_style={"summary": "User1-Contact 스타일"},
            grammar_patterns={"summary": "패턴1"},
            emotional_tone={"summary": "톤1"},
            representative_sentences=["예문1"],
        )

        ContactAnalysisResult.objects.create(
            user=user2,
            contact=contact2,
            lexical_style={"summary": "User2-Contact 스타일"},
            grammar_patterns={"summary": "패턴2"},
            emotional_tone={"summary": "톤2"},
            representative_sentences=["예문2"],
        )

        # 각 사용자는 자신의 결과만 조회 가능
        result1 = _fetch_analysis_for_single(self.user, self.contact)
        result2 = _fetch_analysis_for_single(user2, contact2)

        self.assertEqual(result1["lexical_style"]["summary"], "User1-Contact 스타일")
        self.assertEqual(result2["lexical_style"]["summary"], "User2-Contact 스타일")

        # 교차 조회는 None 반환
        cross_result = _fetch_analysis_for_single(self.user, contact2)
        self.assertIsNone(cross_result)


# =========================
# collect_prompt_context
# =========================
class CollectPromptContextTest(TestCase):
    """
    collect_prompt_context() 의 경우 Contact/Group/PromptOption/ContactContext 관계를
    다양한 경우(1명, 다수 동일 그룹, 다수 다른 그룹, 아예 등록 안 된 수신자)로 나누어 검증한다.

    fewshots 수집은 _fetch_fewshot_* 로직에 의존하므로,
    여기서는 patch 로 주입해서 "호출됐다 / 반환됐다"만 확인한다.
    """

    def setUp(self):
        self.user = User.objects.create(email="owner@example.com", name="Owner")

        # 시스템/사용자 PromptOption 생성
        # PromptOption 필드는 (id, key, name, prompt, created_by, ...)
        self.opt_sys = PromptOption.objects.create(
            key="sys-polite",
            name="System Polite",
            prompt="Please respond politely.",
            created_by=None,
        )
        self.opt_usr = PromptOption.objects.create(
            key="usr-friendly",
            name="User Friendly",
            prompt="Keep it friendly.",
            created_by=self.user,
        )

        # 그룹 1 (g1) - user 소유
        self.g1 = Group.objects.create(
            user=self.user,
            name="Team Alpha",
            description="Internal team comms",
        )
        self.g1.options.add(self.opt_sys, self.opt_usr)

        # 그룹 2 (g2) - user 소유
        self.g2 = Group.objects.create(
            user=self.user,
            name="Team Beta",
            description="External partners",
        )
        self.g2.options.add(self.opt_sys)  # 교집합은 opt_sys 만

        # 연락처 1 - g1 소속
        self.c1 = Contact.objects.create(
            user=self.user,
            group=self.g1,
            name="Alice A.",
            email="alice@alpha.com",
        )
        self.ctx1 = ContactContext.objects.create(
            contact=self.c1,
            sender_role="Team lead",
            recipient_role="Client manager",
            relationship_details="Long-term partnership",
            personal_prompt="Be polite but concise.",
            language_preference="en",
        )

        # 연락처 2 - g1 소속
        self.c2 = Contact.objects.create(
            user=self.user,
            group=self.g1,
            name="Bob B.",
            email="bob@alpha.com",
        )
        ContactContext.objects.create(
            contact=self.c2,
            sender_role="Engineer",
            recipient_role="Manager",
            relationship_details="Daily sync",
            personal_prompt="Keep it technical.",
            language_preference="en",
        )

        # 연락처 3 - g2 소속
        self.c3 = Contact.objects.create(
            user=self.user,
            group=self.g2,
            name="Charlie C.",
            email="charlie@beta.com",
        )
        ContactContext.objects.create(
            contact=self.c3,
            sender_role="Support",
            recipient_role="Vendor",
            relationship_details="Escalation channel",
            personal_prompt="Use escalation tone.",
            language_preference="ko",
        )

    @patch("apps.ai.services.utils._fetch_analysis_for_single", return_value=["FS1", "FS2"])
    def test_single_recipient_full_context(self, mock_fetch_single):
        """
        - 수신자 1명, 등록된 Contact 1명 -> 개인 컨텍스트/그룹 정보/개인 프롬프트/언어까지 다 들어간다.
        - fewshots 는 _fetch_fewshot_bodies_for_single 호출 결과를 넣어야 한다.
        """
        out = collect_prompt_context(
            self.user,
            to_emails=[self.c1.email],
            include_analysis=True,
        )

        # recipients: 실제 이름을 사용
        self.assertEqual(out["recipients"], ["Alice A."])

        # group 정보
        self.assertEqual(out["group_name"], "Team Alpha")
        self.assertEqual(out["group_description"], "Internal team comms")

        # group.options -> prompt_options
        self.assertIn("Please respond politely.", out["prompt_options"])
        self.assertIn("Keep it friendly.", out["prompt_options"])

        # 개인 context
        self.assertEqual(out["sender_role"], "Team lead")
        self.assertEqual(out["recipient_role"], "Client manager")
        self.assertEqual(out["personal_prompt"], "Be polite but concise.")
        self.assertEqual(out["language"], "en")

        # fewshots
        self.assertEqual(out["analysis"], ["FS1", "FS2"])
        mock_fetch_single.assert_called_once()

    @patch("apps.ai.services.utils._fetch_analysis_for_group", return_value=["GFS1"])
    def test_multiple_same_group(self, mock_fetch_group):
        """
        - 동일 그룹 소속 두 명을 동시에 보내는 경우
        - group_name/description/prompt_options 은 그 그룹 기준
        - sender_role 등 개인 context 는 빠짐 (base 그대로 None)
        """
        out = collect_prompt_context(
            self.user,
            to_emails=[self.c1.email, self.c2.email],
            include_analysis=True,
        )

        # recipients: 각각 이름
        self.assertEqual(out["recipients"], ["Alice A.", "Bob B."])

        # 공통 그룹(g1)
        self.assertEqual(out["group_name"], "Team Alpha")
        self.assertEqual(out["group_description"], "Internal team comms")

        # g1.options
        self.assertIn("Please respond politely.", out["prompt_options"])
        self.assertIn("Keep it friendly.", out["prompt_options"])

        # 단일 컨텍스트 정보는 없어야 함
        self.assertIsNone(out["sender_role"])
        self.assertIsNone(out["recipient_role"])
        self.assertIsNone(out["personal_prompt"])
        self.assertIsNone(out["language"])

        # fewshots 는 group 기반 fetch 호출
        self.assertEqual(out["analysis"], ["GFS1"])
        mock_fetch_group.assert_called_once()

    def test_multiple_diff_groups_intersection(self):
        """
        - 서로 다른 그룹(g1, g2) 소속 주소들을 동시에 보낸 경우
        - group_name 은 "Team Alpha, Team Beta" (둘 다 이름 join)
        - prompt_options 는 두 그룹 옵션 교집합 (여기서는 opt_sys만)
        - fewshots 는 include_fewshots=True 가 아니면 빈 []
        """
        out = collect_prompt_context(
            self.user,
            to_emails=[self.c1.email, self.c3.email],
            include_analysis=False,
        )

        # recipients: 이름 두 개
        self.assertEqual(out["recipients"], ["Alice A.", "Charlie C."])

        # group_name 은 join
        self.assertIn("Team Alpha", out["group_name"])
        self.assertIn("Team Beta", out["group_name"])

        # 교집합: g1.options={sys,usr}, g2.options={sys} -> intersection={sys}
        # sys의 prompt="Please respond politely."
        self.assertIn("Please respond politely.", out["prompt_options"])
        # user 전용 옵션("Keep it friendly.")은 교집합이 아님
        self.assertNotIn("Keep it friendly.", out["prompt_options"])

        # 단일 컨텍스트 아님 → 개인 정보 없음
        self.assertIsNone(out["sender_role"])
        self.assertIsNone(out["recipient_role"])
        self.assertIsNone(out["personal_prompt"])
        self.assertIsNone(out["language"])

        # include_analysis=False → analysis 는 None 유지
        self.assertEqual(out["analysis"], None)

    def test_unregistered_email_fallback_label(self):
        """
        - 등록 안 된 이메일이 들어오면 "Recipient {i}" 라벨 사용
        - 연락처/그룹 못 찾으면 base 구조만 반환
        """
        out = collect_prompt_context(
            self.user,
            to_emails=["nobody@example.com"],
            include_analysis=True,
        )

        # 이름 몰라서 Recipient 1
        self.assertEqual(out["recipients"], ["Recipient 1"])

        # 나머지는 base 그대로
        self.assertIsNone(out["group_name"])
        self.assertIsNone(out["group_description"])
        self.assertEqual(out["prompt_options"], [])
        self.assertIsNone(out["personal_prompt"])
        self.assertIsNone(out["sender_role"])
        self.assertIsNone(out["recipient_role"])
        self.assertIsNone(out["language"])
        # anlaysis 기본 None
        self.assertEqual(out["analysis"], None)


class TestAttachmentAnalysis(TestCase):

    @patch("apps.mail.models.AttachmentAnalysis.get_recent_by_attachment")
    @patch("apps.ai.services.attachment_analysis.get_attachment_logic")
    @patch("apps.ai.services.attachment_analysis.extract_text_from_bytes")
    @patch("apps.ai.services.attachment_analysis.attachment_analysis_chain")
    def test_analyze_gmail_attachment_new(self, mock_chain, mock_extract, mock_get_attachment, mock_cached):
        mock_cached.return_value = None
        mock_get_attachment.return_value = {"data": b"dummy data", "filename": "test.txt", "mime_type": "text/plain"}
        mock_extract.return_value = "extracted text"
        mock_result = MagicMock()
        mock_result.summary = "summary"
        mock_result.insights = "insights"
        mock_result.mail_guide = "guide"
        mock_result.model_dump.return_value = {"summary": "summary", "insights": "insights", "mail_guide": "guide"}
        mock_chain.invoke.return_value = mock_result

        user_id = 1
        result = analyze_gmail_attachment(user_id, "msg1", "att1", "test.txt", "text/plain")

        self.assertEqual(result["summary"], "summary")
        self.assertEqual(result["insights"], "insights")
        self.assertEqual(result["mail_guide"], "guide")

    @patch("apps.mail.models.AttachmentAnalysis.get_recent_by_attachment")
    def test_analyze_gmail_attachment_cached(self, mock_cached):
        mock_cached.return_value = MagicMock(summary="cached_summary", insights="cached_insights", mail_guide="cached_guide")

        user_id = 1
        result = analyze_gmail_attachment(user_id, "msg1", "att1", "test.txt", "text/plain")

        self.assertEqual(result["summary"], "cached_summary")
        self.assertEqual(result["insights"], "cached_insights")
        self.assertEqual(result["mail_guide"], "cached_guide")

    @patch("apps.mail.models.AttachmentAnalysis.get_recent_by_content_key")
    @patch("apps.ai.services.attachment_analysis.extract_text_from_bytes")
    @patch("apps.ai.services.attachment_analysis.attachment_analysis_chain")
    def test_analyze_uploaded_file_new(self, mock_chain, mock_extract, mock_cached):
        mock_cached.return_value = None
        mock_extract.return_value = "extracted text"
        mock_result = MagicMock()
        mock_result.summary = "summary"
        mock_result.insights = "insights"
        mock_result.mail_guide = "guide"
        mock_result.model_dump.return_value = {"summary": "summary", "insights": "insights", "mail_guide": "guide"}
        mock_chain.invoke.return_value = mock_result

        class DummyFile:
            name = "upload.txt"

            def read(self):
                return b"dummy data"

            content_type = "text/plain"

        user_id = 1
        file_obj = DummyFile()
        result = analyze_uploaded_file(user_id, file_obj)

        self.assertEqual(result["summary"], "summary")
        self.assertEqual(result["insights"], "insights")
        self.assertEqual(result["mail_guide"], "guide")

    @patch("apps.mail.models.AttachmentAnalysis.get_recent_by_content_key")
    def test_analyze_uploaded_file_cached(self, mock_cached):
        mock_cached.return_value = MagicMock(summary="cached_summary", insights="cached_insights", mail_guide="cached_guide")

        class DummyFile:
            name = "upload.txt"

            def read(self):
                return b"dummy data"

            content_type = "text/plain"

        user_id = 1
        file_obj = DummyFile()
        result = analyze_uploaded_file(user_id, file_obj)

        self.assertEqual(result["summary"], "cached_summary")
        self.assertEqual(result["insights"], "cached_insights")
        self.assertEqual(result["mail_guide"], "cached_guide")
