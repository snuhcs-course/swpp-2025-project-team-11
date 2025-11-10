package com.fiveis.xend.data.model

import org.junit.Assert.*
import org.junit.Test

class AddContactRequestTest {

    @Test
    fun create_add_contact_request_with_required_fields_only() {
        val request = AddContactRequest(
            name = "John Doe",
            email = "john@example.com"
        )

        assertEquals("John Doe", request.name)
        assertEquals("john@example.com", request.email)
        assertNull(request.groupId)
        assertNull(request.context)
    }

    @Test
    fun create_add_contact_request_with_group_id() {
        val request = AddContactRequest(
            name = "Jane Smith",
            email = "jane@example.com",
            groupId = 10L
        )

        assertEquals("Jane Smith", request.name)
        assertEquals("jane@example.com", request.email)
        assertEquals(10L, request.groupId)
        assertNull(request.context)
    }

    @Test
    fun create_add_contact_request_with_context() {
        val context = AddContactRequestContext(
            senderRole = "Manager",
            recipientRole = "Employee",
            relationshipDetails = "Direct report"
        )

        val request = AddContactRequest(
            name = "Bob Johnson",
            email = "bob@example.com",
            context = context
        )

        assertEquals("Bob Johnson", request.name)
        assertEquals("bob@example.com", request.email)
        assertNotNull(request.context)
        assertEquals("Manager", request.context?.senderRole)
        assertEquals("Employee", request.context?.recipientRole)
        assertEquals("Direct report", request.context?.relationshipDetails)
    }

    @Test
    fun create_add_contact_request_with_all_fields() {
        val context = AddContactRequestContext(
            senderRole = "CEO",
            recipientRole = "Director",
            relationshipDetails = "Strategic partner",
            personalPrompt = "Use formal language",
            languagePreference = "ENG"
        )

        val request = AddContactRequest(
            name = "Alice Brown",
            email = "alice@example.com",
            groupId = 5L,
            context = context
        )

        assertEquals("Alice Brown", request.name)
        assertEquals("alice@example.com", request.email)
        assertEquals(5L, request.groupId)
        assertNotNull(request.context)
        assertEquals("CEO", request.context?.senderRole)
        assertEquals("ENG", request.context?.languagePreference)
    }

    @Test
    fun add_contact_request_copy_updates_name() {
        val original = AddContactRequest(
            name = "Original Name",
            email = "original@test.com"
        )

        val updated = original.copy(name = "Updated Name")

        assertEquals("Updated Name", updated.name)
        assertEquals("original@test.com", updated.email)
    }

    @Test
    fun add_contact_requests_with_same_values_are_equal() {
        val request1 = AddContactRequest(
            name = "Test User",
            email = "test@example.com",
            groupId = 1L
        )

        val request2 = AddContactRequest(
            name = "Test User",
            email = "test@example.com",
            groupId = 1L
        )

        assertEquals(request1, request2)
        assertEquals(request1.hashCode(), request2.hashCode())
    }
}

class AddContactRequestContextTest {

    @Test
    fun create_context_with_default_values() {
        val context = AddContactRequestContext()

        assertEquals("", context.senderRole)
        assertEquals("", context.recipientRole)
        assertEquals("", context.relationshipDetails)
        assertEquals("", context.personalPrompt)
        assertEquals("KOR", context.languagePreference)
    }

    @Test
    fun create_context_with_custom_values() {
        val context = AddContactRequestContext(
            senderRole = "Team Lead",
            recipientRole = "Team Member",
            relationshipDetails = "Same department",
            personalPrompt = "Keep it casual",
            languagePreference = "ENG"
        )

        assertEquals("Team Lead", context.senderRole)
        assertEquals("Team Member", context.recipientRole)
        assertEquals("Same department", context.relationshipDetails)
        assertEquals("Keep it casual", context.personalPrompt)
        assertEquals("ENG", context.languagePreference)
    }

    @Test
    fun create_context_with_null_values() {
        val context = AddContactRequestContext(
            senderRole = null,
            recipientRole = null,
            relationshipDetails = null,
            personalPrompt = null,
            languagePreference = null
        )

        assertNull(context.senderRole)
        assertNull(context.recipientRole)
        assertNull(context.relationshipDetails)
        assertNull(context.personalPrompt)
        assertNull(context.languagePreference)
    }

    @Test
    fun context_copy_updates_sender_role() {
        val original = AddContactRequestContext(
            senderRole = "Junior",
            languagePreference = "KOR"
        )

        val updated = original.copy(senderRole = "Senior")

        assertEquals("Senior", updated.senderRole)
        assertEquals("KOR", updated.languagePreference)
    }

    @Test
    fun contexts_with_same_values_are_equal() {
        val context1 = AddContactRequestContext(
            senderRole = "Manager",
            languagePreference = "ENG"
        )

        val context2 = AddContactRequestContext(
            senderRole = "Manager",
            languagePreference = "ENG"
        )

        assertEquals(context1, context2)
        assertEquals(context1.hashCode(), context2.hashCode())
    }

    @Test
    fun default_language_preference_is_korean() {
        val context = AddContactRequestContext()

        assertEquals("KOR", context.languagePreference)
    }

    @Test
    fun default_sender_role_is_empty() {
        val context = AddContactRequestContext()

        assertEquals("", context.senderRole)
    }
}
