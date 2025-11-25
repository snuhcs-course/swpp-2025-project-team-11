package com.fiveis.xend.data.database.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GroupEntityTest {

    @Test
    fun test_construction_withAllFields() {
        val group = GroupEntity(
            id = 1L,
            name = "Team A",
            description = "Engineering team",
            emoji = "🚀",
            createdAt = "2024-01-01",
            updatedAt = "2024-01-02"
        )

        assertEquals(1L, group.id)
        assertEquals("Team A", group.name)
        assertEquals("Engineering team", group.description)
        assertEquals("🚀", group.emoji)
        assertEquals("2024-01-01", group.createdAt)
        assertEquals("2024-01-02", group.updatedAt)
    }

    @Test
    fun test_construction_withNullFields() {
        val group = GroupEntity(
            id = 1L,
            name = "Team B",
            description = null,
            emoji = null,
            createdAt = null,
            updatedAt = null
        )

        assertEquals("Team B", group.name)
        assertNull(group.description)
        assertNull(group.emoji)
        assertNull(group.createdAt)
        assertNull(group.updatedAt)
    }

    @Test
    fun test_construction_withDefaultValues() {
        val group = GroupEntity(id = 1L, name = "Group")

        assertNull(group.description)
        assertNull(group.emoji)
    }

    @Test
    fun test_copy_changesName() {
        val original = GroupEntity(1L, "Original", "Description", "😀")
        val modified = original.copy(name = "Updated")

        assertEquals("Updated", modified.name)
        assertEquals("Description", modified.description)
        assertEquals("😀", modified.emoji)
    }

    @Test
    fun test_copy_changesEmoji() {
        val original = GroupEntity(1L, "Group", emoji = "🎉")
        val modified = original.copy(emoji = "🎊")

        assertEquals("🎊", modified.emoji)
        assertEquals("Group", modified.name)
    }

    @Test
    fun test_equals_sameValues_returnsTrue() {
        val group1 = GroupEntity(1L, "Team", "Desc", "🔥", "2024-01-01", "2024-01-02")
        val group2 = GroupEntity(1L, "Team", "Desc", "🔥", "2024-01-01", "2024-01-02")

        assertEquals(group1, group2)
    }

    @Test
    fun test_multipleEmojiTypes() {
        val group1 = GroupEntity(1L, "Team1", emoji = "👍")
        val group2 = GroupEntity(2L, "Team2", emoji = "❤️")
        val group3 = GroupEntity(3L, "Team3", emoji = "🌟")

        assertEquals("👍", group1.emoji)
        assertEquals("❤️", group2.emoji)
        assertEquals("🌟", group3.emoji)
    }
}
