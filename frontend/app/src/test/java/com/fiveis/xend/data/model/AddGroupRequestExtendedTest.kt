package com.fiveis.xend.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddGroupRequestExtendedTest {

    @Test
    fun test_addGroupRequest_basic() {
        val request = AddGroupRequest(
            name = "Work Team",
            description = "Team for work projects",
            emoji = "💼",
            optionIds = listOf(10L, 20L)
        )

        assertEquals("Work Team", request.name)
        assertEquals("Team for work projects", request.description)
        assertEquals("💼", request.emoji)
        assertEquals(2, request.optionIds.size)
    }

    @Test
    fun test_addGroupRequest_emptyLists() {
        val request = AddGroupRequest(
            name = "Empty Group",
            description = "No members",
            emoji = "📦",
            optionIds = emptyList()
        )

        assertEquals(0, request.optionIds.size)
    }

    @Test
    fun test_addGroupRequest_nullDescription() {
        val request = AddGroupRequest(
            name = "Group",
            description = "Test",
            emoji = null,
            optionIds = listOf()
        )

        assertNull(request.emoji)
    }

    @Test
    fun test_addGroupRequest_manyPromptOptions() {
        val optionIds = (1L..20L).toList()
        val request = AddGroupRequest(
            name = "Group",
            description = "Options",
            emoji = "⚙️",
            optionIds = optionIds
        )

        assertEquals(20, request.optionIds.size)
    }

    @Test
    fun test_addGroupRequest_specialCharactersInName() {
        val request = AddGroupRequest(
            name = "한글 그룹 & Special 💌",
            description = "特殊文字 groupe",
            emoji = "🎉",
            optionIds = listOf(1L)
        )

        assertEquals("한글 그룹 & Special 💌", request.name)
        assertEquals("特殊文字 groupe", request.description)
    }

    @Test
    fun test_addGroupRequest_variousEmojis() {
        val emojis = listOf("😀", "🚀", "⭐", "🌈", "🔥", "💡", "🎯", "🏆")
        val requests = emojis.map { emoji ->
            AddGroupRequest(
                name = "Group",
                description = "Desc",
                emoji = emoji,
                optionIds = emptyList()
            )
        }

        requests.forEachIndexed { index, request ->
            assertEquals(emojis[index], request.emoji)
        }
    }

    @Test
    fun test_addGroupRequest_longDescription() {
        val longDescription = "A".repeat(5000)
        val request = AddGroupRequest(
            name = "Group",
            description = longDescription,
            emoji = "📝",
            optionIds = emptyList()
        )

        assertEquals(5000, request.description.length)
    }

    @Test
    fun test_addGroupRequest_equality() {
        val request1 = AddGroupRequest(
            name = "Team",
            description = "Desc",
            emoji = "💼",
            optionIds = listOf(1L)
        )
        val request2 = AddGroupRequest(
            name = "Team",
            description = "Desc",
            emoji = "💼",
            optionIds = listOf(1L)
        )

        assertEquals(request1, request2)
        assertEquals(request1.hashCode(), request2.hashCode())
    }

    @Test
    fun test_addGroupRequest_inequality_differentName() {
        val request1 = AddGroupRequest("Team1", "Desc", "💼", emptyList())
        val request2 = AddGroupRequest("Team2", "Desc", "💼", emptyList())

        assertNotEquals(request1, request2)
    }

    @Test
    fun test_addGroupRequest_copy() {
        val original = AddGroupRequest(
            name = "Original",
            description = "Desc",
            emoji = "💼",
            optionIds = listOf(1L)
        )
        val modified = original.copy(name = "Modified", emoji = "🌟")

        assertEquals("Original", original.name)
        assertEquals("💼", original.emoji)
        assertEquals("Modified", modified.name)
        assertEquals("🌟", modified.emoji)
    }

    @Test
    fun test_addGroupRequest_duplicatePromptOptionIds() {
        val request = AddGroupRequest(
            name = "Group",
            description = "Desc",
            emoji = "⚙️",
            optionIds = listOf(1L, 1L, 2L)
        )

        assertEquals(3, request.optionIds.size)
    }

    @Test
    fun test_addGroupRequest_defaultEmptyOptions() {
        val request = AddGroupRequest(
            name = "Team",
            description = "Desc"
        )

        assertEquals(0, request.optionIds.size)
    }

    @Test
    fun test_addGroupRequest_nullEmoji() {
        val request = AddGroupRequest(
            name = "Team",
            description = "Desc",
            emoji = null
        )

        assertNull(request.emoji)
    }
}
