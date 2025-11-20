package com.fiveis.xend.data.database.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactContextEntityExtendedTest {

    @Test
    fun test_contactContextEntity_allFields() {
        val entity = ContactContextEntity(
            contactId = 100L,
            senderRole = "Manager",
            recipientRole = "Employee",
            relationshipDetails = "Direct report",
            personalPrompt = "Be encouraging",
            languagePreference = "en",
            createdAt = "2025-01-01",
            updatedAt = "2025-01-02"
        )

        assertEquals(100L, entity.contactId)
        assertEquals("Manager", entity.senderRole)
        assertEquals("Employee", entity.recipientRole)
        assertEquals("Direct report", entity.relationshipDetails)
        assertEquals("Be encouraging", entity.personalPrompt)
        assertEquals("en", entity.languagePreference)
        assertEquals("2025-01-01", entity.createdAt)
        assertEquals("2025-01-02", entity.updatedAt)
    }

    @Test
    fun test_contactContextEntity_minimalFields() {
        val entity = ContactContextEntity(
            contactId = 100L
        )

        assertEquals(100L, entity.contactId)
        assertNull(entity.senderRole)
        assertNull(entity.recipientRole)
        assertNull(entity.relationshipDetails)
        assertNull(entity.personalPrompt)
        assertNull(entity.languagePreference)
        assertNull(entity.createdAt)
        assertNull(entity.updatedAt)
    }

    @Test
    fun test_contactContextEntity_equality() {
        val entity1 = ContactContextEntity(
            contactId = 100L,
            senderRole = "Manager"
        )
        val entity2 = ContactContextEntity(
            contactId = 100L,
            senderRole = "Manager"
        )

        assertEquals(entity1, entity2)
        assertEquals(entity1.hashCode(), entity2.hashCode())
    }

    @Test
    fun test_contactContextEntity_inequality_differentContactId() {
        val entity1 = ContactContextEntity(contactId = 100L)
        val entity2 = ContactContextEntity(contactId = 200L)

        assertNotEquals(entity1, entity2)
    }

    @Test
    fun test_contactContextEntity_copy() {
        val original = ContactContextEntity(
            contactId = 100L,
            senderRole = "Colleague"
        )
        val modified = original.copy(senderRole = "Friend")

        assertEquals("Colleague", original.senderRole)
        assertEquals("Friend", modified.senderRole)
    }

    @Test
    fun test_contactContextEntity_specialCharacters() {
        val entity = ContactContextEntity(
            contactId = 100L,
            senderRole = "한글 역할",
            recipientRole = "日本語",
            relationshipDetails = "友達 🎉"
        )

        assertEquals("한글 역할", entity.senderRole)
        assertEquals("日本語", entity.recipientRole)
        assertEquals("友達 🎉", entity.relationshipDetails)
    }

    @Test
    fun test_contactContextEntity_longPrompt() {
        val longPrompt = "A".repeat(5000)
        val entity = ContactContextEntity(
            contactId = 100L,
            personalPrompt = longPrompt
        )

        assertEquals(5000, entity.personalPrompt?.length)
    }

    @Test
    fun test_contactContextEntity_variousLanguagePreferences() {
        val languages = listOf("en", "ko", "ja", "zh", "es", "fr")
        languages.forEach { lang ->
            val entity = ContactContextEntity(
                contactId = 100L,
                languagePreference = lang
            )
            assertEquals(lang, entity.languagePreference)
        }
    }

    @Test
    fun test_contactContextEntity_updateTimestamps() {
        val entity = ContactContextEntity(
            contactId = 100L,
            createdAt = "2025-01-01T10:00:00",
            updatedAt = "2025-01-01T10:00:00"
        )
        val updated = entity.copy(updatedAt = "2025-01-15T15:30:00")

        assertEquals("2025-01-01T10:00:00", updated.createdAt)
        assertEquals("2025-01-15T15:30:00", updated.updatedAt)
    }

    @Test
    fun test_contactContextEntity_nullableSenderRole() {
        val entity = ContactContextEntity(
            contactId = 100L,
            senderRole = null,
            recipientRole = "Employee"
        )

        assertNull(entity.senderRole)
        assertEquals("Employee", entity.recipientRole)
    }

    @Test
    fun test_contactContextEntity_multipleContextsForDifferentContacts() {
        val context1 = ContactContextEntity(contactId = 1L, relationshipDetails = "Friend")
        val context2 = ContactContextEntity(contactId = 2L, relationshipDetails = "Colleague")
        val context3 = ContactContextEntity(contactId = 3L, relationshipDetails = "Family")

        assertNotEquals(context1, context2)
        assertNotEquals(context2, context3)
        assertNotEquals(context1, context3)
    }
}
