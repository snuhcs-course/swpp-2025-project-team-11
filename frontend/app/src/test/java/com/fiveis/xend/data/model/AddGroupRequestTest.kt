package com.fiveis.xend.data.model

import org.junit.Assert.*
import org.junit.Test

class AddGroupRequestTest {

    @Test
    fun create_add_group_request_with_required_fields_only() {
        val request = AddGroupRequest(
            name = "Work Contacts",
            description = "My work colleagues"
        )

        assertEquals("Work Contacts", request.name)
        assertEquals("My work colleagues", request.description)
        assertTrue(request.optionIds.isEmpty())
    }

    @Test
    fun create_add_group_request_with_option_ids() {
        val request = AddGroupRequest(
            name = "Friends",
            description = "Personal friends",
            optionIds = listOf(1L, 2L, 3L)
        )

        assertEquals("Friends", request.name)
        assertEquals("Personal friends", request.description)
        assertEquals(3, request.optionIds.size)
        assertEquals(1L, request.optionIds[0])
        assertEquals(2L, request.optionIds[1])
        assertEquals(3L, request.optionIds[2])
    }

    @Test
    fun create_add_group_request_with_empty_option_ids() {
        val request = AddGroupRequest(
            name = "Empty Group",
            description = "No options",
            optionIds = emptyList()
        )

        assertTrue(request.optionIds.isEmpty())
    }

    @Test
    fun add_group_request_copy_updates_name() {
        val original = AddGroupRequest(
            name = "Original Name",
            description = "Description",
            optionIds = listOf(1L)
        )

        val updated = original.copy(name = "Updated Name")

        assertEquals("Updated Name", updated.name)
        assertEquals("Description", updated.description)
        assertEquals(listOf(1L), updated.optionIds)
    }

    @Test
    fun add_group_requests_with_same_values_are_equal() {
        val request1 = AddGroupRequest(
            name = "Group A",
            description = "Description A",
            optionIds = listOf(1L, 2L)
        )

        val request2 = AddGroupRequest(
            name = "Group A",
            description = "Description A",
            optionIds = listOf(1L, 2L)
        )

        assertEquals(request1, request2)
        assertEquals(request1.hashCode(), request2.hashCode())
    }

    @Test
    fun add_group_requests_with_different_names_are_not_equal() {
        val request1 = AddGroupRequest(
            name = "Group A",
            description = "Description"
        )

        val request2 = AddGroupRequest(
            name = "Group B",
            description = "Description"
        )

        assertNotEquals(request1, request2)
    }

    @Test
    fun add_group_request_to_string_contains_fields() {
        val request = AddGroupRequest(
            name = "Test Group",
            description = "Test Description",
            optionIds = listOf(1L)
        )

        val toString = request.toString()

        assertTrue(toString.contains("Test Group"))
        assertTrue(toString.contains("Test Description"))
    }
}
