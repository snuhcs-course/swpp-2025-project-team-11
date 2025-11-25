package com.fiveis.xend.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class JwtTokensExtendedTest {

    @Test
    fun test_jwtTokens_basic() {
        val tokens = JwtTokens(
            access = "access_token",
            refresh = "refresh_token"
        )

        assertEquals("access_token", tokens.access)
        assertEquals("refresh_token", tokens.refresh)
    }

    @Test
    fun test_jwtTokens_realJwtFormat() {
        val accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        val refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE1MTYyNDI2MjJ9.4Adcj0vkIr8yT5KpkjZKlT6B8FzcCfP5mjPDJWQQLOg"
        val tokens = JwtTokens(access = accessToken, refresh = refreshToken)

        assertEquals(accessToken, tokens.access)
        assertEquals(refreshToken, tokens.refresh)
        assert(tokens.access?.split(".")?.size == 3) // JWT has 3 parts
        assert(tokens.refresh?.split(".")?.size == 3)
    }

    @Test
    fun test_jwtTokens_longTokens() {
        val longAccess = "access_" + "A".repeat(1000)
        val longRefresh = "refresh_" + "B".repeat(1000)
        val tokens = JwtTokens(access = longAccess, refresh = longRefresh)

        assert((tokens.access?.length ?: 0) > 1000)
        assert((tokens.refresh?.length ?: 0) > 1000)
    }

    @Test
    fun test_jwtTokens_equality() {
        val tokens1 = JwtTokens("access1", "refresh1")
        val tokens2 = JwtTokens("access1", "refresh1")

        assertEquals(tokens1, tokens2)
        assertEquals(tokens1.hashCode(), tokens2.hashCode())
    }

    @Test
    fun test_jwtTokens_inequality_differentAccess() {
        val tokens1 = JwtTokens("access1", "refresh")
        val tokens2 = JwtTokens("access2", "refresh")

        assertNotEquals(tokens1, tokens2)
    }

    @Test
    fun test_jwtTokens_inequality_differentRefresh() {
        val tokens1 = JwtTokens("access", "refresh1")
        val tokens2 = JwtTokens("access", "refresh2")

        assertNotEquals(tokens1, tokens2)
    }

    @Test
    fun test_jwtTokens_copy_access() {
        val original = JwtTokens("access_old", "refresh_old")
        val modified = original.copy(access = "access_new")

        assertEquals("access_old", original.access)
        assertEquals("access_new", modified.access)
        assertEquals("refresh_old", modified.refresh)
    }

    @Test
    fun test_jwtTokens_copy_refresh() {
        val original = JwtTokens("access_old", "refresh_old")
        val modified = original.copy(refresh = "refresh_new")

        assertEquals("refresh_old", original.refresh)
        assertEquals("refresh_new", modified.refresh)
        assertEquals("access_old", modified.access)
    }

    @Test
    fun test_jwtTokens_emptyTokens() {
        val tokens = JwtTokens(access = "", refresh = "")

        assertEquals("", tokens.access)
        assertEquals("", tokens.refresh)
    }

    @Test
    fun test_jwtTokens_differentLengths() {
        val shortAccess = "short"
        val longRefresh = "very_long_refresh_token_" + "A".repeat(500)
        val tokens = JwtTokens(access = shortAccess, refresh = longRefresh)

        assertEquals(5, tokens.access?.length)
        assert((tokens.refresh?.length ?: 0) > 500)
    }

    @Test
    fun test_jwtTokens_specialCharacters() {
        val access = "access.token-with_special/chars+123=456"
        val refresh = "refresh.token-with_special/chars+789=012"
        val tokens = JwtTokens(access = access, refresh = refresh)

        assertEquals(access, tokens.access)
        assertEquals(refresh, tokens.refresh)
    }
}
