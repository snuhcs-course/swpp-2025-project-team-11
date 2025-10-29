# Create your views here.
# apps/ai/views.py
from django.http import StreamingHttpResponse
from drf_spectacular.utils import (
    OpenApiExample,
    OpenApiTypes,
    extend_schema,
)
from rest_framework import generics

from ..core.mixins import AuthRequiredMixin
from .serializers import MailGenerateRequest, ReplyGenerateRequest
from .services.langchain import stream_mail_generation, stream_reply_options_llm


class MailGenerateStreamView(AuthRequiredMixin, generics.GenericAPIView):
    serializer_class = MailGenerateRequest

    @extend_schema(
        operation_id="mail_generate_stream",
        summary="Generate mail via streaming (SSE)",
        description=(
            "주어진 입력을 바탕으로 **SSE**(text/event-stream)로 메일 생성 이벤트를 전송합니다.\n"
            "- 첫 이벤트: `ready`\n"
            "- 제목: 단일 `subject`\n"
            "- 본문: 다수의 `body.delta` (seq는 0부터 시작, `id`는 1부터 증가)\n"
            "- 종료: `done` (정상), 혹은 `error` 후 종료"
        ),
        request=MailGenerateRequest,
        responses={
            200: (OpenApiTypes.STR, "text/event-stream"),
        },
        examples=[
            OpenApiExample(
                "SSE success stream",
                response_only=True,
                value=(
                    # Ready (+ client-side retry hint). id 없음, retry=5000
                    "event: ready\n"
                    'data: {"ts":1731234567890}\n'
                    "retry: 5000\n"
                    "\n"
                    # Subject (single, non-streaming). id=0
                    "event: subject\n"
                    "id: 0\n"
                    'data: {"title":"Interview Result for Five I\'s",'
                    ' "text":"Interview Result for Five I\'s\\n\\n"}\n'
                    "\n"
                    # Body deltas. 첫 델타: id=1, seq=0
                    "event: body.delta\n"
                    "id: 1\n"
                    'data: {"seq":0, "text":"Hello, and thank you for applying to Five I\'s. "}\n'
                    "\n"
                    # 둘째 델타: id=2, seq=1
                    "event: body.delta\n"
                    "id: 2\n"
                    'data: {"seq":1, "text":"After careful consideration, we ... "}\n'
                    "\n"
                    "event: ping\n"
                    "data: {}\n"
                    "\n"
                    # 마지막 델타 예시: id=3, seq=2
                    "event: body.delta\n"
                    "id: 3\n"
                    'data: {"seq":2, "text":"We appreciate your time and interest. "}\n'
                    "\n"
                    # 종료: done (reason=stop). id=4
                    "event: done\n"
                    "id: 4\n"
                    'data: {"reason":"stop"}\n'
                    "\n"
                ),
            ),
            # 에러 발생 시
            OpenApiExample(
                "SSE error stream",
                response_only=True,
                value=(
                    "event: ready\n"
                    'data: {"ts":1731234567890}\n'
                    "retry: 5000\n"
                    "\n"
                    "event: subject\n"
                    "id: 0\n"
                    'data: {"title":"", "text":""}\n'
                    "\n"
                    "event: error\n"
                    "id: 1\n"
                    'data: {"message":"Upstream LLM timeout"}\n'
                    "\n"
                    "event: done\n"
                    "id: 2\n"
                    'data: {"reason":"stop"}\n'
                    "\n"
                ),
            ),
        ],
    )
    def post(self, request):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        data = serializer.validated_data

        gen = stream_mail_generation(
            user=request.user,
            subject=data.get("subject"),
            body=data.get("body"),
            to_emails=data.get("to_emails"),
        )

        resp = StreamingHttpResponse(gen, content_type="text/event-stream; charset=utf-8")
        resp["Cache-Control"] = "no-cache"
        resp["X-Accel-Buffering"] = "no"
        return resp


class ReplyOptionsStreamView(AuthRequiredMixin, generics.GenericAPIView):
    serializer_class = ReplyGenerateRequest

    @extend_schema(
        operation_id="reply_generate_stream",
        summary="Generate reply options via streaming (SSE)",
        description=(
            "주어진 입력을 바탕으로 **SSE**(text/event-stream)로 '답장 선택지' 생성 이벤트를 전송합니다.\n"
            "- 첫 이벤트: `ready`\n"
            "- 옵션 목록: 단일 `options` (count, items=[{id,type,title}])\n"
            "- 본문: 다수의 `option.delta` (각 옵션별 id=0.., seq=0부터)\n"
            "- 옵션별 종료: `option.done`\n"
            "- 전체 종료: `done` (정상), 혹은 `option.error`/`done`\n"
            "- 중간 상태: `ping` (주기적 heartbeat)"
        ),
        request=ReplyGenerateRequest,
        responses={200: (OpenApiTypes.STR, "text/event-stream")},
        examples=[
            OpenApiExample(
                "SSE success stream",
                response_only=True,
                value=(
                    # Ready
                    "event: ready\n"
                    'data: {"ts":1731234567890}\n'
                    "retry: 5000\n\n"
                    # Options
                    "event: options\n"
                    "id: 1\n"
                    'data: {"count":3,"items":['
                    '{"id":0,"type":"긍정형","title":"회의 일정 변경 수락"},'
                    '{"id":1,"type":"부정형","title":"이번 주는 어려움 안내"},'
                    '{"id":2,"type":"일정 조율형","title":"대체 시간 제안"}]}'
                    # Interleaved deltas
                    "event: option.delta\n"
                    "id: 2\n"
                    'data: {"id":0,"seq":0,"text":"안녕하세요, "} \n\n'
                    "event: option.delta\n"
                    "id: 3\n"
                    'data: {"id":1,"seq":0,"text":"안녕하세요. 보내주신 메일 확인했습니다. "} \n\n'
                    "event: ping\n"
                    "id: 4\n"
                    "data: {}\n\n"
                    "event: option.done\n"
                    "id: 5\n"
                    'data: {"id":0,"total_seq":128}\n\n'
                    "event: option.done\n"
                    "id: 6\n"
                    'data: {"id":1,"total_seq":102}\n\n'
                    "event: option.done\n"
                    "id: 7\n"
                    'data: {"id":2,"total_seq":117}\n\n'
                    "event: done\n"
                    "id: 8\n"
                    'data: {"reason":"all_options_finished"}\n\n'
                ),
            ),
        ],
    )
    def post(self, request):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        data = serializer.validated_data

        def gen():
            yield from stream_reply_options_llm(
                user=request.user,
                subject=data.get("subject"),
                body=data.get("body"),
                to_email=data.get("to_email"),
            )

        resp = StreamingHttpResponse(gen(), content_type="text/event-stream; charset=utf-8")
        resp["Cache-Control"] = "no-cache"
        resp["X-Accel-Buffering"] = "no"
        return resp
