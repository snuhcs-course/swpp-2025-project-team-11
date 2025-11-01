package com.fiveis.xend.network

import org.junit.Assert.*
import org.junit.Test

class ReplyOptionInfoTest {

    @Test
    fun create_reply_option_info_with_all_fields() {
        val option = ReplyOptionInfo(
            id = 1,
            type = "accept",
            title = "Accept the invitation"
        )

        assertEquals(1, option.id)
        assertEquals("accept", option.type)
        assertEquals("Accept the invitation", option.title)
    }

    @Test
    fun create_reply_option_info_with_decline_type() {
        val option = ReplyOptionInfo(
            id = 2,
            type = "decline",
            title = "Decline the invitation"
        )

        assertEquals(2, option.id)
        assertEquals("decline", option.type)
    }

    @Test
    fun reply_option_info_copy_updates_title() {
        val original = ReplyOptionInfo(
            id = 1,
            type = "suggest",
            title = "Original title"
        )

        val updated = original.copy(title = "Updated title")

        assertEquals("Updated title", updated.title)
        assertEquals(1, updated.id)
        assertEquals("suggest", updated.type)
    }

    @Test
    fun reply_option_infos_with_same_values_are_equal() {
        val option1 = ReplyOptionInfo(
            id = 1,
            type = "accept",
            title = "Accept"
        )

        val option2 = ReplyOptionInfo(
            id = 1,
            type = "accept",
            title = "Accept"
        )

        assertEquals(option1, option2)
        assertEquals(option1.hashCode(), option2.hashCode())
    }

    @Test
    fun reply_option_infos_with_different_ids_are_not_equal() {
        val option1 = ReplyOptionInfo(
            id = 1,
            type = "accept",
            title = "Accept"
        )

        val option2 = ReplyOptionInfo(
            id = 2,
            type = "accept",
            title = "Accept"
        )

        assertNotEquals(option1, option2)
    }

    @Test
    fun reply_option_info_to_string_contains_fields() {
        val option = ReplyOptionInfo(
            id = 5,
            type = "custom",
            title = "Custom response"
        )

        val toString = option.toString()

        assertTrue(toString.contains("5") || toString.contains("id"))
        assertTrue(toString.contains("custom"))
        assertTrue(toString.contains("Custom response"))
    }

    @Test
    fun reply_option_info_with_various_types() {
        val acceptOption = ReplyOptionInfo(id = 1, type = "accept", title = "Accept")
        val declineOption = ReplyOptionInfo(id = 2, type = "decline", title = "Decline")
        val suggestOption = ReplyOptionInfo(id = 3, type = "suggest", title = "Suggest alternative")

        assertEquals("accept", acceptOption.type)
        assertEquals("decline", declineOption.type)
        assertEquals("suggest", suggestOption.type)
    }
}
