package com.fiveis.xend.data.model

import org.junit.Assert.*
import org.junit.Test

class ContactResponseTest {

    @Test
    fun create_contact_response_with_required_fields_only() {
        val response = ContactResponse(
            id = 1L,
            name = "John Doe",
            email = "john@example.com"
        )

        assertEquals(1L, response.id)
        assertEquals("John Doe", response.name)
        assertEquals("john@example.com", response.email)
        assertNull(response.group)
        assertNull(response.context)
        assertNull(response.createdAt)
        assertNull(response.updatedAt)
    }

    @Test
    fun create_contact_response_with_all_fields() {
        val group = GroupResponse(
            id = 10L,
            name = "Work",
            description = "Work contacts"
        )
        val context = ContactResponseContext(
            id = 100L,
            senderRole = "Manager",
            recipientRole = "Employee"
        )

        val response = ContactResponse(
            id = 1L,
            group = group,
            name = "Jane Smith",
            email = "jane@example.com",
            context = context,
            createdAt = "2025-10-30T10:00:00Z",
            updatedAt = "2025-10-30T11:00:00Z"
        )

        assertEquals(1L, response.id)
        assertEquals("Jane Smith", response.name)
        assertEquals("jane@example.com", response.email)
        assertNotNull(response.group)
        assertEquals(10L, response.group?.id)
        assertNotNull(response.context)
        assertEquals(100L, response.context?.id)
        assertEquals("2025-10-30T10:00:00Z", response.createdAt)
        assertEquals("2025-10-30T11:00:00Z", response.updatedAt)
    }

    @Test
    fun contact_response_to_domain_converts_correctly() {
        val context = ContactResponseContext(
            id = 100L,
            senderRole = "CEO",
            recipientRole = "Director"
        )

        val response = ContactResponse(
            id = 5L,
            name = "Alice",
            email = "alice@test.com",
            context = context
        )

        val domain = response.toDomain()

        assertEquals(5L, domain.id)
        assertEquals("Alice", domain.name)
        assertEquals("alice@test.com", domain.email)
        assertNotNull(domain.context)
        assertEquals(100L, domain.context?.id)
        assertEquals("CEO", domain.context?.senderRole)
    }

    @Test
    fun contact_response_copy_updates_name() {
        val original = ContactResponse(
            id = 1L,
            name = "Original Name",
            email = "email@test.com"
        )

        val updated = original.copy(name = "Updated Name")

        assertEquals("Updated Name", updated.name)
        assertEquals("email@test.com", updated.email)
    }

    @Test
    fun contact_responses_with_same_values_are_equal() {
        val response1 = ContactResponse(
            id = 1L,
            name = "Test",
            email = "test@example.com"
        )

        val response2 = ContactResponse(
            id = 1L,
            name = "Test",
            email = "test@example.com"
        )

        assertEquals(response1, response2)
        assertEquals(response1.hashCode(), response2.hashCode())
    }
}

class ContactResponseContextTest {

    @Test
    fun create_contact_response_context_with_id_only() {
        val context = ContactResponseContext(id = 1L)

        assertEquals(1L, context.id)
        assertNull(context.senderRole)
        assertNull(context.recipientRole)
        assertNull(context.relationshipDetails)
        assertNull(context.personalPrompt)
        assertNull(context.languagePreference)
        assertNull(context.createdAt)
        assertNull(context.updatedAt)
    }

    @Test
    fun create_contact_response_context_with_all_fields() {
        val context = ContactResponseContext(
            id = 1L,
            senderRole = "Manager",
            recipientRole = "Employee",
            relationshipDetails = "Direct report",
            personalPrompt = "Use formal tone",
            languagePreference = "ENG",
            createdAt = "2025-10-30T10:00:00Z",
            updatedAt = "2025-10-30T11:00:00Z"
        )

        assertEquals(1L, context.id)
        assertEquals("Manager", context.senderRole)
        assertEquals("Employee", context.recipientRole)
        assertEquals("Direct report", context.relationshipDetails)
        assertEquals("Use formal tone", context.personalPrompt)
        assertEquals("ENG", context.languagePreference)
        assertEquals("2025-10-30T10:00:00Z", context.createdAt)
        assertEquals("2025-10-30T11:00:00Z", context.updatedAt)
    }

    @Test
    fun contact_response_context_to_domain_converts_correctly() {
        val context = ContactResponseContext(
            id = 5L,
            senderRole = "Team Lead",
            recipientRole = "Team Member",
            languagePreference = "KOR"
        )

        val domain = context.toDomain()

        assertEquals(5L, domain.id)
        assertEquals("Team Lead", domain.senderRole)
        assertEquals("Team Member", domain.recipientRole)
        assertEquals("KOR", domain.languagePreference)
    }

    @Test
    fun contact_response_context_copy_updates_sender_role() {
        val original = ContactResponseContext(
            id = 1L,
            senderRole = "Junior"
        )

        val updated = original.copy(senderRole = "Senior")

        assertEquals("Senior", updated.senderRole)
        assertEquals(1L, updated.id)
    }

    @Test
    fun contact_response_contexts_with_same_values_are_equal() {
        val context1 = ContactResponseContext(
            id = 1L,
            senderRole = "Role"
        )

        val context2 = ContactResponseContext(
            id = 1L,
            senderRole = "Role"
        )

        assertEquals(context1, context2)
        assertEquals(context1.hashCode(), context2.hashCode())
    }
}
