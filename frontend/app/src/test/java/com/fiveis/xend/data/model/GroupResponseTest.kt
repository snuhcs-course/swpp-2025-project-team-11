package com.fiveis.xend.data.model

import org.junit.Assert.*
import org.junit.Test

class GroupResponseTest {

    @Test
    fun create_group_response_with_required_fields_only() {
        val response = GroupResponse(
            id = 1L,
            name = "Work Group"
        )

        assertEquals(1L, response.id)
        assertEquals("Work Group", response.name)
        assertNull(response.description)
        assertTrue(response.options.isEmpty())
        assertNull(response.contacts)
        assertNull(response.createdAt)
        assertNull(response.updatedAt)
    }

    @Test
    fun create_group_response_with_all_fields() {
        val option = PromptOption(
            id = 10L,
            key = "formal",
            name = "Formal",
            prompt = "Use formal language"
        )
        val contact = ContactResponse(
            id = 100L,
            name = "John Doe",
            email = "john@example.com"
        )

        val response = GroupResponse(
            id = 1L,
            name = "Professional Contacts",
            description = "Work-related contacts",
            options = listOf(option),
            contacts = listOf(contact),
            createdAt = "2025-10-30T10:00:00Z",
            updatedAt = "2025-10-30T11:00:00Z"
        )

        assertEquals(1L, response.id)
        assertEquals("Professional Contacts", response.name)
        assertEquals("Work-related contacts", response.description)
        assertEquals(1, response.options.size)
        assertEquals(1, response.contacts?.size)
        assertEquals("2025-10-30T10:00:00Z", response.createdAt)
        assertEquals("2025-10-30T11:00:00Z", response.updatedAt)
    }

    @Test
    fun group_response_to_domain_converts_correctly() {
        val option = PromptOption(
            id = 10L,
            key = "casual",
            name = "Casual",
            prompt = "Use casual tone"
        )
        val contact = ContactResponse(
            id = 100L,
            name = "Alice",
            email = "alice@test.com"
        )

        val response = GroupResponse(
            id = 5L,
            name = "Friends",
            description = "Personal friends",
            options = listOf(option),
            contacts = listOf(contact)
        )

        val domain = response.toDomain()

        assertEquals(5L, domain.id)
        assertEquals("Friends", domain.name)
        assertEquals("Personal friends", domain.description)
        assertEquals(1, domain.options.size)
        assertEquals(1, domain.members.size)
        assertEquals("Alice", domain.members[0].name)
        assertNull(domain.members[0].group) // Circular reference prevented
    }

    @Test
    fun group_response_to_domain_with_null_contacts() {
        val response = GroupResponse(
            id = 1L,
            name = "Empty Group",
            contacts = null
        )

        val domain = response.toDomain()

        assertEquals(1L, domain.id)
        assertEquals("Empty Group", domain.name)
        assertTrue(domain.members.isEmpty())
    }

    @Test
    fun group_response_copy_updates_name() {
        val original = GroupResponse(
            id = 1L,
            name = "Original Name"
        )

        val updated = original.copy(name = "Updated Name")

        assertEquals("Updated Name", updated.name)
        assertEquals(1L, updated.id)
    }

    @Test
    fun group_responses_with_same_values_are_equal() {
        val response1 = GroupResponse(
            id = 1L,
            name = "Test Group"
        )

        val response2 = GroupResponse(
            id = 1L,
            name = "Test Group"
        )

        assertEquals(response1, response2)
        assertEquals(response1.hashCode(), response2.hashCode())
    }

    @Test
    fun group_responses_with_different_ids_are_not_equal() {
        val response1 = GroupResponse(
            id = 1L,
            name = "Group"
        )

        val response2 = GroupResponse(
            id = 2L,
            name = "Group"
        )

        assertNotEquals(response1, response2)
    }

    @Test
    fun group_response_with_multiple_options_and_contacts() {
        val option1 = PromptOption(id = 1L, key = "key1", name = "Option 1", prompt = "Prompt 1")
        val option2 = PromptOption(id = 2L, key = "key2", name = "Option 2", prompt = "Prompt 2")
        val contact1 = ContactResponse(id = 10L, name = "Contact 1", email = "c1@test.com")
        val contact2 = ContactResponse(id = 20L, name = "Contact 2", email = "c2@test.com")

        val response = GroupResponse(
            id = 1L,
            name = "Large Group",
            options = listOf(option1, option2),
            contacts = listOf(contact1, contact2)
        )

        assertEquals(2, response.options.size)
        assertEquals(2, response.contacts?.size)
    }

    @Test
    fun group_response_to_string_contains_name() {
        val response = GroupResponse(
            id = 1L,
            name = "My Test Group"
        )

        val toString = response.toString()
        assertTrue(toString.contains("My Test Group"))
    }
}
