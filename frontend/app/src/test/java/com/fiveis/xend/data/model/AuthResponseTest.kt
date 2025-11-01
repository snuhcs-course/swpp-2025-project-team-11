package com.fiveis.xend.data.model

import org.junit.Assert.*
import org.junit.Test

class AuthResponseTest {

    @Test
    fun create_auth_response_with_jwt_tokens() {
        val jwtTokens = JwtTokens(
            access = "jwt-access-123",
            refresh = "jwt-refresh-456"
        )

        val authResponse = AuthResponse(jwt = jwtTokens)

        assertNotNull(authResponse.jwt)
        assertEquals("jwt-access-123", authResponse.jwt?.access)
        assertEquals("jwt-refresh-456", authResponse.jwt?.refresh)
        assertNull(authResponse.accessToken)
        assertNull(authResponse.refreshToken)
    }

    @Test
    fun create_auth_response_with_legacy_tokens() {
        val authResponse = AuthResponse(
            accessToken = "legacy-access-123",
            refreshToken = "legacy-refresh-456"
        )

        assertNull(authResponse.jwt)
        assertEquals("legacy-access-123", authResponse.accessToken)
        assertEquals("legacy-refresh-456", authResponse.refreshToken)
    }

    @Test
    fun create_auth_response_with_both_jwt_and_legacy_tokens() {
        val jwtTokens = JwtTokens(
            access = "jwt-access",
            refresh = "jwt-refresh"
        )

        val authResponse = AuthResponse(
            jwt = jwtTokens,
            accessToken = "legacy-access",
            refreshToken = "legacy-refresh"
        )

        assertNotNull(authResponse.jwt)
        assertEquals("jwt-access", authResponse.jwt?.access)
        assertEquals("jwt-refresh", authResponse.jwt?.refresh)
        assertEquals("legacy-access", authResponse.accessToken)
        assertEquals("legacy-refresh", authResponse.refreshToken)
    }

    @Test
    fun create_empty_auth_response() {
        val authResponse = AuthResponse()

        assertNull(authResponse.jwt)
        assertNull(authResponse.accessToken)
        assertNull(authResponse.refreshToken)
    }

    @Test
    fun auth_response_copy_updates_jwt() {
        val original = AuthResponse(
            jwt = JwtTokens("old-access", "old-refresh")
        )

        val newJwt = JwtTokens("new-access", "new-refresh")
        val updated = original.copy(jwt = newJwt)

        assertEquals("new-access", updated.jwt?.access)
        assertEquals("new-refresh", updated.jwt?.refresh)
    }

    @Test
    fun auth_responses_with_same_values_are_equal() {
        val response1 = AuthResponse(
            jwt = JwtTokens("access", "refresh")
        )

        val response2 = AuthResponse(
            jwt = JwtTokens("access", "refresh")
        )

        assertEquals(response1, response2)
        assertEquals(response1.hashCode(), response2.hashCode())
    }

    @Test
    fun auth_responses_with_different_legacy_tokens_are_not_equal() {
        val response1 = AuthResponse(
            accessToken = "access-1",
            refreshToken = "refresh-1"
        )

        val response2 = AuthResponse(
            accessToken = "access-2",
            refreshToken = "refresh-2"
        )

        assertNotEquals(response1, response2)
    }
}
