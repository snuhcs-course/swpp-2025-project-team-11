package com.fiveis.xend.data.model

import org.junit.Assert.*
import org.junit.Test

class MailListResponseTest {

    @Test
    fun create_mail_list_response_with_all_fields() {
        val email1 = EmailItem(
            id = "1",
            threadId = "thread1",
            subject = "Email 1",
            fromEmail = "from1@test.com",
            snippet = "Snippet 1",
            date = "2025-10-30",
            dateRaw = "1698624000",
            isUnread = true,
            labelIds = listOf("INBOX")
        )
        val email2 = EmailItem(
            id = "2",
            threadId = "thread2",
            subject = "Email 2",
            fromEmail = "from2@test.com",
            snippet = "Snippet 2",
            date = "2025-10-29",
            dateRaw = "1698537600",
            isUnread = false,
            labelIds = listOf("SENT")
        )

        val response = MailListResponse(
            messages = listOf(email1, email2),
            nextPageToken = "next-page-token-123",
            resultSizeEstimate = 50
        )

        assertEquals(2, response.messages.size)
        assertEquals("next-page-token-123", response.nextPageToken)
        assertEquals(50, response.resultSizeEstimate)
    }

    @Test
    fun create_mail_list_response_with_null_next_page_token() {
        val response = MailListResponse(
            messages = emptyList(),
            nextPageToken = null,
            resultSizeEstimate = 0
        )

        assertNull(response.nextPageToken)
        assertTrue(response.messages.isEmpty())
        assertEquals(0, response.resultSizeEstimate)
    }

    @Test
    fun create_mail_list_response_with_empty_messages() {
        val response = MailListResponse(
            messages = emptyList(),
            nextPageToken = "token",
            resultSizeEstimate = 0
        )

        assertTrue(response.messages.isEmpty())
    }

    @Test
    fun mail_list_response_copy_updates_next_page_token() {
        val original = MailListResponse(
            messages = emptyList(),
            nextPageToken = "old-token",
            resultSizeEstimate = 10
        )

        val updated = original.copy(nextPageToken = "new-token")

        assertEquals("new-token", updated.nextPageToken)
        assertEquals(10, updated.resultSizeEstimate)
    }

    @Test
    fun mail_list_responses_with_same_values_are_equal() {
        val response1 = MailListResponse(
            messages = emptyList(),
            nextPageToken = "token",
            resultSizeEstimate = 5
        )

        val response2 = MailListResponse(
            messages = emptyList(),
            nextPageToken = "token",
            resultSizeEstimate = 5
        )

        assertEquals(response1, response2)
        assertEquals(response1.hashCode(), response2.hashCode())
    }

    @Test
    fun mail_list_responses_with_different_result_size_are_not_equal() {
        val response1 = MailListResponse(
            messages = emptyList(),
            nextPageToken = "token",
            resultSizeEstimate = 5
        )

        val response2 = MailListResponse(
            messages = emptyList(),
            nextPageToken = "token",
            resultSizeEstimate = 10
        )

        assertNotEquals(response1, response2)
    }

    @Test
    fun mail_list_response_to_string_contains_result_size() {
        val response = MailListResponse(
            messages = emptyList(),
            nextPageToken = "my-token",
            resultSizeEstimate = 25
        )

        val toString = response.toString()
        assertTrue(toString.contains("25") || toString.contains("resultSizeEstimate"))
    }
}
