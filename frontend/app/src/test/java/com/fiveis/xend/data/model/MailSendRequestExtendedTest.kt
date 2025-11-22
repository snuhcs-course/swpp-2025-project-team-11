package com.fiveis.xend.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MailSendRequestExtendedTest {

    @Test
    fun test_mailSendRequest_basic() {
        val request = MailSendRequest(
            to = listOf("recipient@example.com"),
            subject = "Test Subject",
            body = "Test Body"
        )

        assertEquals(1, request.to.size)
        assertEquals("recipient@example.com", request.to[0])
        assertEquals("Test Subject", request.subject)
        assertEquals("Test Body", request.body)
    }

    @Test
    fun test_mailSendRequest_multipleRecipients() {
        val request = MailSendRequest(
            to = listOf("alice@example.com", "bob@example.com", "charlie@example.com"),
            subject = "Meeting",
            body = "Let's meet"
        )

        assertEquals(3, request.to.size)
        assertEquals("alice@example.com", request.to[0])
        assertEquals("bob@example.com", request.to[1])
        assertEquals("charlie@example.com", request.to[2])
    }

    @Test
    fun test_mailSendRequest_emptyRecipients() {
        val request = MailSendRequest(
            to = emptyList(),
            subject = "Subject",
            body = "Body"
        )

        assertEquals(0, request.to.size)
    }

    @Test
    fun test_mailSendRequest_longSubject() {
        val longSubject = "A".repeat(1000)
        val request = MailSendRequest(
            to = listOf("test@example.com"),
            subject = longSubject,
            body = "Body"
        )

        assertEquals(1000, request.subject.length)
    }

    @Test
    fun test_mailSendRequest_longBody() {
        val longBody = "This is a very long email body. ".repeat(500)
        val request = MailSendRequest(
            to = listOf("test@example.com"),
            subject = "Subject",
            body = longBody
        )

        assertTrue(request.body.length > 10000)
    }

    @Test
    fun test_mailSendRequest_htmlBody() {
        val htmlBody = "<html><body><h1>Title</h1><p>Paragraph</p></body></html>"
        val request = MailSendRequest(
            to = listOf("test@example.com"),
            subject = "HTML Email",
            body = htmlBody
        )

        assertTrue(request.body.contains("<html>"))
        assertTrue(request.body.contains("<h1>"))
    }

    @Test
    fun test_mailSendRequest_specialCharactersInSubject() {
        val request = MailSendRequest(
            to = listOf("test@example.com"),
            subject = "한글 제목 & Special 💌",
            body = "Body"
        )

        assertEquals("한글 제목 & Special 💌", request.subject)
    }

    @Test
    fun test_mailSendRequest_specialCharactersInBody() {
        val request = MailSendRequest(
            to = listOf("test@example.com"),
            subject = "Subject",
            body = "こんにちは 🌸 Special chars: @#$%"
        )

        assertEquals("こんにちは 🌸 Special chars: @#$%", request.body)
    }

    @Test
    fun test_mailSendRequest_equality() {
        val request1 = MailSendRequest(
            to = listOf("test@example.com"),
            subject = "Subject",
            body = "Body"
        )
        val request2 = MailSendRequest(
            to = listOf("test@example.com"),
            subject = "Subject",
            body = "Body"
        )

        assertEquals(request1, request2)
        assertEquals(request1.hashCode(), request2.hashCode())
    }

    @Test
    fun test_mailSendRequest_inequality_differentTo() {
        val request1 = MailSendRequest(
            to = listOf("test1@example.com"),
            subject = "Subject",
            body = "Body"
        )
        val request2 = MailSendRequest(
            to = listOf("test2@example.com"),
            subject = "Subject",
            body = "Body"
        )

        assertNotEquals(request1, request2)
    }

    @Test
    fun test_mailSendRequest_copy() {
        val original = MailSendRequest(
            to = listOf("original@example.com"),
            subject = "Original",
            body = "Original body"
        )
        val modified = original.copy(subject = "Modified")

        assertEquals("Original", original.subject)
        assertEquals("Modified", modified.subject)
    }

    @Test
    fun test_mailSendRequest_manyRecipients() {
        val recipients = (1..100).map { "user$it@example.com" }
        val request = MailSendRequest(
            to = recipients,
            subject = "Mass Email",
            body = "Body"
        )

        assertEquals(100, request.to.size)
    }

    @Test
    fun test_mailSendRequest_emptySubject() {
        val request = MailSendRequest(
            to = listOf("test@example.com"),
            subject = "",
            body = "Body with no subject"
        )

        assertEquals("", request.subject)
    }

    @Test
    fun test_mailSendRequest_emptyBody() {
        val request = MailSendRequest(
            to = listOf("test@example.com"),
            subject = "Subject",
            body = ""
        )

        assertEquals("", request.body)
    }
}
