package com.fiveis.xend.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MailListResponseExtendedTest {

    private fun createEmailItem(id: String) = EmailItem(
        id = id,
        threadId = "thread_$id",
        subject = "Subject $id",
        fromEmail = "sender@example.com",
        snippet = "Snippet $id",
        date = "2024-01-15T10:00:00Z",
        dateRaw = "1705334400000",
        isUnread = false,
        labelIds = listOf("INBOX")
    )

    @Test
    fun test_mailListResponse_withEmails() {
        val messages = listOf(
            createEmailItem("1"),
            createEmailItem("2"),
            createEmailItem("3")
        )
        val response = MailListResponse(
            messages = messages,
            nextPageToken = "token_next_page",
            resultSizeEstimate = 3
        )

        assertEquals(3, response.messages.size)
        assertEquals("token_next_page", response.nextPageToken)
        assertEquals(3, response.resultSizeEstimate)
        assertEquals("Subject 1", response.messages[0].subject)
    }

    @Test
    fun test_mailListResponse_emptyList() {
        val response = MailListResponse(
            messages = emptyList(),
            nextPageToken = null,
            resultSizeEstimate = 0
        )

        assertEquals(0, response.messages.size)
        assertNull(response.nextPageToken)
        assertEquals(0, response.resultSizeEstimate)
    }

    @Test
    fun test_mailListResponse_noNextPageToken() {
        val messages = listOf(createEmailItem("1"))
        val response = MailListResponse(
            messages = messages,
            nextPageToken = null,
            resultSizeEstimate = 1
        )

        assertEquals(1, response.messages.size)
        assertNull(response.nextPageToken)
    }

    @Test
    fun test_mailListResponse_manyEmails() {
        val messages = (1..100).map { createEmailItem(it.toString()) }
        val response = MailListResponse(
            messages = messages,
            nextPageToken = "token_page2",
            resultSizeEstimate = 100
        )

        assertEquals(100, response.messages.size)
        assertEquals("token_page2", response.nextPageToken)
        assertEquals(100, response.resultSizeEstimate)
    }

    @Test
    fun test_mailListResponse_copy() {
        val original = MailListResponse(
            messages = listOf(createEmailItem("1")),
            nextPageToken = "token1",
            resultSizeEstimate = 1
        )
        val modified = original.copy(nextPageToken = "token2")

        assertEquals("token1", original.nextPageToken)
        assertEquals("token2", modified.nextPageToken)
    }

    @Test
    fun test_mailListResponse_equality() {
        val messages = listOf(createEmailItem("1"))
        val response1 = MailListResponse(messages, "token", 1)
        val response2 = MailListResponse(messages, "token", 1)

        assertEquals(response1, response2)
    }

    @Test
    fun test_mailListResponse_longPageToken() {
        val longToken = "A".repeat(500)
        val response = MailListResponse(
            messages = emptyList(),
            nextPageToken = longToken,
            resultSizeEstimate = 0
        )

        assertEquals(500, response.nextPageToken?.length)
    }
}
