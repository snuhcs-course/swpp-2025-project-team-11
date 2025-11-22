package com.fiveis.xend.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftItemTest {

    @Test
    fun test_construction_withAllFields() {
        val draft = DraftItem(
            id = 1L,
            subject = "Test Subject",
            body = "Test Body",
            recipients = listOf("user1@example.com", "user2@example.com"),
            timestamp = 1234567890L
        )

        assertEquals(1L, draft.id)
        assertEquals("Test Subject", draft.subject)
        assertEquals("Test Body", draft.body)
        assertEquals(2, draft.recipients.size)
        assertEquals("user1@example.com", draft.recipients[0])
        assertEquals(1234567890L, draft.timestamp)
    }

    @Test
    fun test_construction_withDefaultValues() {
        val draft = DraftItem(
            subject = "Subject",
            body = "Body"
        )

        assertEquals(0L, draft.id)
        assertEquals("Subject", draft.subject)
        assertEquals("Body", draft.body)
        assertTrue(draft.recipients.isEmpty())
        assertTrue(draft.timestamp > 0L)
    }

    @Test
    fun test_construction_emptyRecipients() {
        val draft = DraftItem(
            id = 2L,
            subject = "Test",
            body = "Test body",
            recipients = emptyList()
        )

        assertTrue(draft.recipients.isEmpty())
    }

    @Test
    fun test_copy_changesFields() {
        val original = DraftItem(
            id = 1L,
            subject = "Original",
            body = "Original body",
            recipients = listOf("test@example.com"),
            timestamp = 1000L
        )

        val modified = original.copy(subject = "Modified", body = "Modified body")

        assertEquals("Modified", modified.subject)
        assertEquals("Modified body", modified.body)
        assertEquals(1L, modified.id)
        assertEquals(listOf("test@example.com"), modified.recipients)
        assertEquals(1000L, modified.timestamp)
    }

    @Test
    fun test_equals_sameValues_returnsTrue() {
        val draft1 = DraftItem(
            id = 1L,
            subject = "Test",
            body = "Body",
            recipients = listOf("test@example.com"),
            timestamp = 1000L
        )
        val draft2 = DraftItem(
            id = 1L,
            subject = "Test",
            body = "Body",
            recipients = listOf("test@example.com"),
            timestamp = 1000L
        )

        assertEquals(draft1, draft2)
    }
}
