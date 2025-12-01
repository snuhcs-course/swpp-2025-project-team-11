package com.fiveis.xend.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AddContactRequestExtendedTest {

    @Test
    fun test_addContactRequest_allFields() {
        val request = AddContactRequest(
            name = "John Doe",
            email = "john@example.com",
            groupId = 5L,
            context = AddContactRequestContext(
                senderRole = "Manager",
                recipientRole = "Employee",
                relationshipDetails = "Direct report",
                personalPrompt = "Be encouraging",
                languagePreference = "en"
            )
        )

        assertEquals("John Doe", request.name)
        assertEquals("john@example.com", request.email)
        assertEquals(5L, request.groupId)
        assertEquals("Manager", request.context?.senderRole)
        assertEquals("Employee", request.context?.recipientRole)
        assertEquals("Direct report", request.context?.relationshipDetails)
        assertEquals("Be encouraging", request.context?.personalPrompt)
        assertEquals("en", request.context?.languagePreference)
    }

    @Test
    fun test_addContactRequest_minimalFields() {
        val request = AddContactRequest(
            name = "Jane Doe",
            email = "jane@example.com"
        )

        assertEquals("Jane Doe", request.name)
        assertEquals("jane@example.com", request.email)
        assertNull(request.groupId)
        assertNull(request.context)
    }

    @Test
    fun test_addContactRequest_withGroupId() {
        val request = AddContactRequest(
            name = "Alice",
            email = "alice@example.com",
            groupId = 10L
        )

        assertEquals(10L, request.groupId)
    }

    @Test
    fun test_addContactRequest_withContextFields() {
        val request = AddContactRequest(
            name = "Bob",
            email = "bob@example.com",
            context = AddContactRequestContext(
                senderRole = "Colleague",
                recipientRole = "Colleague",
                relationshipDetails = "Team member",
                personalPrompt = "Be friendly",
                languagePreference = "ko"
            )
        )

        assertEquals("Colleague", request.context?.senderRole)
        assertEquals("Colleague", request.context?.recipientRole)
        assertEquals("Team member", request.context?.relationshipDetails)
        assertEquals("Be friendly", request.context?.personalPrompt)
        assertEquals("ko", request.context?.languagePreference)
    }

    @Test
    fun test_addContactRequest_equality() {
        val request1 = AddContactRequest(
            name = "Test",
            email = "test@example.com",
            groupId = 1L
        )
        val request2 = AddContactRequest(
            name = "Test",
            email = "test@example.com",
            groupId = 1L
        )

        assertEquals(request1, request2)
        assertEquals(request1.hashCode(), request2.hashCode())
    }

    @Test
    fun test_addContactRequest_inequality() {
        val request1 = AddContactRequest(
            name = "Test1",
            email = "test1@example.com"
        )
        val request2 = AddContactRequest(
            name = "Test2",
            email = "test2@example.com"
        )

        assertNotEquals(request1, request2)
    }

    @Test
    fun test_addContactRequest_longPersonalPrompt() {
        val longPrompt = "A".repeat(5000)
        val request = AddContactRequest(
            name = "User",
            email = "user@example.com",
            context = AddContactRequestContext(
                personalPrompt = longPrompt
            )
        )

        assertEquals(5000, request.context?.personalPrompt?.length)
    }

    @Test
    fun test_addContactRequest_emptyOptionalFields() {
        val request = AddContactRequest(
            name = "Test",
            email = "test@example.com",
            context = AddContactRequestContext(
                senderRole = "",
                recipientRole = "",
                relationshipDetails = ""
            )
        )

        assertEquals("", request.context?.senderRole)
        assertEquals("", request.context?.recipientRole)
        assertEquals("", request.context?.relationshipDetails)
    }

    @Test
    fun test_addContactRequest_defaultLanguagePreference() {
        val request = AddContactRequest(
            name = "Test",
            email = "test@example.com",
            context = AddContactRequestContext()
        )

        assertEquals("KOR", request.context?.languagePreference)
    }

    @Test
    fun test_addContactRequest_copy() {
        val original = AddContactRequest(
            name = "Original",
            email = "original@example.com",
            groupId = 1L
        )
        val modified = original.copy(name = "Modified")

        assertEquals("Original", original.name)
        assertEquals("Modified", modified.name)
        assertEquals(original.email, modified.email)
        assertEquals(original.groupId, modified.groupId)
    }

    @Test
    fun test_addContactRequestContext_allDefaults() {
        val context = AddContactRequestContext()

        assertEquals("", context.senderRole)
        assertEquals("", context.recipientRole)
        assertEquals("", context.relationshipDetails)
        assertEquals("", context.personalPrompt)
        assertEquals("KOR", context.languagePreference)
    }

    @Test
    fun test_addContactRequestContext_withOnlyRole() {
        val context = AddContactRequestContext(
            senderRole = "Boss",
            recipientRole = "Employee"
        )

        assertEquals("Boss", context.senderRole)
        assertEquals("Employee", context.recipientRole)
    }

    @Test
    fun test_addContactRequestContext_withRelationshipDetails() {
        val longDetails = "A".repeat(2000)
        val context = AddContactRequestContext(
            relationshipDetails = longDetails
        )

        assertEquals(2000, context.relationshipDetails?.length)
    }
}
