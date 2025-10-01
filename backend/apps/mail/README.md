# Mail App - Gmail API Integration

This app provides REST API endpoints for fetching and managing Gmail messages.

## Features

- Fetch inbox messages with pagination
- Get message details
- Mark messages as read/unread
- Uses Gmail API with OAuth2 authentication

## Prerequisites

Before using this app, users must authenticate via Google OAuth2 (handled by `my_auth` app). The user's Google refresh token is stored in the database and used to obtain access tokens.

## API Endpoints

### 1. List Emails

**GET** `/api/mail/emails/`

Fetches a list of emails from the user's Gmail inbox.

**Authentication:** Required (JWT token)

**Query Parameters:**
- `max_results` (int, optional): Maximum number of results (default: 20, max: 100)
- `page_token` (str, optional): Token for pagination
- `labels` (str, optional): Comma-separated labels (default: "INBOX")
  - Examples: "INBOX", "INBOX,UNREAD", "SENT"

**Response:**
```json
{
  "messages": [
    {
      "id": "18f2a3b4c5d6e7f8",
      "thread_id": "18f2a3b4c5d6e7f8",
      "subject": "Meeting Tomorrow",
      "from_email": "sender@example.com",
      "snippet": "Hi, let's meet tomorrow at 10am...",
      "date": "2025-10-01T09:30:00Z",
      "is_unread": true,
      "label_ids": ["INBOX", "UNREAD"]
    }
  ],
  "next_page_token": "token123",
  "result_size_estimate": 42
}
```

**Example Request:**
```bash
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  "http://localhost/api/mail/emails/?max_results=10&labels=INBOX,UNREAD"
```

---

### 2. Get Email Detail

**GET** `/api/mail/emails/<message_id>/`

Fetches detailed information about a specific email.

**Authentication:** Required (JWT token)

**URL Parameters:**
- `message_id` (str, required): Gmail message ID

**Response:**
```json
{
  "id": "18f2a3b4c5d6e7f8",
  "thread_id": "18f2a3b4c5d6e7f8",
  "subject": "Meeting Tomorrow",
  "from_email": "sender@example.com",
  "to": "recipient@example.com",
  "date": "2025-10-01T09:30:00Z",
  "body": "Full email body content...",
  "snippet": "Hi, let's meet tomorrow...",
  "is_unread": true,
  "label_ids": ["INBOX", "UNREAD"]
}
```

**Example Request:**
```bash
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  "http://localhost/api/mail/emails/18f2a3b4c5d6e7f8/"
```

---

### 3. Mark Email as Read/Unread

**PATCH** `/api/mail/emails/<message_id>/read/`

Marks an email as read or unread.

**Authentication:** Required (JWT token)

**URL Parameters:**
- `message_id` (str, required): Gmail message ID

**Request Body:**
```json
{
  "is_read": true
}
```

**Response:**
```json
{
  "detail": "Success",
  "is_read": true
}
```

**Example Request:**
```bash
curl -X PATCH \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"is_read": true}' \
  "http://localhost/api/mail/emails/18f2a3b4c5d6e7f8/read/"
```

---

## Implementation Details

### Architecture

- **views.py**: REST API endpoints using Django REST Framework
- **services.py**: Gmail API integration service
- **models.py**: Email model for optional local caching
- **serializers.py**: Request/response serialization

### Authentication Flow

1. User authenticates via Google OAuth2 (handled by `my_auth` app)
2. User's Google refresh token is stored in database
3. When accessing mail endpoints:
   - JWT token is validated
   - Google refresh token is used to obtain a new access token
   - Access token is used to call Gmail API

### Error Handling

- **401 Unauthorized**: Invalid JWT or missing Google refresh token
- **404 Not Found**: Message ID not found
- **500 Internal Server Error**: Gmail API errors

---

## Installation & Setup

### 1. Install Dependencies

Dependencies are already added to `pyproject.toml`. Install via Poetry (inside Docker):

```bash
poetry install --no-root
```

### 2. Configure Google OAuth2

Ensure the following scopes are requested during OAuth2 flow:
- `https://www.googleapis.com/auth/gmail.readonly`

### 3. Run Migrations

```bash
poetry run python manage.py makemigrations mail
poetry run python manage.py migrate
```

### 4. Test the API

Start the server and test with curl or your frontend:

```bash
# List emails
curl -H "Authorization: Bearer YOUR_JWT" http://localhost/api/mail/emails/

# Get email detail
curl -H "Authorization: Bearer YOUR_JWT" http://localhost/api/mail/emails/MESSAGE_ID/

# Mark as read
curl -X PATCH -H "Authorization: Bearer YOUR_JWT" \
  -H "Content-Type: application/json" \
  -d '{"is_read": true}' \
  http://localhost/api/mail/emails/MESSAGE_ID/read/
```

---

## Future Enhancements

- [ ] Email search functionality
- [ ] Send email API
- [ ] Draft email management
- [ ] Attachment handling
- [ ] Email filtering by date range
- [ ] Bulk operations (mark multiple as read)
- [ ] Webhook support for real-time updates

---

## References

- [Gmail API Documentation](https://developers.google.com/gmail/api)
- [Google OAuth2 Scopes](https://developers.google.com/identity/protocols/oauth2/scopes#gmail)
