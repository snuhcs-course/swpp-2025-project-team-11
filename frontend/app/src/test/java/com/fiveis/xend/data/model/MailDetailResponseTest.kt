package com.fiveis.xend.data.model

import org.junit.Assert.*
import org.junit.Test

class MailDetailResponseTest {

    @Test
    fun create_mail_detail_response_with_all_fields() {
        val response = MailDetailResponse(
            id = "mail-123",
            thread_id = "thread-456",
            subject = "Meeting Tomorrow",
            from_email = "sender@example.com",
            to = "recipient@example.com",
            date = "2025-10-30",
            date_raw = "1698624000",
            body = "Let's have a meeting tomorrow at 10 AM.",
            snippet = "Let's have a meeting tomorrow...",
            is_unread = true,
            label_ids = listOf("INBOX", "IMPORTANT")
        )

        assertEquals("mail-123", response.id)
        assertEquals("thread-456", response.thread_id)
        assertEquals("Meeting Tomorrow", response.subject)
        assertEquals("sender@example.com", response.from_email)
        assertEquals("recipient@example.com", response.to)
        assertEquals("2025-10-30", response.date)
        assertEquals("1698624000", response.date_raw)
        assertEquals("Let's have a meeting tomorrow at 10 AM.", response.body)
        assertEquals("Let's have a meeting tomorrow...", response.snippet)
        assertTrue(response.is_unread)
        assertEquals(2, response.label_ids.size)
    }

    @Test
    fun create_mail_detail_response_with_read_status() {
        val response = MailDetailResponse(
            id = "mail-789",
            thread_id = "thread-abc",
            subject = "Read Email",
            from_email = "from@test.com",
            to = "to@test.com",
            date = "2025-10-29",
            date_raw = "1698537600",
            body = "This email has been read.",
            snippet = "This email has been read.",
            is_unread = false,
            label_ids = listOf("INBOX")
        )

        assertFalse(response.is_unread)
    }

    @Test
    fun mail_detail_response_copy_updates_subject() {
        val original = MailDetailResponse(
            id = "id",
            thread_id = "thread",
            subject = "Original Subject",
            from_email = "from@test.com",
            to = "to@test.com",
            date = "2025-10-30",
            date_raw = "1698624000",
            body = "Body",
            snippet = "Snippet",
            is_unread = true,
            label_ids = emptyList()
        )

        val updated = original.copy(subject = "Updated Subject")

        assertEquals("Updated Subject", updated.subject)
        assertEquals("Original Subject", original.subject)
    }

    @Test
    fun mail_detail_responses_with_same_values_are_equal() {
        val response1 = MailDetailResponse(
            id = "same-id",
            thread_id = "same-thread",
            subject = "Same Subject",
            from_email = "same@test.com",
            to = "same@test.com",
            date = "2025-10-30",
            date_raw = "1698624000",
            body = "Same body",
            snippet = "Same snippet",
            is_unread = true,
            label_ids = listOf("INBOX")
        )

        val response2 = MailDetailResponse(
            id = "same-id",
            thread_id = "same-thread",
            subject = "Same Subject",
            from_email = "same@test.com",
            to = "same@test.com",
            date = "2025-10-30",
            date_raw = "1698624000",
            body = "Same body",
            snippet = "Same snippet",
            is_unread = true,
            label_ids = listOf("INBOX")
        )

        assertEquals(response1, response2)
        assertEquals(response1.hashCode(), response2.hashCode())
    }

    @Test
    fun mail_detail_responses_with_different_ids_are_not_equal() {
        val response1 = MailDetailResponse(
            id = "id1",
            thread_id = "thread",
            subject = "Subject",
            from_email = "from@test.com",
            to = "to@test.com",
            date = "2025-10-30",
            date_raw = "1698624000",
            body = "Body",
            snippet = "Snippet",
            is_unread = true,
            label_ids = emptyList()
        )

        val response2 = MailDetailResponse(
            id = "id2",
            thread_id = "thread",
            subject = "Subject",
            from_email = "from@test.com",
            to = "to@test.com",
            date = "2025-10-30",
            date_raw = "1698624000",
            body = "Body",
            snippet = "Snippet",
            is_unread = true,
            label_ids = emptyList()
        )

        assertNotEquals(response1, response2)
    }

    @Test
    fun mail_detail_response_to_string_contains_subject() {
        val response = MailDetailResponse(
            id = "id",
            thread_id = "thread",
            subject = "Important Email",
            from_email = "from@test.com",
            to = "to@test.com",
            date = "2025-10-30",
            date_raw = "1698624000",
            body = "Body",
            snippet = "Snippet",
            is_unread = true,
            label_ids = emptyList()
        )

        val toString = response.toString()
        assertTrue(toString.contains("Important Email"))
    }

    @Test
    fun mail_detail_response_with_empty_label_ids() {
        val response = MailDetailResponse(
            id = "id",
            thread_id = "thread",
            subject = "Subject",
            from_email = "from@test.com",
            to = "to@test.com",
            date = "2025-10-30",
            date_raw = "1698624000",
            body = "Body",
            snippet = "Snippet",
            is_unread = false,
            label_ids = emptyList()
        )

        assertTrue(response.label_ids.isEmpty())
    }
}
