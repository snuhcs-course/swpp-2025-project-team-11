package com.fiveis.xend.data.model

import org.junit.Assert.*
import org.junit.Test

class TokenRefreshRequestTest {

    @Test
    fun create_token_refresh_request_with_refresh_token() {
        val request = TokenRefreshRequest(refreshToken = "refresh-token-abc")

        assertEquals("refresh-token-abc", request.refreshToken)
    }

    @Test
    fun token_refresh_request_copy_updates_refresh_token() {
        val original = TokenRefreshRequest(refreshToken = "original")
        val updated = original.copy(refreshToken = "updated")

        assertEquals("updated", updated.refreshToken)
    }

    @Test
    fun token_refresh_requests_with_same_values_are_equal() {
        val request1 = TokenRefreshRequest(refreshToken = "same")
        val request2 = TokenRefreshRequest(refreshToken = "same")

        assertEquals(request1, request2)
        assertEquals(request1.hashCode(), request2.hashCode())
    }

    @Test
    fun token_refresh_requests_with_different_values_are_not_equal() {
        val request1 = TokenRefreshRequest(refreshToken = "token1")
        val request2 = TokenRefreshRequest(refreshToken = "token2")

        assertNotEquals(request1, request2)
    }

    @Test
    fun token_refresh_request_to_string_contains_refresh_token() {
        val request = TokenRefreshRequest(refreshToken = "my-token")
        val toString = request.toString()

        assertTrue(toString.contains("my-token"))
    }
}
