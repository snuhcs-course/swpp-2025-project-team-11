package com.fiveis.xend.data.model

import org.junit.Assert.*
import org.junit.Test

class TokenRefreshResponseTest {

    @Test
    fun create_token_refresh_response_with_tokens() {
        val response = TokenRefreshResponse(
            accessToken = "new-access-token",
            refreshToken = "new-refresh-token"
        )

        assertEquals("new-access-token", response.accessToken)
        assertEquals("new-refresh-token", response.refreshToken)
    }

    @Test
    fun token_refresh_response_copy_updates_access_token() {
        val original = TokenRefreshResponse(
            accessToken = "old-access",
            refreshToken = "refresh"
        )
        val updated = original.copy(accessToken = "new-access")

        assertEquals("new-access", updated.accessToken)
        assertEquals("refresh", updated.refreshToken)
    }

    @Test
    fun token_refresh_response_copy_updates_refresh_token() {
        val original = TokenRefreshResponse(
            accessToken = "access",
            refreshToken = "old-refresh"
        )
        val updated = original.copy(refreshToken = "new-refresh")

        assertEquals("access", updated.accessToken)
        assertEquals("new-refresh", updated.refreshToken)
    }

    @Test
    fun token_refresh_responses_with_same_values_are_equal() {
        val response1 = TokenRefreshResponse(
            accessToken = "access",
            refreshToken = "refresh"
        )
        val response2 = TokenRefreshResponse(
            accessToken = "access",
            refreshToken = "refresh"
        )

        assertEquals(response1, response2)
        assertEquals(response1.hashCode(), response2.hashCode())
    }

    @Test
    fun token_refresh_responses_with_different_access_tokens_are_not_equal() {
        val response1 = TokenRefreshResponse(
            accessToken = "access1",
            refreshToken = "refresh"
        )
        val response2 = TokenRefreshResponse(
            accessToken = "access2",
            refreshToken = "refresh"
        )

        assertNotEquals(response1, response2)
    }

    @Test
    fun token_refresh_response_to_string_contains_tokens() {
        val response = TokenRefreshResponse(
            accessToken = "my-access",
            refreshToken = "my-refresh"
        )
        val toString = response.toString()

        assertTrue(toString.contains("my-access"))
        assertTrue(toString.contains("my-refresh"))
    }
}
