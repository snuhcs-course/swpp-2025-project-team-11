from django.http import StreamingHttpResponse
from drf_spectacular.utils import (
    OpenApiExample,
    OpenApiResponse,
    OpenApiTypes,
    extend_schema,
)
from rest_framework import generics, status
from rest_framework.parsers import FormParser, MultiPartParser
from rest_framework.response import Response

from ..core.mixins import AuthRequiredMixin
from ..core.renderers import SSERenderer
from ..core.utils.docs import extend_schema_with_common_errors
from .serializers import (
    AttachmentAnalysisResponseSerializer,
    AttachmentAnalyzeFromMailSerializer,
    AttachmentAnalyzeUploadSerializer,
    MailGenerateAnalysisResponseSerializer,
    MailGenerateRequest,
    PromptPreviewRequestSerializer,
    ReplyGenerateRequest,
)
from .services.attachment_analysis import analyze_gmail_attachment, analyze_uploaded_file
from .services.mail_generation import (
    debug_mail_generation_analysis,
    stream_mail_generation,
    stream_mail_generation_test,
    stream_mail_generation_with_plan,
)
from .services.prompt_preview import generate_prompt_preview
from .services.reply import stream_reply_options_llm
from .services.utils import get_attachments_for_content_keys, get_attachments_for_message


class MailGenerateStreamView(AuthRequiredMixin, generics.GenericAPIView):
    serializer_class = MailGenerateRequest
    renderer_classes = [SSERenderer]

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

        attachment_keys = data.get("attachment_content_keys") or []
        attachments = get_attachments_for_content_keys(request.user, attachment_keys)

        gen = stream_mail_generation(
            user=request.user,
            subject=data.get("subject"),
            body=data.get("body"),
            to_emails=data.get("to_emails"),
            attachments=attachments,
        )

        resp = StreamingHttpResponse(gen, content_type="text/event-stream; charset=utf-8")
        resp["Cache-Control"] = "no-cache"
        resp["X-Accel-Buffering"] = "no"
        return resp


class MailGenerateWithPlanStreamView(AuthRequiredMixin, generics.GenericAPIView):
    serializer_class = MailGenerateRequest
    renderer_classes = [SSERenderer]

    @extend_schema(
        operation_id="mail_generate_with_plan_stream",
        summary="Generate mail via streaming (SSE) **with plan**",
        description=(
            "주어진 입력을 바탕으로 **SSE**(`text/event-stream`)로 메일 생성 과정을 전송합니다.\n\n"
            "이 엔드포인트는 일반 메일 생성 스트리밍과 달리 **본문을 쓰기 전에 LLM이 작성한 계획(plan)을 먼저** 스트리밍합니다.\n"
            "클라이언트는 plan 단계를 UI에 미리 보여주고, 이어서 본문(body) 델타를 수신할 수 있습니다.\n\n"
            "이벤트 순서는 다음과 같습니다:\n"
            "1. `ready` – 연결 및 재시도 힌트\n"
            "2. `plan.start` – 플랜 스트리밍 시작 알림\n"
            "3. `plan.delta` × N – 플랜을 단락/번호별로 스트리밍 (idx는 1부터)\n"
            "4. `plan.done` – 플랜 스트리밍 종료\n"
            "5. `subject` – 언마스크된 최종 제목 1회 전송\n"
            "6. `body.start` – 본문 스트리밍 시작 알림\n"
            "7. `body.delta` × N – 본문 텍스트 조각(언마스크) 스트리밍, seq는 0부터\n"
            "8. `body.done` – 본문 스트리밍 종료\n"
            "9. (옵션) `patched` – LLM validator가 본문을 보정한 경우 보정된 전체 텍스트 전송\n"
            "10. `done` – 모든 처리가 끝났음을 알리는 종료 이벤트\n\n"
            "에러가 발생하면 `error` 이벤트가 먼저 오고 이후 `done` 으로 종료됩니다."
        ),
        request=MailGenerateRequest,
        responses={
            200: ("text/event-stream", None),
        },
        examples=[
            OpenApiExample(
                "SSE success stream (with plan)",
                response_only=True,
                value=(
                    "event: ready\n"
                    'data: {"ts":1731234567890}\n'
                    "retry: 5000\n"
                    "\n"
                    "event: plan.start\n"
                    "id: plan-0\n"
                    "data: {}\n"
                    "\n"
                    "event: plan.delta\n"
                    "id: plan-1\n"
                    'data: {"idx":1, "text":"[1] 인사 및 수업 정보 언급\\n"}\n'
                    "\n"
                    "event: plan.delta\n"
                    "id: plan-2\n"
                    'data: {"idx":2, "text":"[2] 결석 사유(예비군) 명시\\n"}\n'
                    "\n"
                    "event: plan.done\n"
                    "id: plan-done\n"
                    "data: {}\n"
                    "\n"
                    "event: subject\n"
                    "id: 0\n"
                    'data: {"title":"11월 5일 예비군 사유 결석 관련 출석 인정 요청"}\n'
                    "\n"
                    "event: body.start\n"
                    "id: 1\n"
                    "data: {}\n"
                    "\n"
                    "event: body.delta\n"
                    "id: 2\n"
                    'data: {"seq":0, "text":"조교님 안녕하세요. ... "}\n'
                    "\n"
                    "event: body.delta\n"
                    "id: 3\n"
                    'data: {"seq":1, "text":"금일(11월 5일) 예비군 훈련으로 부득이하게 수업에 참석하지 못하였습니다. "}\n'
                    "\n"
                    "event: body.done\n"
                    "id: 4\n"
                    'data: {"text":"\\n"}\n'
                    "\n"
                    "event: patched\n"
                    "id: 5\n"
                    'data: {"text":"조교님 안녕하세요... (보정된 전체 본문)"}\n'
                    "\n"
                    "event: done\n"
                    "id: 6\n"
                    'data: {"reason":"stop"}\n'
                    "\n"
                ),
            ),
            OpenApiExample(
                "SSE error stream (with plan)",
                response_only=True,
                value=(
                    "event: ready\n"
                    'data: {"ts":1731234567890}\n'
                    "retry: 5000\n"
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

        attachment_keys = data.get("attachment_content_keys") or []
        attachments = get_attachments_for_content_keys(request.user, attachment_keys)

        gen = stream_mail_generation_with_plan(
            user=request.user,
            subject=data.get("subject"),
            body=data.get("body"),
            to_emails=data.get("to_emails"),
            attachments=attachments,
        )

        resp = StreamingHttpResponse(gen, content_type="text/event-stream; charset=utf-8")
        resp["Cache-Control"] = "no-cache"
        resp["X-Accel-Buffering"] = "no"
        return resp


class ReplyOptionsStreamView(AuthRequiredMixin, generics.GenericAPIView):
    serializer_class = ReplyGenerateRequest
    renderer_classes = [SSERenderer]

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

        message_id = data.get("message_id") or ""
        attachments = get_attachments_for_message(request.user, message_id) if message_id else []

        resp = StreamingHttpResponse(
            stream_reply_options_llm(
                user=request.user,
                subject=data.get("subject"),
                body=data.get("body"),
                to_email=data.get("to_email"),
                attachments=attachments,
            ),
            content_type="text/event-stream; charset=utf-8",
        )
        resp["Cache-Control"] = "no-cache"
        resp["X-Accel-Buffering"] = "no"
        return resp


class EmailPromptPreviewView(AuthRequiredMixin, generics.GenericAPIView):
    serializer_class = PromptPreviewRequestSerializer

    @extend_schema_with_common_errors(
        summary="Preview mail-generation prompt for given recipients",
        responses={200: {"type": "object", "properties": {"preview_text": {"type": "string"}}}},
    )
    def post(self, request):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        preview_text = generate_prompt_preview(
            user=request.user,
            to_emails=serializer.validated_data["to"],
        )
        return Response({"preview_text": preview_text}, status=status.HTTP_200_OK)


class AttachmentAnalyzeFromMailView(AuthRequiredMixin, generics.GenericAPIView):
    serializer_class = AttachmentAnalyzeFromMailSerializer

    @extend_schema(
        summary="Analyze a Gmail attachment (by message/attachment id)",
        request=AttachmentAnalyzeFromMailSerializer,
        responses={
            200: OpenApiResponse(
                response=AttachmentAnalysisResponseSerializer,
                description="분석 성공",
                examples=[
                    OpenApiExample(
                        "Success",
                        value={
                            "summary": "프로젝트 일정 및 요구사항이 요약된 문서입니다. 마감은 2025-11-20로 명시되어 있습니다.",
                            "insights": "- 핵심 마일스톤은 3단계로 구분됨\n- 위험요인은 데이터 마이그레이션 지연",
                            "mail_guide": "담당자에게 마감 준수 가능 여부와 리소스 보강 필요성 문의 메일을 제안하세요.",
                        },
                    )
                ],
            ),
            400: OpenApiResponse(
                description="검증 실패 / 지원되지 않는 파일 형식 / 파일 사이즈 초과 등",
                examples=[
                    OpenApiExample("Unsupported type", value={"error": "Unsupported file type '.exe'. Supported types are: pdf, docx, txt"}),
                    OpenApiExample("Too large", value={"error": "File size exceeds 20MB limit."}),
                ],
            ),
        },
    )
    def post(self, request, *args, **kwargs):
        ser = self.get_serializer(data=request.data)
        ser.is_valid(raise_exception=True)
        data = ser.validated_data

        try:
            result = analyze_gmail_attachment(
                request.user,
                message_id=data["message_id"],
                attachment_id=data["attachment_id"],
                filename=data["filename"],
                mime_type=data.get("mime_type", ""),
            )
        except ValueError as e:
            return Response({"error": str(e)}, status=status.HTTP_400_BAD_REQUEST)

        return Response(result, status=status.HTTP_200_OK)


class AttachmentAnalyzeUploadView(AuthRequiredMixin, generics.GenericAPIView):
    serializer_class = AttachmentAnalyzeUploadSerializer
    parser_classes = [MultiPartParser, FormParser]

    @extend_schema(
        summary="Analyze an uploaded file",
        request={"multipart/form-data": AttachmentAnalyzeUploadSerializer},
        responses={
            200: OpenApiResponse(
                response=AttachmentAnalysisResponseSerializer,
                description="분석 성공",
                examples=[
                    OpenApiExample(
                        "Success",
                        value={
                            "summary": "회의록 요약: 주요 의사결정은 API 런칭 일정 연기.",
                            "insights": "성능 이슈로 캐시 도입 필요. QA 일정 재조정.",
                            "mail_guide": "PM에게 일정 변경 공지 및 대안(캐시 전략) 제안 메일.",
                            "content_key": "메일 생성 시 필요한 키",
                        },
                    )
                ],
            ),
            400: OpenApiResponse(
                description="검증 실패 / 지원되지 않는 파일 형식 / 파일 사이즈 초과 등",
                examples=[
                    OpenApiExample("Unsupported type", value={"error": "Unsupported file type '.zip'. Supported types are: pdf, docx, txt"}),
                    OpenApiExample("Too large", value={"error": "File size exceeds 20MB limit."}),
                ],
            ),
        },
    )
    def post(self, request, *args, **kwargs):
        ser = self.get_serializer(data=request.data)
        ser.is_valid(raise_exception=True)
        file_obj = ser.validated_data["file"]

        try:
            result = analyze_uploaded_file(request.user, file_obj=file_obj)
        except ValueError as e:
            return Response({"error": str(e)}, status=status.HTTP_400_BAD_REQUEST)

        return Response(result, status=status.HTTP_200_OK)


class MailGenerateAnalysisTestView(AuthRequiredMixin, generics.GenericAPIView):
    serializer_class = MailGenerateRequest

    @extend_schema(
        summary="[Debug] Generate mail with/without analysis (non-streaming)",
        request=MailGenerateRequest,
        responses={
            200: OpenApiResponse(
                response=MailGenerateAnalysisResponseSerializer,
                examples=[
                    OpenApiExample(
                        "Success",
                        value={
                            "analysis": "수신자가 최근 회의에서 언급한 요청사항을 기반으로 공손한 톤 권장.",
                            "without_analysis": {"subject": "프로젝트 관련 안내드립니다", "body": "안녕하세요. 프로젝트 관련하여 안내드립니다..."},
                            "with_analysis": {
                                "subject": "요청하신 프로젝트 관련 안내드립니다",
                                "body": "안녕하세요. 지난 회의에서 요청하신 내용을 바탕으로...",
                            },
                        },
                    )
                ],
            ),
        },
    )
    def post(self, request):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        data = serializer.validated_data

        attachment_keys = data.get("attachment_content_keys") or []
        attachments = get_attachments_for_content_keys(request.user, attachment_keys)

        result = debug_mail_generation_analysis(
            user=request.user,
            subject=data.get("subject"),
            body=data.get("body"),
            to_emails=data.get("to_emails"),
            attachments=attachments,
        )
        return Response(result)


class MailGenerateStreamTestView(AuthRequiredMixin, generics.GenericAPIView):
    renderer_classes = [SSERenderer]

    @extend_schema(
        operation_id="mail_generate_stream_test",
        summary="Generate mail via streaming (SSE) - TEST with dummy data",
        description=(
            "테스트용 SSE 엔드포인트. 실제 AI API 대신 더미 텍스트를 스트리밍합니다.\n"
            "- 인위적인 딜레이를 두고 긴 더미 텍스트를 전송\n"
            "- 실제 엔드포인트와 동일한 이벤트 형식 사용\n"
            "- 프론트엔드 스트리밍 테스트용\n"
            "- 빈 요청({})도 허용됨"
        ),
        request=None,
        responses={
            200: (OpenApiTypes.STR, "text/event-stream"),
        },
    )
    def post(self, request):
        # 테스트용이므로 validation 없이 바로 더미 스트림 전송
        gen = stream_mail_generation_test()

        resp = StreamingHttpResponse(gen, content_type="text/event-stream; charset=utf-8")
        resp["Cache-Control"] = "no-cache"
        resp["X-Accel-Buffering"] = "no"
        return resp
