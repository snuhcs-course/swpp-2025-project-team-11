package com.fiveis.xend.data.model

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class GroupTest {

    @Test
    fun create_group_with_required_fields_only() {
        val group = Group(
            id = 1L,
            name = "Work Contacts"
        )

        assertEquals(1L, group.id)
        assertEquals("Work Contacts", group.name)
        assertNull(group.description)
        assertTrue(group.options.isEmpty())
        assertTrue(group.members.isEmpty())
        assertNull(group.createdAt)
        assertNull(group.updatedAt)
    }

    @Test
    fun create_group_with_all_fields() {
        val option = PromptOption(
            id = 10L,
            key = "professional",
            name = "Professional",
            prompt = "Use professional language"
        )

        val contact = Contact(
            id = 100L,
            name = "John Doe",
            email = "john@example.com"
        )

        val group = Group(
            id = 1L,
            name = "Work Contacts",
            description = "Contacts from work",
            options = listOf(option),
            members = listOf(contact),
            createdAt = "2024-10-30T10:00:00Z",
            updatedAt = "2024-10-30T12:00:00Z"
        )

        assertEquals(1L, group.id)
        assertEquals("Work Contacts", group.name)
        assertEquals("Contacts from work", group.description)
        assertEquals(1, group.options.size)
        assertEquals(option, group.options[0])
        assertEquals(1, group.members.size)
        assertEquals(contact, group.members[0])
        assertEquals("2024-10-30T10:00:00Z", group.createdAt)
        assertEquals("2024-10-30T12:00:00Z", group.updatedAt)
    }

    @Test
    fun group_copy_updates_specific_fields() {
        val original = Group(
            id = 1L,
            name = "Original Group"
        )

        val updated = original.copy(
            name = "Updated Group",
            description = "New description"
        )

        assertEquals(1L, updated.id)
        assertEquals("Updated Group", updated.name)
        assertEquals("New description", updated.description)
    }

    @Test
    fun groups_with_same_values_are_equal() {
        val group1 = Group(
            id = 1L,
            name = "Test Group",
            description = "Test description"
        )

        val group2 = Group(
            id = 1L,
            name = "Test Group",
            description = "Test description"
        )

        assertEquals(group1, group2)
        assertEquals(group1.hashCode(), group2.hashCode())
    }

    @Test
    fun groups_with_different_ids_are_not_equal() {
        val group1 = Group(
            id = 1L,
            name = "Test Group"
        )

        val group2 = group1.copy(id = 2L)

        assertNotEquals(group1, group2)
    }

    @Test
    fun group_with_multiple_members() {
        val contact1 = Contact(id = 1L, name = "Alice", email = "alice@test.com")
        val contact2 = Contact(id = 2L, name = "Bob", email = "bob@test.com")
        val contact3 = Contact(id = 3L, name = "Charlie", email = "charlie@test.com")

        val group = Group(
            id = 1L,
            name = "Team",
            members = listOf(contact1, contact2, contact3)
        )

        assertEquals(3, group.members.size)
        assertTrue(group.members.contains(contact1))
        assertTrue(group.members.contains(contact2))
        assertTrue(group.members.contains(contact3))
    }

    @Test
    fun group_with_multiple_options() {
        val option1 = PromptOption(id = 1L, key = "formal", name = "Formal", prompt = "Be formal")
        val option2 = PromptOption(id = 2L, key = "brief", name = "Brief", prompt = "Be brief")
        val option3 = PromptOption(id = 3L, key = "detailed", name = "Detailed", prompt = "Include details")

        val group = Group(
            id = 1L,
            name = "Business Contacts",
            options = listOf(option1, option2, option3)
        )

        assertEquals(3, group.options.size)
        assertEquals(option1, group.options[0])
        assertEquals(option2, group.options[1])
        assertEquals(option3, group.options[2])
    }
}
