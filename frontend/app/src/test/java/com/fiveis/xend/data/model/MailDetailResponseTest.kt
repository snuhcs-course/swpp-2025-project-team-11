package com.fiveis.xend.data.model

import org.junit.Assert.*
import org.junit.Test

class MailDetailResponseTest {

    private fun attachmentList() = listOf(
        Attachment(
            attachmentId = "att-1",
            filename = "report.pdf",
            mimeType = "application/pdf",
            size = 1024
        )
    )

    @Test
    fun create_mail_detail_response_with_all_fields() {
        val response = MailDetailResponse(
            id = "mail-123",
            threadId = "thread-456",
            subject = "Meeting Tomorrow",
            fromEmail = "sender@example.com",
            toEmail = "recipient@example.com",
            to = "Recipient <recipient@example.com>",
            date = "2025-10-30",
            dateRaw = "1698624000",
            body = "Let's have a meeting tomorrow at 10 AM.",
            snippet = "Let's have a meeting tomorrow...",
            isUnread = true,
            labelIds = listOf("INBOX", "IMPORTANT"),
            attachments = attachmentList()
        )

        assertEquals("mail-123", response.id)
        assertEquals("thread-456", response.threadId)
        assertEquals("Meeting Tomorrow", response.subject)
        assertEquals("sender@example.com", response.fromEmail)
        assertEquals("recipient@example.com", response.toEmail)
        assertEquals("2025-10-30", response.date)
        assertEquals("1698624000", response.dateRaw)
        assertEquals("Let's have a meeting tomorrow at 10 AM.", response.body)
        assertEquals("Let's have a meeting tomorrow...", response.snippet)
        assertTrue(response.isUnread)
        assertEquals(2, response.labelIds.size)
        assertEquals(1, response.attachments.size)
    }

    @Test
    fun create_mail_detail_response_with_read_status() {
        val response = MailDetailResponse(
            id = "mail-789",
            threadId = "thread-abc",
            subject = "Read Email",
            fromEmail = "from@test.com",
            toEmail = "to@test.com",
            to = "to@test.com",
            date = "2025-10-29",
            dateRaw = "1698537600",
            body = "This email has been read.",
            snippet = "This email has been read.",
            isUnread = false,
            labelIds = listOf("INBOX")
        )

        assertFalse(response.isUnread)
    }

    @Test
    fun mail_detail_response_copy_updates_subject() {
        val original = MailDetailResponse(
            id = "id",
            threadId = "thread",
            subject = "Original Subject",
            fromEmail = "from@test.com",
            toEmail = "to@test.com",
            to = "to@test.com",
            date = "2025-10-30",
            dateRaw = "1698624000",
            body = "Body",
            snippet = "Snippet",
            isUnread = true,
            labelIds = emptyList()
        )

        val updated = original.copy(subject = "Updated Subject")

        assertEquals("Updated Subject", updated.subject)
        assertEquals("Original Subject", original.subject)
    }

    @Test
    fun mail_detail_responses_with_same_values_are_equal() {
        val response1 = MailDetailResponse(
            id = "same-id",
            threadId = "same-thread",
            subject = "Same Subject",
            fromEmail = "same@test.com",
            toEmail = "same@test.com",
            to = "same@test.com",
            date = "2025-10-30",
            dateRaw = "1698624000",
            body = "Same body",
            snippet = "Same snippet",
            isUnread = true,
            labelIds = listOf("INBOX"),
            attachments = attachmentList()
        )

        val response2 = response1.copy()

        assertEquals(response1, response2)
        assertEquals(response1.hashCode(), response2.hashCode())
    }

    @Test
    fun mail_detail_responses_with_different_ids_are_not_equal() {
        val response1 = MailDetailResponse(
            id = "id1",
            threadId = "thread",
            subject = "Subject",
            fromEmail = "from@test.com",
            toEmail = "to@test.com",
            to = "to@test.com",
            date = "2025-10-30",
            dateRaw = "1698624000",
            body = "Body",
            snippet = "Snippet",
            isUnread = true,
            labelIds = emptyList()
        )

        val response2 = response1.copy(id = "id2")

        assertNotEquals(response1, response2)
    }

    @Test
    fun mail_detail_response_to_string_contains_subject() {
        val response = MailDetailResponse(
            id = "id",
            threadId = "thread",
            subject = "Important Email",
            fromEmail = "from@test.com",
            toEmail = "to@test.com",
            to = "to@test.com",
            date = "2025-10-30",
            dateRaw = "1698624000",
            body = "Body",
            snippet = "Snippet",
            isUnread = true,
            labelIds = emptyList()
        )

        val toString = response.toString()
        assertTrue(toString.contains("Important Email"))
    }

    @Test
    fun mail_detail_response_with_empty_label_ids() {
        val response = MailDetailResponse(
            id = "id",
            threadId = "thread",
            subject = "Subject",
            fromEmail = "from@test.com",
            toEmail = "to@test.com",
            to = "to@test.com",
            date = "2025-10-30",
            dateRaw = "1698624000",
            body = "Body",
            snippet = "Snippet",
            isUnread = false,
            labelIds = emptyList()
        )

        assertTrue(response.labelIds.isEmpty())
    }
}
