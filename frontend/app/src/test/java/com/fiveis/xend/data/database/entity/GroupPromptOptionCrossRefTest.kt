package com.fiveis.xend.data.database.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class GroupPromptOptionCrossRefTest {

    @Test
    fun test_createCrossRef_success() {
        val crossRef = GroupPromptOptionCrossRef(
            groupId = 1L,
            optionId = 5L
        )

        assertEquals(1L, crossRef.groupId)
        assertEquals(5L, crossRef.optionId)
    }

    @Test
    fun test_equality_sameValues() {
        val ref1 = GroupPromptOptionCrossRef(groupId = 1L, optionId = 5L)
        val ref2 = GroupPromptOptionCrossRef(groupId = 1L, optionId = 5L)

        assertEquals(ref1, ref2)
        assertEquals(ref1.hashCode(), ref2.hashCode())
    }

    @Test
    fun test_equality_differentGroupId() {
        val ref1 = GroupPromptOptionCrossRef(groupId = 1L, optionId = 5L)
        val ref2 = GroupPromptOptionCrossRef(groupId = 2L, optionId = 5L)

        assertNotEquals(ref1, ref2)
    }

    @Test
    fun test_equality_differentOptionId() {
        val ref1 = GroupPromptOptionCrossRef(groupId = 1L, optionId = 5L)
        val ref2 = GroupPromptOptionCrossRef(groupId = 1L, optionId = 6L)

        assertNotEquals(ref1, ref2)
    }

    @Test
    fun test_copy_modifyGroupId() {
        val original = GroupPromptOptionCrossRef(groupId = 1L, optionId = 5L)
        val modified = original.copy(groupId = 10L)

        assertEquals(10L, modified.groupId)
        assertEquals(5L, modified.optionId)
    }

    @Test
    fun test_copy_modifyOptionId() {
        val original = GroupPromptOptionCrossRef(groupId = 1L, optionId = 5L)
        val modified = original.copy(optionId = 50L)

        assertEquals(1L, modified.groupId)
        assertEquals(50L, modified.optionId)
    }

    @Test
    fun test_toString_containsFields() {
        val crossRef = GroupPromptOptionCrossRef(groupId = 1L, optionId = 5L)
        val string = crossRef.toString()

        assert(string.contains("groupId=1"))
        assert(string.contains("optionId=5"))
    }
}
