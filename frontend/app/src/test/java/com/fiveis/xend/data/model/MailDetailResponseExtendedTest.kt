package com.fiveis.xend.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MailDetailResponseExtendedTest {

    @Test
    fun test_mailDetailResponse_allFieldsSet() {
        val response = MailDetailResponse(
            id = "msg123",
            threadId = "thread456",
            subject = "Test Email",
            fromEmail = "sender@example.com",
            toEmail = "recipient@example.com",
            to = "Recipient Name",
            date = "2025-01-15",
            dateRaw = "1705334400000",
            body = "This is the email body",
            snippet = "This is...",
            isUnread = true,
            labelIds = listOf("INBOX", "IMPORTANT"),
            attachments = emptyList()
        )

        assertEquals("msg123", response.id)
        assertEquals("thread456", response.threadId)
        assertEquals("Test Email", response.subject)
        assertEquals("sender@example.com", response.fromEmail)
        assertEquals("recipient@example.com", response.toEmail)
        assertEquals("Recipient Name", response.to)
        assertEquals("2025-01-15", response.date)
        assertEquals("1705334400000", response.dateRaw)
        assertEquals("This is the email body", response.body)
        assertEquals("This is...", response.snippet)
        assertTrue(response.isUnread)
        assertEquals(2, response.labelIds.size)
        assertEquals(0, response.attachments.size)
    }

    @Test
    fun test_mailDetailResponse_withAttachments() {
        val attachment1 = Attachment(
            filename = "document.pdf",
            mimeType = "application/pdf",
            size = 1024,
            attachmentId = "att1"
        )
        val attachment2 = Attachment(
            filename = "image.jpg",
            mimeType = "image/jpeg",
            size = 2048,
            attachmentId = "att2"
        )
        val response = MailDetailResponse(
            id = "msg123",
            threadId = "thread456",
            subject = "Email with Attachments",
            fromEmail = "sender@example.com",
            toEmail = "recipient@example.com",
            to = "Recipient",
            date = "2025-01-15",
            dateRaw = "1705334400000",
            body = "Check attachments",
            snippet = "Check...",
            isUnread = false,
            labelIds = listOf("INBOX"),
            attachments = listOf(attachment1, attachment2)
        )

        assertEquals(2, response.attachments.size)
        assertEquals("document.pdf", response.attachments[0].filename)
        assertEquals("image.jpg", response.attachments[1].filename)
        assertEquals(1024, response.attachments[0].size)
        assertEquals(2048, response.attachments[1].size)
    }

    @Test
    fun test_mailDetailResponse_readEmail() {
        val response = MailDetailResponse(
            id = "msg123",
            threadId = "thread456",
            subject = "Read Email",
            fromEmail = "sender@example.com",
            toEmail = "recipient@example.com",
            to = "Recipient",
            date = "2025-01-15",
            dateRaw = "1705334400000",
            body = "Already read",
            snippet = "Already...",
            isUnread = false,
            labelIds = listOf("INBOX"),
            attachments = emptyList()
        )

        assertFalse(response.isUnread)
    }

    @Test
    fun test_mailDetailResponse_emptyLabels() {
        val response = MailDetailResponse(
            id = "msg123",
            threadId = "thread456",
            subject = "No Labels",
            fromEmail = "sender@example.com",
            toEmail = "recipient@example.com",
            to = "Recipient",
            date = "2025-01-15",
            dateRaw = "1705334400000",
            body = "No labels",
            snippet = "No...",
            isUnread = true,
            labelIds = emptyList(),
            attachments = emptyList()
        )

        assertEquals(0, response.labelIds.size)
    }

    @Test
    fun test_mailDetailResponse_multipleLabels() {
        val response = MailDetailResponse(
            id = "msg123",
            threadId = "thread456",
            subject = "Multiple Labels",
            fromEmail = "sender@example.com",
            toEmail = "recipient@example.com",
            to = "Recipient",
            date = "2025-01-15",
            dateRaw = "1705334400000",
            body = "Test",
            snippet = "Test...",
            isUnread = false,
            labelIds = listOf("INBOX", "IMPORTANT", "STARRED", "CATEGORY_PERSONAL"),
            attachments = emptyList()
        )

        assertEquals(4, response.labelIds.size)
        assertTrue(response.labelIds.contains("INBOX"))
        assertTrue(response.labelIds.contains("IMPORTANT"))
        assertTrue(response.labelIds.contains("STARRED"))
        assertTrue(response.labelIds.contains("CATEGORY_PERSONAL"))
    }

    @Test
    fun test_mailDetailResponse_longBody() {
        val longBody = "A".repeat(10000)
        val response = MailDetailResponse(
            id = "msg123",
            threadId = "thread456",
            subject = "Long Email",
            fromEmail = "sender@example.com",
            toEmail = "recipient@example.com",
            to = "Recipient",
            date = "2025-01-15",
            dateRaw = "1705334400000",
            body = longBody,
            snippet = "AAA...",
            isUnread = true,
            labelIds = listOf("INBOX"),
            attachments = emptyList()
        )

        assertEquals(10000, response.body.length)
    }

    @Test
    fun test_mailDetailResponse_emptySubject() {
        val response = MailDetailResponse(
            id = "msg123",
            threadId = "thread456",
            subject = "",
            fromEmail = "sender@example.com",
            toEmail = "recipient@example.com",
            to = "Recipient",
            date = "2025-01-15",
            dateRaw = "1705334400000",
            body = "No subject",
            snippet = "No...",
            isUnread = false,
            labelIds = listOf("INBOX"),
            attachments = emptyList()
        )

        assertEquals("", response.subject)
    }

    @Test
    fun test_mailDetailResponse_specialCharactersInSubject() {
        val response = MailDetailResponse(
            id = "msg123",
            threadId = "thread456",
            subject = "Re: 한글 제목 & Special chars!!! 💌",
            fromEmail = "sender@example.com",
            toEmail = "recipient@example.com",
            to = "Recipient",
            date = "2025-01-15",
            dateRaw = "1705334400000",
            body = "Test",
            snippet = "Test...",
            isUnread = true,
            labelIds = listOf("INBOX"),
            attachments = emptyList()
        )

        assertEquals("Re: 한글 제목 & Special chars!!! 💌", response.subject)
    }

    @Test
    fun test_mailDetailResponse_copy() {
        val original = MailDetailResponse(
            id = "msg123",
            threadId = "thread456",
            subject = "Original",
            fromEmail = "sender@example.com",
            toEmail = "recipient@example.com",
            to = "Recipient",
            date = "2025-01-15",
            dateRaw = "1705334400000",
            body = "Original body",
            snippet = "Original...",
            isUnread = true,
            labelIds = listOf("INBOX"),
            attachments = emptyList()
        )
        val modified = original.copy(subject = "Modified", isUnread = false)

        assertEquals("Original", original.subject)
        assertTrue(original.isUnread)
        assertEquals("Modified", modified.subject)
        assertFalse(modified.isUnread)
    }
}
