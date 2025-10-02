from cryptography.fernet import Fernet
from django.conf import settings
from django.utils import timezone
from googleapiclient.errors import HttpError
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework_simplejwt.authentication import JWTAuthentication

from apps.user.models import GoogleAccount, User
from apps.user.services import google_refresh

from .serializers import (
    EmailDetailSerializer,
    EmailListSerializer,
    EmailSendResponseSerializer,
    EmailSendSerializer,
)
from .services import GmailService


class EmailListView(APIView):
    """
    GET /api/mail/emails/
    Fetch list of received emails
    """

    authentication_classes = [JWTAuthentication]
    permission_classes = [IsAuthenticated]

    def get(self, request):
        """
        Query Parameters:
        - max_results: int (default 20, max 100)
        - page_token: str (for pagination)
        - labels: str (comma-separated, e.g. "INBOX,UNREAD")
        """
        user = request.user

        # Get Google access token (refresh using refresh token)
        try:
            google_account = user.google_accounts.get()
            access_token = google_refresh(google_account)
        except GoogleAccount.DoesNotExist:
            return Response(
                {"detail": "Google account not linked"}, status=status.HTTP_401_UNAUTHORIZED
            )
        except (ValueError, TimeoutError) as e:
            return Response({"detail": str(e)}, status=status.HTTP_401_UNAUTHORIZED)

        # Parse parameters
        try:
            max_results = min(int(request.query_params.get("max_results", 20)), 100)
        except (ValueError, TypeError):
            max_results = 20

        page_token = request.query_params.get("page_token")
        labels = request.query_params.get("labels", "INBOX")
        label_ids = [label.strip() for label in labels.split(",")] if labels else ["INBOX"]

        # Call Gmail API
        gmail_service = GmailService(access_token)
        try:
            result = gmail_service.list_messages(
                max_results=max_results, page_token=page_token, label_ids=label_ids
            )
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

        # Fetch detailed info for each message
        messages = []
        for msg in result.get("messages", []):
            try:
                message_detail = gmail_service.get_message(msg["id"])
                messages.append(message_detail)
            except Exception:
                # Skip individual message fetch failures
                continue

        serializer = EmailListSerializer(messages, many=True)

        return Response(
            {
                "messages": serializer.data,
                "next_page_token": result.get("nextPageToken"),
                "result_size_estimate": result.get("resultSizeEstimate", 0),
            },
            status=status.HTTP_200_OK,
        )


class EmailDetailView(APIView):
    """
    GET /api/mail/emails/<message_id>/
    Fetch specific email details
    """

    authentication_classes = [JWTAuthentication]
    permission_classes = [IsAuthenticated]

    def get(self, request, message_id):
        user = request.user

        # Get Google access token
        try:
            google_account = user.google_accounts.get()
            access_token = google_refresh(google_account)
        except GoogleAccount.DoesNotExist:
            return Response(
                {"detail": "Google account not linked"}, status=status.HTTP_401_UNAUTHORIZED
            )
        except (ValueError, TimeoutError) as e:
            return Response({"detail": str(e)}, status=status.HTTP_401_UNAUTHORIZED)

        # Call Gmail API
        gmail_service = GmailService(access_token)
        try:
            message = gmail_service.get_message(message_id)
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


class EmailSendView(APIView):
    """
    POST /api/mail/emails/send/
    Send an email via Gmail
    """

    authentication_classes = [JWTAuthentication]
    permission_classes = [IsAuthenticated]

    def post(self, request):
        user = request.user

        # Validate request data
        serializer = EmailSendSerializer(data=request.data)
        if not serializer.is_valid():
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

        # Get Google access token
        try:
            google_account = user.google_accounts.get()
            access_token = google_refresh(google_account)
        except GoogleAccount.DoesNotExist:
            return Response(
                {"detail": "Google account not linked"}, status=status.HTTP_401_UNAUTHORIZED
            )
        except (ValueError, TimeoutError) as e:
            return Response({"detail": str(e)}, status=status.HTTP_401_UNAUTHORIZED)

        # Send email via Gmail API
        gmail_service = GmailService(access_token)
        try:
            result = gmail_service.send_message(
                to=serializer.validated_data["to"],
                subject=serializer.validated_data["subject"],
                body=serializer.validated_data["body"],
            )
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
        # Mock user for testing
        test_email = request.query_params.get("email", "test@example.com")

        try:
            user = User.objects.get(email=test_email)
            google_account = user.google_accounts.get()

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


class EmailMarkReadView(APIView):
    """
    PATCH /api/mail/emails/<message_id>/read/
    Mark email as read or unread
    """

    authentication_classes = [JWTAuthentication]
    permission_classes = [IsAuthenticated]

    def patch(self, request, message_id):
        user = request.user
        is_read = request.data.get("is_read", True)

        # Get Google access token
        try:
            google_account = user.google_accounts.get()
            access_token = google_refresh(google_account)
        except GoogleAccount.DoesNotExist:
            return Response(
                {"detail": "Google account not linked"}, status=status.HTTP_401_UNAUTHORIZED
            )
        except (ValueError, TimeoutError) as e:
            return Response({"detail": str(e)}, status=status.HTTP_401_UNAUTHORIZED)

        # Mark as read or unread
        gmail_service = GmailService(access_token)
        try:
            if is_read:
                result = gmail_service.mark_as_read(message_id)
            else:
                result = gmail_service.mark_as_unread(message_id)

            return Response(
                {"id": result.get("id"), "labelIds": result.get("labelIds", [])},
                status=status.HTTP_200_OK,
            )
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
