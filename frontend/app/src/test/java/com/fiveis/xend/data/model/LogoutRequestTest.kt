package com.fiveis.xend.data.model

import org.junit.Assert.*
import org.junit.Test

class LogoutRequestTest {

    @Test
    fun create_logout_request_with_refresh_token() {
        val request = LogoutRequest(refresh = "refresh-token-123")

        assertEquals("refresh-token-123", request.refresh)
    }

    @Test
    fun logout_request_copy_updates_refresh_token() {
        val original = LogoutRequest(refresh = "original-token")
        val updated = original.copy(refresh = "updated-token")

        assertEquals("updated-token", updated.refresh)
    }

    @Test
    fun logout_requests_with_same_values_are_equal() {
        val request1 = LogoutRequest(refresh = "same-token")
        val request2 = LogoutRequest(refresh = "same-token")

        assertEquals(request1, request2)
        assertEquals(request1.hashCode(), request2.hashCode())
    }

    @Test
    fun logout_requests_with_different_values_are_not_equal() {
        val request1 = LogoutRequest(refresh = "token-1")
        val request2 = LogoutRequest(refresh = "token-2")

        assertNotEquals(request1, request2)
    }

    @Test
    fun logout_request_to_string_contains_refresh_token() {
        val request = LogoutRequest(refresh = "my-refresh-token")
        val toString = request.toString()

        assertTrue(toString.contains("my-refresh-token"))
    }
}
