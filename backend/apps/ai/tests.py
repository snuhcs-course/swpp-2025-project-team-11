import json
import re
from unittest.mock import Mock, patch

from django.contrib.auth import get_user_model
from django.test import SimpleTestCase, TestCase
from django.urls import reverse
from django.utils import timezone
from rest_framework import status
from rest_framework.test import APIClient
from rest_framework_simplejwt.tokens import RefreshToken

from apps.ai.services import langchain as lc
from apps.ai.services import pii_masker as pm
from apps.ai.services.utils import (
    _fetch_fewshot_bodies_for_group,
    _fetch_fewshot_bodies_for_single,
    build_prompt_inputs,
    collect_prompt_context,
    heartbeat,
    sse_event,
)
from apps.contact.models import Contact, ContactContext, Group, PromptOption
from apps.mail.models import SentMail

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
    """generator → list로 한 번에 뽑아주는 헬퍼"""
    return list(gen)


class StreamMailGenerationTest(SimpleTestCase):
    """
    stream_mail_generation() 동작 테스트
    - 컨텍스트/프롬프트 입력 수집이 호출되는지
    - PiiMasker로 마스킹 후 subject_chain/body_chain이 호출되는지
    - SSE 이벤트 시퀀스가 ready → subject → body.delta* → done 형태로 나오는지
    """

    @patch("apps.ai.services.langchain.time.monotonic", side_effect=[0, 0, 0])
    @patch("apps.ai.services.langchain.heartbeat", return_value="HEARTBEAT_EVT")
    @patch("apps.ai.services.langchain.sse_event")
    @patch.object(lc, "_body_chain")
    @patch.object(lc, "_subject_chain")
    @patch("apps.ai.services.langchain.PiiMasker")
    @patch("apps.ai.services.langchain.build_prompt_inputs")
    @patch("apps.ai.services.langchain.collect_prompt_context")
    @patch("apps.ai.services.langchain.unmask_stream")
    @patch("apps.ai.services.langchain.make_req_id", return_value="req123")
    def test_stream_mail_generation_happy_path(
        self,
        mock_make_req_id,
        mock_unmask_stream,
        mock_collect_ctx,
        mock_build_inputs,
        MockPiiMasker,
        mock_subject_chain,
        mock_body_chain,
        mock_sse_event,
        mock_heartbeat,
        _,
    ):
        # 가짜 유저 / 수신자
        user = Mock()
        to_emails = ["a@example.com", "b@example.com"]

        # collect_prompt_context -> ctx 리턴
        mock_collect_ctx.return_value = {"ctx_key": "ctx_val"}

        # build_prompt_inputs -> raw_inputs 리턴
        mock_build_inputs.return_value = {
            "language": "ko",
            "recipients": ["a@example.com", "b@example.com"],
            "group_name": "Team",
            "group_description": "group-desc",
            "prompt_text": "be polite",
            "sender_role": "me",
            "recipient_role": "them",
            "fewshots": ["f1", "f2"],
        }

        # 마스커 모킹
        masker_instance = Mock()
        masker_instance.mask_inputs.return_value = (
            {
                "subject": "MASKED_SUBJ",
                "body": "MASKED_BODY",
                "language": "ko",
                "recipients": ["a@example.com", "b@example.com"],
                "group_name": "Team",
                "group_description": "group-desc",
                "prompt_text": "be polite",
                "sender_role": "me",
                "recipient_role": "them",
                "fewshots": ["f1", "f2"],
            },
            {1: "secret-a"},
        )
        MockPiiMasker.return_value = masker_instance

        # 제목 체인 -> "SUBJECT_OUT   "
        mock_subject_chain.invoke.return_value = "SUBJECT_OUT   "

        # 본문 체인 -> stream() 이터레이터
        mock_body_chain.stream.return_value = iter(["BODY_CHUNK_1", "BODY_CHUNK_2"])

        # unmask_stream 동작 통일
        def _unmask_side_effect(chunks, req_id, mapping):
            # 제목 쪽은 list로 들어옴, 본문 쪽은 iterator로 들어옴
            if isinstance(chunks, list):
                for piece in chunks:
                    yield f"UNMASKED_{piece.strip()}"
            else:
                for piece in chunks:
                    yield f"UNMASKED_{piece}"

        mock_unmask_stream.side_effect = _unmask_side_effect

        # sse_event → 사람이 읽기 쉽게 dict로
        def _sse_side_effect(event_type, payload, eid=None, retry_ms=None):
            data = {"event": event_type, "payload": payload}
            if eid is not None:
                data["eid"] = eid
            if retry_ms is not None:
                data["retry_ms"] = retry_ms
            return data

        mock_sse_event.side_effect = _sse_side_effect

        gen = lc.stream_mail_generation(
            user=user,
            subject="hi there",
            body="this is body",
            to_emails=to_emails,
        )
        out_events = _drain(gen)

        # collect_prompt_context / build_prompt_inputs 호출 확인
        mock_collect_ctx.assert_called_once_with(user, to_emails)
        mock_build_inputs.assert_called_once_with({"ctx_key": "ctx_val"})

        # PiiMasker 호출 확인
        MockPiiMasker.assert_called_once_with("req123")
        masker_instance.mask_inputs.assert_called_once()

        # subject_chain.invoke 에 마스킹된 subject가 들어갔는지
        mock_subject_chain.invoke.assert_called_once()
        called_inputs_for_subject = mock_subject_chain.invoke.call_args[0][0]
        self.assertEqual(called_inputs_for_subject["subject"], "MASKED_SUBJ")

        # body_chain.stream 호출 확인 (locked_subject 등)
        mock_body_chain.stream.assert_called_once()
        locked_inputs = mock_body_chain.stream.call_args[0][0]
        self.assertEqual(locked_inputs["locked_subject"], "SUBJECT_OUT")
        self.assertEqual(locked_inputs["body"], "MASKED_BODY")

        # 이벤트 순서 / 구조
        # 0 ready / 1 subject / 2.. body.delta / last done
        self.assertGreaterEqual(len(out_events), 4)
        self.assertEqual(out_events[0]["event"], "ready")
        self.assertIn("ts", out_events[0]["payload"])

        self.assertEqual(out_events[1]["event"], "subject")
        self.assertEqual(out_events[1]["payload"]["title"], "UNMASKED_SUBJECT_OUT")
        self.assertTrue(out_events[1]["payload"]["text"].startswith("UNMASKED_SUBJECT_OUT"))

        body_chunks = [e for e in out_events if e["event"] == "body.delta"]
        self.assertEqual(len(body_chunks), 2)
        self.assertIn("UNMASKED_BODY_CHUNK_1", body_chunks[0]["payload"]["text"])
        self.assertIn("UNMASKED_BODY_CHUNK_2", body_chunks[1]["payload"]["text"])

        self.assertEqual(out_events[-1]["event"], "done")
        self.assertEqual(out_events[-1]["payload"]["reason"], "stop")


class StreamReplyOptionsLLMTest(SimpleTestCase):
    """
    stream_reply_options_llm() 동작 테스트
    - ready → options → option.delta / option.done → done
    - 내부적으로 각 옵션별로 thread에서 reply_body_chain.astream(async gen)을 돌리며 queue로 이벤트를 전달
    - 우리는 reply_body_chain, plan_chain 등을 mock 해서 결정적인 흐름만 확인
    """

    @patch("apps.ai.services.langchain.time.monotonic", return_value=0)
    @patch("apps.ai.services.langchain.sse_event")
    @patch("apps.ai.services.langchain.PiiMasker")
    @patch("apps.ai.services.langchain.build_prompt_inputs")
    @patch("apps.ai.services.langchain.collect_prompt_context")
    @patch("apps.ai.services.langchain.plan_chain")
    @patch("apps.ai.services.langchain.reply_body_chain")
    @patch("apps.ai.services.langchain.make_req_id", return_value="req777")
    def test_stream_reply_options_llm_basic(
        self,
        mock_make_req_id,
        mock_reply_body_chain,
        mock_plan_chain,
        mock_collect_ctx,
        mock_build_inputs,
        MockPiiMasker,
        mock_sse_event,
        mock_monotonic,
    ):
        user = Mock()
        to_email = "boss@example.com"

        # collect_prompt_context / build_prompt_inputs mock
        mock_collect_ctx.return_value = {"ctx": "val"}
        mock_build_inputs.return_value = {
            "language": "ko",
            "recipients": [to_email],
            "group_description": "desc",
            "prompt_text": "be nice",
            "sender_role": "me",
            "recipient_role": "boss",
        }

        # 마스커 mock
        masker_instance = Mock()
        masker_instance.mask_inputs.return_value = (
            {
                "incoming_subject": "MASKED_SUBJ",
                "incoming_body": "MASKED_BODY",
                "language": "ko",
                "recipients": [to_email],
                "group_description": "desc",
                "prompt_text": "be nice",
                "sender_role": "me",
                "recipient_role": "boss",
            },
            {1: "orig1", 2: "orig2"},
        )
        MockPiiMasker.return_value = masker_instance

        # plan_chain.invoke -> ReplyPlan 유사 객체
        class FakeOpt:
            def __init__(self, t, title):
                self.type = t
                self.title = title

        class FakePlan:
            def __init__(self):
                self.language = "ko"
                self.options = [
                    FakeOpt("긍정 응답", "네, 가능합니다"),
                    FakeOpt("일정 조율", "시간을 조정하고 싶습니다"),
                ]

        mock_plan_chain.invoke.return_value = FakePlan()

        # reply_body_chain.astream -> async generator
        async def fake_astream(_inputs):
            # delta 2조각 생성
            yield "BODY_PART_A"
            yield "BODY_PART_B"

        mock_reply_body_chain.astream.side_effect = fake_astream

        # sse_event -> dict
        def _sse_side_effect(event_type, payload, eid=None, retry_ms=None):
            data = {"event": event_type, "payload": payload}
            if eid is not None:
                data["eid"] = eid
            if retry_ms is not None:
                data["retry_ms"] = retry_ms
            return data

        mock_sse_event.side_effect = _sse_side_effect

        # unmask_stream mock
        with patch("apps.ai.services.langchain.unmask_stream") as mock_unmask_stream:

            def _unmask_side_effect(chunks, req_id, mapping):
                # chunks는 list[str] 또는 async 생성된 본문 조각들에서 온 list[str]
                for c in chunks:
                    yield f"UNMASKED_{c}"

            mock_unmask_stream.side_effect = _unmask_side_effect

            gen = lc.stream_reply_options_llm(
                user=user,
                subject="orig sub",
                body="orig body",
                to_email=to_email,
            )
            out_events = _drain(gen)

        # ready 먼저 나와야 함
        self.assertGreaterEqual(len(out_events), 4)
        self.assertEqual(out_events[0]["event"], "ready")
        self.assertIn("ts", out_events[0]["payload"])

        # options 이벤트 확인
        options_events = [e for e in out_events if e["event"] == "options"]
        self.assertEqual(len(options_events), 1)
        opt_payload = options_events[0]["payload"]
        self.assertEqual(opt_payload["count"], 2)
        self.assertEqual(len(opt_payload["items"]), 2)

        # unmask된 title/타입이 들어왔는지
        first_item = opt_payload["items"][0]
        self.assertEqual(first_item["id"], 0)
        # `네, 가능합니다` -> "UNMASKED_네, 가능합니다"
        self.assertTrue(first_item["title"].startswith("UNMASKED_"))
        self.assertTrue(first_item["type"].startswith("UNMASKED_"))

        # option.delta 들이 존재해야 함 (LLM 본문 생성 스트림)
        delta_events = [e for e in out_events if e["event"] == "option.delta"]
        self.assertTrue(len(delta_events) > 0)

        # 각 옵션마다 option.done 이 한 번씩 나와야 함
        done_events = [e for e in out_events if e["event"] == "option.done"]
        self.assertEqual(len(done_events), 2)

        # 마지막 done 은 reason=all_options_finished
        self.assertEqual(out_events[-1]["event"], "done")
        self.assertEqual(out_events[-1]["payload"]["reason"], "all_options_finished")


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
            "fewshots": ["body1", "body2"],
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
        self.assertEqual(out["fewshots"], ["body1", "body2"])

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


# =========================
# fewshot fetch helpers
# =========================
class FewshotFetchTest(TestCase):
    """
    _fetch_fewshot_bodies_for_single / _fetch_fewshot_bodies_for_group
    의 DB 쿼리 동작을 직접 검증한다.
    """

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

        # mail 3개: body 길이 다르게
        self.mail1 = SentMail.objects.create(
            user=self.user,
            contact=self.contact,
            body="Short body.",
            sent_at=timezone.now(),
        )
        self.mail2 = SentMail.objects.create(
            user=self.user,
            contact=self.contact,
            body="This is a much longer email body that should qualify as long content.",
            sent_at=timezone.now(),
        )
        self.mail3 = SentMail.objects.create(
            user=self.user,
            contact=self.contact,
            body="Another long-ish body for testing purposes.",
            sent_at=timezone.now(),
        )

    def test_fetch_fewshot_bodies_for_single_no_min_len(self):
        out = _fetch_fewshot_bodies_for_single(
            user=self.user,
            contact=self.contact,
            k=2,
            min_body_len=0,
        )
        # 최신순 정렬("-sent_at")이므로 mail3, mail2 가 앞에 올 것이라고 가정
        self.assertEqual(len(out), 2)
        self.assertIn(out[0], [self.mail2.body, self.mail3.body, self.mail1.body])
        self.assertIn(out[1], [self.mail2.body, self.mail3.body, self.mail1.body])

    def test_fetch_fewshot_bodies_for_single_with_min_len(self):
        # min_body_len 크게 잡으면 짧은 body는 빠짐
        out = _fetch_fewshot_bodies_for_single(
            user=self.user,
            contact=self.contact,
            k=5,
            min_body_len=40,
        )
        self.assertTrue(all(len(b) >= 40 for b in out))
        # mail2, mail3 는 길이가 충분히 김
        self.assertIn(self.mail2.body, out)
        self.assertIn(self.mail3.body, out)
        # mail1 은 짧아서 없어야 함
        self.assertNotIn(self.mail1.body, out)

    def test_fetch_fewshot_bodies_for_group(self):
        """
        group 단위 fewshot 추출:
        - 동일 group 에 속한 모든 SentMail 중 최신순으로 k개 반환
        - k=2 이므로 가장 최근 2개만 와도 OK
        """
        out = _fetch_fewshot_bodies_for_group(
            user=self.user,
            group=self.group,
            k=2,
            min_body_len=0,
        )

        # 최소 1개 이상은 나와야 한다
        self.assertGreaterEqual(len(out), 1)

        # 최신 메일들이 반환된다는 점만 검증 (mail2/mail3 내용 중 하나 이상은 있어야 함)
        self.assertTrue(
            any(
                candidate in out
                for candidate in [
                    self.mail2.body,
                    self.mail3.body,
                ]
            ),
            msg=f"Expected one of the newer bodies in out, got {out}",
        )

    def test_fetch_fewshot_bodies_for_single_fallback_to_group(self):
        """
        contact3 자체로는 SentMail 이 없어도,
        같은 그룹(self.group)에 속한 다른 contact 들의 메일로 fallback 되는지 확인.
        """
        # 새 그룹/연락처/메일 구성
        group2 = Group.objects.create(
            user=self.user,
            name="Vendors",
            description="Suppliers etc.",
        )
        contact2 = Contact.objects.create(
            user=self.user,
            group=group2,
            name="Bob",
            email="bob@example.com",
        )

        # contact2 는 메일 없음 → group2 도 메일 없음 → 빈 리스트여야 함
        out_empty = _fetch_fewshot_bodies_for_single(
            user=self.user,
            contact=contact2,
            k=3,
            min_body_len=0,
        )
        self.assertEqual(out_empty, [])

        # contact3: self.group에만 속하지만 본인 메일은 없음
        contact3 = Contact.objects.create(
            user=self.user,
            group=self.group,  # self.group 은 mail1/mail2/mail3 가지고 있음
            name="Carol",
            email="carol@example.com",
        )

        out_fallback = _fetch_fewshot_bodies_for_single(
            user=self.user,
            contact=contact3,
            k=2,
            min_body_len=0,
        )

        # fallback이 동작해서 최소 한 개 이상은 받아야 한다
        self.assertNotEqual(out_fallback, [])

        # fallback 결과에 최신 메일 본문들(mail2/mail3 등)이 포함돼 있는지 검사
        self.assertTrue(
            any(
                candidate in out_fallback
                for candidate in [
                    self.mail2.body,
                    self.mail3.body,
                    self.mail1.body,
                ]
            ),
            msg=f"Expected some group-sourced body in out_fallback, got {out_fallback}",
        )


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

    @patch("apps.ai.services.utils._fetch_fewshot_bodies_for_single", return_value=["FS1", "FS2"])
    def test_single_recipient_full_context(self, mock_fetch_single):
        """
        - 수신자 1명, 등록된 Contact 1명 -> 개인 컨텍스트/그룹 정보/개인 프롬프트/언어까지 다 들어간다.
        - fewshots 는 _fetch_fewshot_bodies_for_single 호출 결과를 넣어야 한다.
        """
        out = collect_prompt_context(
            self.user,
            to_emails=[self.c1.email],
            include_fewshots=True,
            fewshot_k=3,
            min_body_len=0,
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
        self.assertEqual(out["fewshots"], ["FS1", "FS2"])
        mock_fetch_single.assert_called_once()

    @patch("apps.ai.services.utils._fetch_fewshot_bodies_for_group", return_value=["GFS1"])
    def test_multiple_same_group(self, mock_fetch_group):
        """
        - 동일 그룹 소속 두 명을 동시에 보내는 경우
        - group_name/description/prompt_options 은 그 그룹 기준
        - sender_role 등 개인 context 는 빠짐 (base 그대로 None)
        """
        out = collect_prompt_context(
            self.user,
            to_emails=[self.c1.email, self.c2.email],
            include_fewshots=True,
            fewshot_k=2,
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
        self.assertEqual(out["fewshots"], ["GFS1"])
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
            include_fewshots=False,
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

        # include_fewshots=False → fewshots 는 기본 [] 유지
        self.assertEqual(out["fewshots"], [])

    def test_unregistered_email_fallback_label(self):
        """
        - 등록 안 된 이메일이 들어오면 "Recipient {i}" 라벨 사용
        - 연락처/그룹 못 찾으면 base 구조만 반환
        """
        out = collect_prompt_context(
            self.user,
            to_emails=["nobody@example.com"],
            include_fewshots=True,
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
        # fewshots 기본 [] (single인데도 contact가 없으므로 _fetch_fewshot... 호출 안 함)
        self.assertEqual(out["fewshots"], [])
