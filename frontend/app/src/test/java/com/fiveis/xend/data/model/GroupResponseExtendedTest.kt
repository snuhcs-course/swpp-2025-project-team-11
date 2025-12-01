package com.fiveis.xend.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GroupResponseExtendedTest {

    @Test
    fun test_groupResponse_basic() {
        val response = GroupResponse(
            id = 1L,
            name = "Team Alpha",
            description = "Main team"
        )

        assertEquals(1L, response.id)
        assertEquals("Team Alpha", response.name)
        assertEquals("Main team", response.description)
        assertNull(response.emoji)
        assertEquals(0, response.options.size)
        assertNull(response.contacts)
    }

    @Test
    fun test_groupResponse_withEmoji() {
        val response = GroupResponse(
            id = 1L,
            name = "Team",
            description = "Desc",
            emoji = "🚀"
        )

        assertEquals("🚀", response.emoji)
    }

    @Test
    fun test_groupResponse_withOptions() {
        val options = listOf(
            PromptOption(id = 1L, key = "key1", name = "Option1", prompt = "Prompt1"),
            PromptOption(id = 2L, key = "key2", name = "Option2", prompt = "Prompt2")
        )
        val response = GroupResponse(
            id = 1L,
            name = "Team",
            description = "Desc",
            options = options
        )

        assertEquals(2, response.options.size)
        assertEquals("Option1", response.options[0].name)
    }

    @Test
    fun test_groupResponse_withContacts() {
        val contacts = listOf(
            ContactResponse(id = 1L, name = "John", email = "john@example.com"),
            ContactResponse(id = 2L, name = "Jane", email = "jane@example.com")
        )
        val response = GroupResponse(
            id = 1L,
            name = "Team",
            description = "Desc",
            contacts = contacts
        )

        assertNotNull(response.contacts)
        assertEquals(2, response.contacts?.size)
        assertEquals("John", response.contacts?.get(0)?.name)
    }

    @Test
    fun test_groupResponse_toDomain() {
        val response = GroupResponse(
            id = 1L,
            name = "Team",
            description = "Description",
            emoji = "💼"
        )

        val domain = response.toDomain()

        assertEquals(1L, domain.id)
        assertEquals("Team", domain.name)
        assertEquals("Description", domain.description)
        assertEquals("💼", domain.emoji)
        assertEquals(0, domain.members.size)
    }

    @Test
    fun test_groupResponse_toDomain_withContacts() {
        val contacts = listOf(
            ContactResponse(id = 1L, name = "Member1", email = "m1@example.com"),
            ContactResponse(id = 2L, name = "Member2", email = "m2@example.com")
        )
        val response = GroupResponse(
            id = 1L,
            name = "Team",
            description = "Desc",
            contacts = contacts
        )

        val domain = response.toDomain()

        assertEquals(2, domain.members.size)
        assertEquals("Member1", domain.members[0].name)
        assertNull(domain.members[0].group) // circular reference prevention
    }

    @Test
    fun test_groupResponse_toDomain_withOptions() {
        val options = listOf(
            PromptOption(id = 1L, key = "key", name = "Option", prompt = "Content")
        )
        val response = GroupResponse(
            id = 1L,
            name = "Team",
            description = "Desc",
            options = options
        )

        val domain = response.toDomain()

        assertEquals(1, domain.options.size)
        assertEquals("Option", domain.options[0].name)
    }
}
