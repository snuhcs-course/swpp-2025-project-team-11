package com.fiveis.xend.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateProfileRequestTest {

    @Test
    fun test_construction_withAllFields() {
        val request = UpdateProfileRequest(
            displayName = "New Name",
            info = "New Info"
        )

        assertEquals("New Name", request.displayName)
        assertEquals("New Info", request.info)
    }

    @Test
    fun test_construction_withNullFields() {
        val request = UpdateProfileRequest(
            displayName = null,
            info = null
        )

        assertNull(request.displayName)
        assertNull(request.info)
    }

    @Test
    fun test_construction_withDefaultValues() {
        val request = UpdateProfileRequest()

        assertNull(request.displayName)
        assertNull(request.info)
    }

    @Test
    fun test_copy_changesFields() {
        val original = UpdateProfileRequest("Original", "Original Info")
        val modified = original.copy(displayName = "Updated")

        assertEquals("Updated", modified.displayName)
        assertEquals("Original Info", modified.info)
    }

    @Test
    fun test_equals_sameValues_returnsTrue() {
        val request1 = UpdateProfileRequest("Name", "Info")
        val request2 = UpdateProfileRequest("Name", "Info")

        assertEquals(request1, request2)
    }
}
