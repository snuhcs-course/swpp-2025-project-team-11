package com.fiveis.xend.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupExtendedTest {

    private fun createContact(id: Long) = Contact(
        id = id,
        name = "Contact $id",
        email = "contact$id@example.com",
        group = null,
        context = null
    )

    private fun createPromptOption(id: Long) = PromptOption(
        id = id,
        key = "key$id",
        name = "Option $id",
        prompt = "Prompt $id"
    )

    @Test
    fun test_group_complete() {
        val members = listOf(createContact(1), createContact(2))
        val options = listOf(createPromptOption(1), createPromptOption(2))
        val group = Group(
            id = 1L,
            name = "Work Team",
            description = "Team for work projects",
            emoji = "💼",
            members = members,
            options = options,
            createdAt = "2025-01-01",
            updatedAt = "2025-01-02"
        )

        assertEquals(1L, group.id)
        assertEquals("Work Team", group.name)
        assertEquals("Team for work projects", group.description)
        assertEquals("💼", group.emoji)
        assertEquals(2, group.members.size)
        assertEquals(2, group.options.size)
        assertEquals("2025-01-01", group.createdAt)
        assertEquals("2025-01-02", group.updatedAt)
    }

    @Test
    fun test_group_emptyMembersAndOptions() {
        val group = Group(
            id = 1L,
            name = "Empty Group",
            description = null,
            emoji = "📦",
            members = emptyList(),
            options = emptyList()
        )

        assertEquals(0, group.members.size)
        assertEquals(0, group.options.size)
        assertNull(group.description)
    }

    @Test
    fun test_group_manyMembers() {
        val members = (1..50).map { createContact(it.toLong()) }
        val group = Group(
            id = 1L,
            name = "Large Group",
            description = "Many members",
            emoji = "🌍",
            members = members,
            options = emptyList()
        )

        assertEquals(50, group.members.size)
    }

    @Test
    fun test_group_manyOptions() {
        val options = (1..20).map { createPromptOption(it.toLong()) }
        val group = Group(
            id = 1L,
            name = "Group with Options",
            description = null,
            emoji = "⚙️",
            members = emptyList(),
            options = options
        )

        assertEquals(20, group.options.size)
    }

    @Test
    fun test_group_specialCharacters() {
        val group = Group(
            id = 1L,
            name = "한글 그룹 & Special 💌",
            description = "特殊文字 groupe",
            emoji = "🎉",
            members = emptyList(),
            options = emptyList()
        )

        assertEquals("한글 그룹 & Special 💌", group.name)
        assertEquals("特殊文字 groupe", group.description)
    }

    @Test
    fun test_group_variousEmojis() {
        val emojis = listOf("😀", "🚀", "⭐", "🌈", "🔥", "💡", "🎯")
        emojis.forEach { emoji ->
            val group = Group(
                id = 1L,
                name = "Group",
                description = null,
                emoji = emoji,
                members = emptyList(),
                options = emptyList()
            )
            assertEquals(emoji, group.emoji)
        }
    }

    @Test
    fun test_group_equality() {
        val group1 = Group(
            1L, "Team", "Desc", "💼",
            emptyList(), emptyList(), "2025-01-01", null
        )
        val group2 = Group(
            1L, "Team", "Desc", "💼",
            emptyList(), emptyList(), "2025-01-01", null
        )

        assertEquals(group1, group2)
        assertEquals(group1.hashCode(), group2.hashCode())
    }

    @Test
    fun test_group_inequality_differentId() {
        val group1 = Group(1L, "Team", null, "💼", emptyList(), emptyList())
        val group2 = Group(2L, "Team", null, "💼", emptyList(), emptyList())

        assertNotEquals(group1, group2)
    }

    @Test
    fun test_group_copy() {
        val original = Group(
            1L, "Original", "Desc", "💼",
            emptyList(), emptyList(), "2025-01-01", null
        )
        val modified = original.copy(name = "Modified", emoji = "🌟")

        assertEquals("Original", original.name)
        assertEquals("💼", original.emoji)
        assertEquals("Modified", modified.name)
        assertEquals("🌟", modified.emoji)
    }

    @Test
    fun test_group_longDescription() {
        val longDesc = "This is a very long description. ".repeat(50)
        val group = Group(
            1L, "Group", longDesc, "📝",
            emptyList(), emptyList()
        )

        assertTrue(group.description!!.length > 1000)
    }

    @Test
    fun test_group_nullDescription() {
        val group = Group(
            1L, "Group", null, "👥",
            emptyList(), emptyList()
        )

        assertNull(group.description)
    }

    @Test
    fun test_group_nullTimestamps() {
        val group = Group(
            1L, "Group", null, "👥",
            emptyList(), emptyList(),
            createdAt = null, updatedAt = null
        )

        assertNull(group.createdAt)
        assertNull(group.updatedAt)
    }

    @Test
    fun test_group_withMembersNoOptions() {
        val members = listOf(createContact(1), createContact(2))
        val group = Group(
            1L, "Team", null, "👥",
            emptyList(), members
        )

        assertEquals(2, group.members.size)
        assertEquals(0, group.options.size)
    }

    @Test
    fun test_group_withOptionsNoMembers() {
        val options = listOf(createPromptOption(1), createPromptOption(2))
        val group = Group(
            1L, "Team", null, "⚙️",
            options, emptyList()
        )

        assertEquals(0, group.members.size)
        assertEquals(2, group.options.size)
    }
}
