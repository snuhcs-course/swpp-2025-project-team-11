package com.fiveis.xend.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LogoutResponseTest {

    @Test
    fun test_construction_setsDetail() {
        val response = LogoutResponse(detail = "Logout successful")

        assertEquals("Logout successful", response.detail)
    }

    @Test
    fun test_copy_changesDetail() {
        val original = LogoutResponse("Original detail")
        val modified = original.copy(detail = "Modified detail")

        assertEquals("Modified detail", modified.detail)
    }

    @Test
    fun test_equals_sameValues_returnsTrue() {
        val response1 = LogoutResponse("Success")
        val response2 = LogoutResponse("Success")

        assertEquals(response1, response2)
    }
}
