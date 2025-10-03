# Create your views here.
# apps/ai/views.py
from django.http import StreamingHttpResponse
from rest_framework.parsers import JSONParser
from rest_framework.renderers import JSONRenderer
from rest_framework.views import APIView

from .serializers import MailGenerateRequest
from .services.langchain import stream_mail_generation


class MailGenerateStreamView(APIView):
    # permission_classes = [IsAuthenticated]
    parser_classes = [JSONParser]
    renderer_classes = [JSONRenderer]

    def post(self, request):
        serializer = MailGenerateRequest(data=request.data)
        serializer.is_valid(raise_exception=True)
        data = serializer.validated_data

        gen = stream_mail_generation(
            subject=data.get("subject"),
            body=data.get("body"),
            relationship=data.get("relationship"),
            situational_prompt=data.get("situational_prompt"),
            style_prompt=data.get("style_prompt"),
            format_prompt=data.get("format_prompt"),
            language=data.get("language"),
        )

        resp = StreamingHttpResponse(gen, content_type="text/event-stream; charset=utf-8")
        resp["Cache-Control"] = "no-cache"
        resp["X-Accel-Buffering"] = "no"
        return resp
