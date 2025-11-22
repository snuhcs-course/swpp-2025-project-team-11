package com.fiveis.xend.data.database.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactContextEntityTest {

    @Test
    fun test_construction_withAllFields() {
        val context = ContactContextEntity(
            contactId = 1L,
            senderRole = "Manager",
            recipientRole = "Employee",
            relationshipDetails = "Direct report",
            personalPrompt = "Use formal tone",
            languagePreference = "en",
            createdAt = "2024-01-01",
            updatedAt = "2024-01-02"
        )

        assertEquals(1L, context.contactId)
        assertEquals("Manager", context.senderRole)
        assertEquals("Employee", context.recipientRole)
        assertEquals("Direct report", context.relationshipDetails)
        assertEquals("Use formal tone", context.personalPrompt)
        assertEquals("en", context.languagePreference)
        assertEquals("2024-01-01", context.createdAt)
        assertEquals("2024-01-02", context.updatedAt)
    }

    @Test
    fun test_construction_withNullFields() {
        val context = ContactContextEntity(
            contactId = 1L,
            senderRole = null,
            recipientRole = null,
            relationshipDetails = null,
            personalPrompt = null,
            languagePreference = null,
            createdAt = null,
            updatedAt = null
        )

        assertEquals(1L, context.contactId)
        assertNull(context.senderRole)
        assertNull(context.recipientRole)
        assertNull(context.relationshipDetails)
        assertNull(context.personalPrompt)
        assertNull(context.languagePreference)
    }

    @Test
    fun test_construction_withDefaultValues() {
        val context = ContactContextEntity(contactId = 5L)

        assertEquals(5L, context.contactId)
        assertNull(context.senderRole)
        assertNull(context.recipientRole)
    }

    @Test
    fun test_copy_changesSenderRole() {
        val original = ContactContextEntity(1L, "CEO", "Manager", "Boss", "Casual", "en")
        val modified = original.copy(senderRole = "Director")

        assertEquals("Director", modified.senderRole)
        assertEquals("Manager", modified.recipientRole)
        assertEquals(1L, modified.contactId)
    }

    @Test
    fun test_copy_changesLanguagePreference() {
        val original = ContactContextEntity(1L, languagePreference = "en")
        val modified = original.copy(languagePreference = "ko")

        assertEquals("ko", modified.languagePreference)
    }

    @Test
    fun test_equals_sameValues_returnsTrue() {
        val context1 = ContactContextEntity(1L, "Role1", "Role2", "Details", "Prompt", "en", "2024-01-01", "2024-01-02")
        val context2 = ContactContextEntity(1L, "Role1", "Role2", "Details", "Prompt", "en", "2024-01-01", "2024-01-02")

        assertEquals(context1, context2)
    }
}
