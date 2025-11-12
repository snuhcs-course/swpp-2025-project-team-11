package com.fiveis.xend.data.model

import org.junit.Assert.*
import org.junit.Test

class EmailItemTest {

    @Test
    fun create_email_item_with_all_fields_succeeds() {
        val emailItem = EmailItem(
            id = "123",
            threadId = "thread-456",
            subject = "Test Subject",
            fromEmail = "sender@example.com",
            snippet = "This is a test email snippet",
            date = "2024-10-30",
            dateRaw = "1698624000",
            isUnread = true,
            labelIds = listOf("INBOX", "IMPORTANT"),
            cachedAt = 1698624000L
        )

        assertEquals("123", emailItem.id)
        assertEquals("thread-456", emailItem.threadId)
        assertEquals("Test Subject", emailItem.subject)
        assertEquals("sender@example.com", emailItem.fromEmail)
        assertEquals("This is a test email snippet", emailItem.snippet)
        assertEquals("2024-10-30", emailItem.date)
        assertEquals("1698624000", emailItem.dateRaw)
        assertTrue(emailItem.isUnread)
        assertEquals(2, emailItem.labelIds.size)
        assertEquals(1698624000L, emailItem.cachedAt)
    }

    @Test
    fun email_item_copy_preserves_values() {
        val original = EmailItem(
            id = "1",
            threadId = "t1",
            subject = "Original",
            fromEmail = "original@test.com",
            snippet = "snippet",
            date = "2024-10-30",
            dateRaw = "1698624000",
            isUnread = true,
            labelIds = listOf("INBOX")
        )

        val copy = original.copy(subject = "Modified")

        assertEquals("1", copy.id)
        assertEquals("Modified", copy.subject)
        assertEquals("original@test.com", copy.fromEmail)
    }

    @Test
    fun email_items_with_same_values_are_equal() {
        val item1 = EmailItem(
            id = "1",
            threadId = "t1",
            subject = "Test",
            fromEmail = "test@example.com",
            snippet = "snippet",
            date = "2024-10-30",
            dateRaw = "1698624000",
            isUnread = false,
            labelIds = listOf("INBOX")
        )

        val item2 = EmailItem(
            id = "1",
            threadId = "t1",
            subject = "Test",
            fromEmail = "test@example.com",
            snippet = "snippet",
            date = "2024-10-30",
            dateRaw = "1698624000",
            isUnread = false,
            labelIds = listOf("INBOX")
        )

        assertEquals(item1, item2)
        assertEquals(item1.hashCode(), item2.hashCode())
    }

    @Test
    fun email_items_with_different_ids_are_not_equal() {
        val item1 = EmailItem(
            id = "1",
            threadId = "t1",
            subject = "Test",
            fromEmail = "test@example.com",
            snippet = "snippet",
            date = "2024-10-30",
            dateRaw = "1698624000",
            isUnread = false,
            labelIds = listOf("INBOX")
        )

        val item2 = item1.copy(id = "2")

        assertNotEquals(item1, item2)
    }

    @Test
    fun email_item_to_string_contains_key_fields() {
        val item = EmailItem(
            id = "123",
            threadId = "t456",
            subject = "Important Email",
            fromEmail = "sender@test.com",
            snippet = "Test snippet",
            date = "2024-10-30",
            dateRaw = "1698624000",
            isUnread = true,
            labelIds = listOf("INBOX", "STARRED")
        )

        val toString = item.toString()

        assertTrue(toString.contains("123"))
        assertTrue(toString.contains("Important Email"))
        assertTrue(toString.contains("sender@test.com"))
    }

    @Test
    fun cached_at_defaults_to_current_time_when_not_provided() {
        val beforeCreation = System.currentTimeMillis()

        val item = EmailItem(
            id = "1",
            threadId = "t1",
            subject = "Test",
            fromEmail = "test@example.com",
            snippet = "snippet",
            date = "2024-10-30",
            dateRaw = "1698624000",
            isUnread = false,
            labelIds = emptyList()
        )

        val afterCreation = System.currentTimeMillis()

        assertTrue(item.cachedAt >= beforeCreation)
        assertTrue(item.cachedAt <= afterCreation)
    }

    @Test
    fun email_item_stores_attachment_metadata() {
        val attachments = listOf(
            Attachment(
                attachmentId = "att-1",
                filename = "report.pdf",
                mimeType = "application/pdf",
                size = 2048
            )
        )

        val item = EmailItem(
            id = "1",
            threadId = "t1",
            subject = "Subject",
            fromEmail = "sender@test.com",
            toEmail = "recipient@test.com",
            snippet = "snippet",
            date = "2024-10-30",
            dateRaw = "1698624000",
            isUnread = true,
            labelIds = listOf("INBOX"),
            attachments = attachments
        )

        assertEquals(attachments, item.attachments)
    }
}
