package com.fiveis.xend.data.model

import android.content.Context
import io.mockk.mockk
import okhttp3.MultipartBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test

class MailSendRequestTest {

    private val mockContext: Context = mockk(relaxed = true)

    @Test
    fun create_mail_send_request_with_required_fields_only() {
        val request = MailSendRequest(
            to = listOf("recipient@example.com"),
            subject = "Test Subject",
            body = "Test body content"
        )

        assertEquals(1, request.to.size)
        assertEquals("recipient@example.com", request.to[0])
        assertTrue(request.cc.isEmpty())
        assertTrue(request.bcc.isEmpty())
        assertEquals("Test Subject", request.subject)
        assertEquals("Test body content", request.body)
        assertTrue(request.isHtml)
    }

    @Test
    fun create_mail_send_request_with_multiple_recipients() {
        val request = MailSendRequest(
            to = listOf("user1@example.com", "user2@example.com", "user3@example.com"),
            subject = "Group Email",
            body = "Message for multiple recipients"
        )

        assertEquals(3, request.to.size)
        assertEquals("user1@example.com", request.to[0])
        assertEquals("user2@example.com", request.to[1])
        assertEquals("user3@example.com", request.to[2])
    }

    @Test
    fun create_mail_send_request_with_cc_recipients() {
        val request = MailSendRequest(
            to = listOf("primary@example.com"),
            cc = listOf("cc1@example.com", "cc2@example.com"),
            subject = "Email with CC",
            body = "Content"
        )

        assertEquals(1, request.to.size)
        assertEquals(2, request.cc.size)
        assertEquals("cc1@example.com", request.cc[0])
        assertEquals("cc2@example.com", request.cc[1])
        assertTrue(request.bcc.isEmpty())
    }

    @Test
    fun create_mail_send_request_with_bcc_recipients() {
        val request = MailSendRequest(
            to = listOf("recipient@example.com"),
            bcc = listOf("bcc@example.com"),
            subject = "Email with BCC",
            body = "Secret copy"
        )

        assertEquals(1, request.to.size)
        assertTrue(request.cc.isEmpty())
        assertEquals(1, request.bcc.size)
        assertEquals("bcc@example.com", request.bcc[0])
    }

    @Test
    fun create_mail_send_request_with_all_fields() {
        val request = MailSendRequest(
            to = listOf("to@example.com"),
            cc = listOf("cc@example.com"),
            bcc = listOf("bcc@example.com"),
            subject = "Complete Email",
            body = "<html><body>HTML content</body></html>",
            isHtml = true
        )

        assertEquals("to@example.com", request.to[0])
        assertEquals("cc@example.com", request.cc[0])
        assertEquals("bcc@example.com", request.bcc[0])
        assertEquals("Complete Email", request.subject)
        assertTrue(request.body.contains("<html>"))
        assertTrue(request.isHtml)
    }

    @Test
    fun create_mail_send_request_with_plain_text() {
        val request = MailSendRequest(
            to = listOf("recipient@example.com"),
            subject = "Plain Text Email",
            body = "Simple plain text",
            isHtml = false
        )

        assertEquals("Simple plain text", request.body)
        assertFalse(request.isHtml)
    }

    @Test
    fun mail_send_request_copy_updates_subject() {
        val original = MailSendRequest(
            to = listOf("user@example.com"),
            subject = "Original Subject",
            body = "Body"
        )

        val updated = original.copy(subject = "Updated Subject")

        assertEquals("Updated Subject", updated.subject)
        assertEquals("Body", updated.body)
        assertEquals(original.to, updated.to)
    }

    @Test
    fun mail_send_requests_with_same_values_are_equal() {
        val request1 = MailSendRequest(
            to = listOf("test@example.com"),
            subject = "Test",
            body = "Content"
        )

        val request2 = MailSendRequest(
            to = listOf("test@example.com"),
            subject = "Test",
            body = "Content"
        )

        assertEquals(request1, request2)
        assertEquals(request1.hashCode(), request2.hashCode())
    }

    @Test
    fun mail_send_requests_with_different_recipients_are_not_equal() {
        val request1 = MailSendRequest(
            to = listOf("user1@example.com"),
            subject = "Test",
            body = "Content"
        )

        val request2 = MailSendRequest(
            to = listOf("user2@example.com"),
            subject = "Test",
            body = "Content"
        )

        assertNotEquals(request1, request2)
    }

    @Test
    fun empty_to_list_creates_valid_request() {
        val request = MailSendRequest(
            to = emptyList(),
            subject = "No recipients",
            body = "Content"
        )

        assertTrue(request.to.isEmpty())
        assertEquals("No recipients", request.subject)
    }

    @Test
    fun toMultipartParts_includes_basic_fields() {
        val request = MailSendRequest(
            to = listOf("recipient@example.com"),
            subject = "Multipart Subject",
            body = "<p>Body</p>",
            isHtml = true
        )

        val parts = request.toMultipartParts(mockContext)
        assertEquals("Multipart Subject", parts.valueFor("subject"))
        assertEquals("<p>Body</p>", parts.valueFor("body"))
        assertEquals("true", parts.valueFor("is_html"))
        assertEquals(listOf("recipient@example.com"), parts.valuesFor("to"))
    }

    @Test
    fun toMultipartParts_preserves_cc_and_bcc() {
        val request = MailSendRequest(
            to = listOf("to@example.com"),
            cc = listOf("cc1@example.com", "cc2@example.com"),
            bcc = listOf("bcc@example.com"),
            subject = "Subject",
            body = "Body"
        )

        val parts = request.toMultipartParts(mockContext)
        assertEquals(listOf("cc1@example.com", "cc2@example.com"), parts.valuesFor("cc"))
        assertEquals(listOf("bcc@example.com"), parts.valuesFor("bcc"))
    }

    private fun List<MultipartBody.Part>.valuesFor(name: String): List<String> =
        filter { it.partName() == name }
            .map { it.bodyAsString() }

    private fun List<MultipartBody.Part>.valueFor(name: String): String? =
        valuesFor(name).firstOrNull()

    private fun MultipartBody.Part.partName(): String? {
        val disposition = headers?.get("Content-Disposition") ?: return null
        return disposition.split(";")
            .map { it.trim() }
            .firstOrNull { it.startsWith("name=") }
            ?.substringAfter("name=\"")
            ?.substringBefore("\"")
    }

    private fun MultipartBody.Part.bodyAsString(): String {
        val buffer = Buffer()
        body.writeTo(buffer)
        return buffer.readUtf8()
    }
}
