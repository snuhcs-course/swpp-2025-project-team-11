from cryptography.fernet import Fernet
from django.conf import settings
from django.utils import timezone
from drf_spectacular.utils import (
    OpenApiParameter,
    OpenApiResponse,
    OpenApiTypes,
    inline_serializer,
)
from googleapiclient.errors import HttpError
from rest_framework import generics, serializers, status
from rest_framework.response import Response
from rest_framework.views import APIView

from apps.user.models import GoogleAccount, User
from apps.user.services import google_refresh

from ..core.mixins import AuthRequiredMixin
from ..core.utils.docs import extend_schema_with_common_errors
from .serializers import (
    EmailDetailSerializer,
    EmailListQuerySerializer,
    EmailListSerializer,
    EmailMarkReadRequestSerializer,
    EmailMarkReadResponseSerializer,
    EmailSendResponseSerializer,
    EmailSendSerializer,
)
from .services import GmailService
from .utils import get_email_detail_logic, list_emails_logic, mark_read_logic, send_email_logic


class EmailListView(generics.GenericAPIView, AuthRequiredMixin):
    """
    GET /api/mail/emails/
    Fetch list of received emails
    """

    query_serializer_class = EmailListQuerySerializer

    @extend_schema_with_common_errors(
        summary="List emails",
        description="List Gmail messages for the authenticated user.",
        request=None,
        parameters=[
            OpenApiParameter(
                name="max_results",
                type=OpenApiTypes.INT,
                location=OpenApiParameter.QUERY,
                required=False,
                description="Max results (1~100). Default: 20",
            ),
            OpenApiParameter(
                name="page_token",
                type=OpenApiTypes.STR,
                location=OpenApiParameter.QUERY,
                required=False,
                description="Pagination token from previous response",
            ),
            OpenApiParameter(
                name="labels",
                type=OpenApiTypes.STR,
                location=OpenApiParameter.QUERY,
                required=False,
                description='Comma-separated labels (e.g., "INBOX,UNREAD"). Default: "INBOX"',
            ),
        ],
        responses={
            200: OpenApiResponse(
                response=inline_serializer(
                    name="EmailListWrappedResponse",
                    fields={
                        "messages": EmailListSerializer(many=True),
                        "next_page_token": serializers.CharField(allow_null=True, required=False),
                        "result_size_estimate": serializers.IntegerField(required=False),
                    },
                ),
                description="messages + next_page_token + result_size_estimate",
            ),
            # 401: OpenApiResponse(description="Unauthorized"),
            # 404: OpenApiResponse(description="Messages not found"),
            # 429: OpenApiResponse(description="Rate limit exceeded"),
            # 500: OpenApiResponse(description="Server error"),
        },
    )
    def get(self, request):
        """
        Query Parameters:
        - max_results: int (default 20, max 100)
        - page_token: str (for pagination)
        - labels: str (comma-separated, e.g. "INBOX,UNREAD")
        """
        user = request.user

        # Parse parameters
        qs = self.query_serializer_class(data=request.query_params)
        qs.is_valid(raise_exception=True)
        max_results = qs.validated_data.get("max_results", 20)
        page_token = qs.validated_data.get("page_token")
        labels = qs.validated_data.get("labels") or "INBOX"
        label_ids = [s.strip() for s in labels.split(",")] if labels else ["INBOX"]

        # Call Gmail API with decorator
        try:
            result, messages = list_emails_logic(user, max_results, page_token, label_ids)
        except ValueError as e:
            return Response({"detail": str(e)}, status=status.HTTP_401_UNAUTHORIZED)
        except HttpError as e:
            if e.resp.status == 404:
                return Response({"detail": "Messages not found"}, status=status.HTTP_404_NOT_FOUND)
            elif e.resp.status == 403:
                return Response(
                    {"detail": "Rate limit exceeded or permission denied"},
                    status=status.HTTP_429_TOO_MANY_REQUESTS,
                )
            elif e.resp.status == 401:
                return Response(
                    {"detail": "Authentication failed"}, status=status.HTTP_401_UNAUTHORIZED
                )
            return Response(
                {"detail": f"Gmail API error: {str(e)}"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )
        except Exception as e:
            return Response(
                {"detail": f"Unexpected error: {str(e)}"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )

        serializer = EmailListSerializer(messages, many=True)

        return Response(
            {
                "messages": serializer.data,
                "next_page_token": result.get("nextPageToken"),
                "result_size_estimate": result.get("resultSizeEstimate", 0),
            },
            status=status.HTTP_200_OK,
        )


class EmailDetailView(generics.GenericAPIView, AuthRequiredMixin):
    """
    GET /api/mail/emails/<message_id>/
    Fetch specific email details
    """

    @extend_schema_with_common_errors(
        summary="Get email detail",
        request=None,
        responses={
            200: EmailDetailSerializer,
        },
    )
    def get(self, request, message_id):
        user = request.user

        # Call Gmail API with decorator
        try:
            message = get_email_detail_logic(user, message_id)
        except ValueError as e:
            return Response({"detail": str(e)}, status=status.HTTP_401_UNAUTHORIZED)
        except HttpError as e:
            if e.resp.status == 404:
                return Response({"detail": "Message not found"}, status=status.HTTP_404_NOT_FOUND)
            elif e.resp.status == 403:
                return Response(
                    {"detail": "Rate limit exceeded or permission denied"},
                    status=status.HTTP_429_TOO_MANY_REQUESTS,
                )
            elif e.resp.status == 401:
                return Response(
                    {"detail": "Authentication failed"}, status=status.HTTP_401_UNAUTHORIZED
                )
            return Response(
                {"detail": f"Gmail API error: {str(e)}"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )
        except Exception as e:
            return Response(
                {"detail": f"Unexpected error: {str(e)}"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )

        serializer = EmailDetailSerializer(message)
        return Response(serializer.data, status=status.HTTP_200_OK)


class EmailSendView(generics.GenericAPIView, AuthRequiredMixin):
    """
    POST /api/mail/emails/send/
    Send an email via Gmail
    """

    serializer_class = EmailSendSerializer

    @extend_schema_with_common_errors(
        summary="Send email",
        responses={
            201: EmailSendResponseSerializer,
        },
    )
    def post(self, request):
        user = request.user

        # Validate request data
        serializer = self.get_serializer(data=request.data)
        if not serializer.is_valid():
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

        # Send email via Gmail API with decorator
        try:
            result = send_email_logic(
                user,
                to=serializer.validated_data["to"],
                subject=serializer.validated_data["subject"],
                body=serializer.validated_data["body"],
            )
        except ValueError as e:
            return Response({"detail": str(e)}, status=status.HTTP_401_UNAUTHORIZED)
        except HttpError as e:
            if e.resp.status == 403:
                return Response(
                    {"detail": "Rate limit exceeded or permission denied"},
                    status=status.HTTP_429_TOO_MANY_REQUESTS,
                )
            elif e.resp.status == 401:
                return Response(
                    {"detail": "Authentication failed"}, status=status.HTTP_401_UNAUTHORIZED
                )
            elif e.resp.status == 400:
                return Response(
                    {"detail": "Invalid email format"}, status=status.HTTP_400_BAD_REQUEST
                )
            return Response(
                {"detail": f"Gmail API error: {str(e)}"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )
        except Exception as e:
            return Response(
                {"detail": f"Unexpected error: {str(e)}"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )

        response_serializer = EmailSendResponseSerializer(result)
        return Response(response_serializer.data, status=status.HTTP_201_CREATED)


class MailTestView(APIView):
    """
    GET /api/mail/test/
    Test endpoint without authentication (for development only)
    """

    def get(self, request):
        # Security check: only available in DEBUG mode
        if not settings.DEBUG:
            return Response(
                {"detail": "This endpoint is only available in DEBUG mode"},
                status=status.HTTP_404_NOT_FOUND,
            )

        # Mock user for testing
        test_email = request.query_params.get("email", "test@example.com")

        try:
            user = User.objects.get(email=test_email)
            google_account = user.google_accounts

            # Check if token is expired
            if google_account.expires_at <= timezone.now():
                access_token = google_refresh(google_account)
                token_status = "refreshed"
            else:
                fernet = Fernet(settings.ENCRYPTION_KEY)
                access_token = fernet.decrypt(google_account.access_token.encode()).decode()
                token_status = "valid"

            # Test Gmail API connection
            gmail_service = GmailService(access_token)
            result = gmail_service.list_messages(max_results=5, label_ids=["INBOX"])

            return Response(
                {
                    "status": "success",
                    "user": user.email,
                    "token_status": token_status,
                    "gmail_api": "connected",
                    "message_count": len(result.get("messages", [])),
                    "result_size_estimate": result.get("resultSizeEstimate", 0),
                },
                status=status.HTTP_200_OK,
            )

        except User.DoesNotExist:
            return Response(
                {"status": "error", "detail": f"User {test_email} not found"},
                status=status.HTTP_404_NOT_FOUND,
            )
        except GoogleAccount.DoesNotExist:
            return Response(
                {"status": "error", "detail": "Google account not linked"},
                status=status.HTTP_404_NOT_FOUND,
            )
        except Exception as e:
            return Response(
                {"status": "error", "detail": str(e)},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )


class EmailMarkReadView(generics.GenericAPIView, AuthRequiredMixin):
    """
    PATCH /api/mail/emails/<message_id>/read/
    Mark email as read or unread
    """

    serializer_class = EmailMarkReadRequestSerializer

    @extend_schema_with_common_errors(
        summary="Mark email read/unread",
        responses={200: EmailMarkReadResponseSerializer},
    )
    def patch(self, request, message_id):
        user = request.user
        is_read = request.data.get("is_read", True)

        # Mark as read or unread with decorator
        try:
            result = mark_read_logic(user, message_id, is_read)
            return Response(
                {"id": result.get("id"), "labelIds": result.get("labelIds", [])},
                status=status.HTTP_200_OK,
            )
        except ValueError as e:
            return Response({"detail": str(e)}, status=status.HTTP_401_UNAUTHORIZED)
        except HttpError as e:
            if e.resp.status == 404:
                return Response({"detail": "Message not found"}, status=status.HTTP_404_NOT_FOUND)
            elif e.resp.status == 403:
                return Response(
                    {"detail": "Rate limit exceeded or permission denied"},
                    status=status.HTTP_429_TOO_MANY_REQUESTS,
                )
            elif e.resp.status == 401:
                return Response(
                    {"detail": "Authentication failed"}, status=status.HTTP_401_UNAUTHORIZED
                )
            return Response(
                {"detail": f"Gmail API error: {str(e)}"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )
        except Exception as e:
            return Response(
                {"detail": f"Unexpected error: {str(e)}"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )
