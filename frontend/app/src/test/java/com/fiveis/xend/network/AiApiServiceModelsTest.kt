package com.fiveis.xend.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiApiServiceModelsTest {

    @Test
    fun test_promptPreviewRequest_singleRecipient() {
        val request = PromptPreviewRequest(to = listOf("test@example.com"))

        assertEquals(1, request.to.size)
        assertEquals("test@example.com", request.to[0])
    }

    @Test
    fun test_promptPreviewRequest_multipleRecipients() {
        val request = PromptPreviewRequest(
            to = listOf("alice@example.com", "bob@example.com", "charlie@example.com")
        )

        assertEquals(3, request.to.size)
        assertEquals("alice@example.com", request.to[0])
        assertEquals("bob@example.com", request.to[1])
        assertEquals("charlie@example.com", request.to[2])
    }

    @Test
    fun test_promptPreviewRequest_emptyList() {
        val request = PromptPreviewRequest(to = emptyList())

        assertEquals(0, request.to.size)
    }

    @Test
    fun test_promptPreviewRequest_equality() {
        val request1 = PromptPreviewRequest(to = listOf("test@example.com"))
        val request2 = PromptPreviewRequest(to = listOf("test@example.com"))

        assertEquals(request1, request2)
        assertEquals(request1.hashCode(), request2.hashCode())
    }

    @Test
    fun test_promptPreviewRequest_inequality() {
        val request1 = PromptPreviewRequest(to = listOf("test1@example.com"))
        val request2 = PromptPreviewRequest(to = listOf("test2@example.com"))

        assertNotEquals(request1, request2)
    }

    @Test
    fun test_promptPreviewResponse_withText() {
        val response = PromptPreviewResponse(previewText = "This is a preview")

        assertEquals("This is a preview", response.previewText)
    }

    @Test
    fun test_promptPreviewResponse_nullText() {
        val response = PromptPreviewResponse(previewText = null)

        assertNull(response.previewText)
    }

    @Test
    fun test_promptPreviewResponse_emptyText() {
        val response = PromptPreviewResponse(previewText = "")

        assertEquals("", response.previewText)
    }

    @Test
    fun test_promptPreviewResponse_equality() {
        val response1 = PromptPreviewResponse(previewText = "Preview")
        val response2 = PromptPreviewResponse(previewText = "Preview")

        assertEquals(response1, response2)
        assertEquals(response1.hashCode(), response2.hashCode())
    }

    @Test
    fun test_promptPreviewResponse_equality_bothNull() {
        val response1 = PromptPreviewResponse(previewText = null)
        val response2 = PromptPreviewResponse(previewText = null)

        assertEquals(response1, response2)
    }

    @Test
    fun test_promptPreviewResponse_inequality() {
        val response1 = PromptPreviewResponse(previewText = "Preview 1")
        val response2 = PromptPreviewResponse(previewText = "Preview 2")

        assertNotEquals(response1, response2)
    }

    @Test
    fun test_promptPreviewRequest_copy() {
        val original = PromptPreviewRequest(to = listOf("test@example.com"))
        val modified = original.copy(to = listOf("new@example.com"))

        assertEquals("test@example.com", original.to[0])
        assertEquals("new@example.com", modified.to[0])
    }

    @Test
    fun test_promptPreviewResponse_copy() {
        val original = PromptPreviewResponse(previewText = "Original")
        val modified = original.copy(previewText = "Modified")

        assertEquals("Original", original.previewText)
        assertEquals("Modified", modified.previewText)
    }

    @Test
    fun test_promptPreviewRequest_toString() {
        val request = PromptPreviewRequest(to = listOf("test@example.com"))
        val string = request.toString()

        assert(string.contains("test@example.com"))
    }

    @Test
    fun test_promptPreviewResponse_toString() {
        val response = PromptPreviewResponse(previewText = "Test preview")
        val string = response.toString()

        assert(string.contains("Test preview"))
    }

    @Test
    fun test_mailSuggestRequest_defaults() {
        val request = MailSuggestRequest(
            subject = "Hello",
            body = "Body",
            toEmails = listOf("user@example.com")
        )

        assertEquals("Hello", request.subject)
        assertEquals("Body", request.body)
        assertEquals(listOf("user@example.com"), request.toEmails)
        assertEquals("body", request.target)
        assertNull(request.cursor)
    }

    @Test
    fun test_mailSuggestResponse_values() {
        val response = MailSuggestResponse(
            target = "body",
            suggestion = "Hi there!"
        )

        assertEquals("body", response.target)
        assertEquals("Hi there!", response.suggestion)
    }
}
