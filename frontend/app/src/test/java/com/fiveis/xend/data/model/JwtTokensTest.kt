package com.fiveis.xend.data.model

import org.junit.Assert.*
import org.junit.Test

class JwtTokensTest {

    @Test
    fun create_jwt_tokens_with_both_tokens() {
        val tokens = JwtTokens(
            access = "access-token-123",
            refresh = "refresh-token-456"
        )

        assertEquals("access-token-123", tokens.access)
        assertEquals("refresh-token-456", tokens.refresh)
    }

    @Test
    fun create_jwt_tokens_with_null_access_token() {
        val tokens = JwtTokens(
            access = null,
            refresh = "refresh-token-456"
        )

        assertNull(tokens.access)
        assertEquals("refresh-token-456", tokens.refresh)
    }

    @Test
    fun create_jwt_tokens_with_null_refresh_token() {
        val tokens = JwtTokens(
            access = "access-token-123",
            refresh = null
        )

        assertEquals("access-token-123", tokens.access)
        assertNull(tokens.refresh)
    }

    @Test
    fun create_jwt_tokens_with_both_null() {
        val tokens = JwtTokens(
            access = null,
            refresh = null
        )

        assertNull(tokens.access)
        assertNull(tokens.refresh)
    }

    @Test
    fun jwt_tokens_copy_updates_access_token() {
        val original = JwtTokens(
            access = "old-access",
            refresh = "refresh-123"
        )

        val updated = original.copy(access = "new-access")

        assertEquals("new-access", updated.access)
        assertEquals("refresh-123", updated.refresh)
    }

    @Test
    fun jwt_tokens_with_same_values_are_equal() {
        val tokens1 = JwtTokens(
            access = "access-123",
            refresh = "refresh-456"
        )

        val tokens2 = JwtTokens(
            access = "access-123",
            refresh = "refresh-456"
        )

        assertEquals(tokens1, tokens2)
        assertEquals(tokens1.hashCode(), tokens2.hashCode())
    }

    @Test
    fun jwt_tokens_with_different_values_are_not_equal() {
        val tokens1 = JwtTokens(
            access = "access-123",
            refresh = "refresh-456"
        )

        val tokens2 = JwtTokens(
            access = "different-access",
            refresh = "refresh-456"
        )

        assertNotEquals(tokens1, tokens2)
    }
}
