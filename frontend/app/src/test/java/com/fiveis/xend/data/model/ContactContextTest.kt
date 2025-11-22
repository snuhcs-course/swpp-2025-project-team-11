package com.fiveis.xend.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactContextTest {

    @Test
    fun test_createContactContext_allFields() {
        val context = ContactContext(
            id = 1L,
            senderRole = "Manager",
            recipientRole = "Employee",
            relationshipDetails = "Direct report",
            personalPrompt = "Be encouraging",
            languagePreference = "en",
            createdAt = "2025-01-01",
            updatedAt = "2025-01-02"
        )

        assertEquals(1L, context.id)
        assertEquals("Manager", context.senderRole)
        assertEquals("Employee", context.recipientRole)
        assertEquals("Direct report", context.relationshipDetails)
        assertEquals("Be encouraging", context.personalPrompt)
        assertEquals("en", context.languagePreference)
        assertEquals("2025-01-01", context.createdAt)
        assertEquals("2025-01-02", context.updatedAt)
    }

    @Test
    fun test_createContactContext_nullableFields() {
        val context = ContactContext(
            id = 1L,
            senderRole = null,
            recipientRole = null,
            relationshipDetails = null,
            personalPrompt = null,
            languagePreference = null,
            createdAt = null,
            updatedAt = null
        )

        assertNull(context.senderRole)
        assertNull(context.recipientRole)
        assertNull(context.relationshipDetails)
        assertNull(context.personalPrompt)
        assertNull(context.languagePreference)
    }

    @Test
    fun test_copy_senderRole() {
        val original = ContactContext(
            id = 1L,
            senderRole = "Colleague"
        )
        val modified = original.copy(senderRole = "Friend")

        assertEquals("Colleague", original.senderRole)
        assertEquals("Friend", modified.senderRole)
    }

    @Test
    fun test_copy_recipientRole() {
        val original = ContactContext(
            id = 1L,
            recipientRole = "Colleague"
        )
        val modified = original.copy(recipientRole = "Manager")

        assertEquals("Colleague", original.recipientRole)
        assertEquals("Manager", modified.recipientRole)
    }

    @Test
    fun test_copy_relationshipDetails() {
        val original = ContactContext(
            id = 1L,
            relationshipDetails = "Friend from university"
        )
        val modified = original.copy(relationshipDetails = "Close friend")

        assertEquals("Friend from university", original.relationshipDetails)
        assertEquals("Close friend", modified.relationshipDetails)
    }

    @Test
    fun test_copy_personalPrompt() {
        val original = ContactContext(
            id = 1L,
            personalPrompt = "Be formal"
        )
        val modified = original.copy(personalPrompt = "Be casual")

        assertEquals("Be formal", original.personalPrompt)
        assertEquals("Be casual", modified.personalPrompt)
    }

    @Test
    fun test_copy_languagePreference() {
        val original = ContactContext(
            id = 1L,
            languagePreference = "en"
        )
        val modified = original.copy(languagePreference = "ko")

        assertEquals("en", original.languagePreference)
        assertEquals("ko", modified.languagePreference)
    }

    @Test
    fun test_equality_sameValues() {
        val context1 = ContactContext(
            id = 1L,
            senderRole = "Manager",
            recipientRole = "Employee"
        )
        val context2 = ContactContext(
            id = 1L,
            senderRole = "Manager",
            recipientRole = "Employee"
        )

        assertEquals(context1, context2)
        assertEquals(context1.hashCode(), context2.hashCode())
    }

    @Test
    fun test_inequality_differentId() {
        val context1 = ContactContext(id = 1L, senderRole = "Manager")
        val context2 = ContactContext(id = 2L, senderRole = "Manager")

        assertNotEquals(context1, context2)
    }

    @Test
    fun test_copy_multipleFields() {
        val original = ContactContext(
            id = 1L,
            senderRole = "Manager",
            recipientRole = "Employee"
        )
        val modified = original.copy(
            senderRole = "CEO",
            recipientRole = "Manager",
            relationshipDetails = "Direct supervisor"
        )

        assertEquals("CEO", modified.senderRole)
        assertEquals("Manager", modified.recipientRole)
        assertEquals("Direct supervisor", modified.relationshipDetails)
    }

    @Test
    fun test_longPersonalPrompt() {
        val longPrompt = "A".repeat(5000)
        val context = ContactContext(
            id = 1L,
            personalPrompt = longPrompt
        )

        assertEquals(5000, context.personalPrompt?.length)
    }

    @Test
    fun test_specialCharactersInFields() {
        val context = ContactContext(
            id = 1L,
            senderRole = "한글 역할",
            recipientRole = "日本語 役割",
            relationshipDetails = "友達 🎉",
            personalPrompt = "Be nice 😊"
        )

        assertEquals("한글 역할", context.senderRole)
        assertEquals("日本語 役割", context.recipientRole)
        assertEquals("友達 🎉", context.relationshipDetails)
        assertEquals("Be nice 😊", context.personalPrompt)
    }

    @Test
    fun test_languagePreference_variousCodes() {
        val context1 = ContactContext(id = 1L, languagePreference = "en")
        val context2 = ContactContext(id = 2L, languagePreference = "ko")
        val context3 = ContactContext(id = 3L, languagePreference = "ja")
        val context4 = ContactContext(id = 4L, languagePreference = "zh")

        assertEquals("en", context1.languagePreference)
        assertEquals("ko", context2.languagePreference)
        assertEquals("ja", context3.languagePreference)
        assertEquals("zh", context4.languagePreference)
    }
}
