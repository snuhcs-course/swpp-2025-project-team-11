package com.fiveis.xend.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AuthResponseExtendedTest {

    @Test
    fun test_authResponse_withJwtTokens() {
        val jwt = JwtTokens(
            access = "access_token_xyz",
            refresh = "refresh_token_xyz"
        )
        val response = AuthResponse(jwt = jwt)

        assertNotNull(response.jwt)
        assertEquals("access_token_xyz", response.jwt?.access)
        assertEquals("refresh_token_xyz", response.jwt?.refresh)
    }

    @Test
    fun test_authResponse_withLegacyTokens() {
        val response = AuthResponse(
            jwt = null,
            accessToken = "access_token_legacy",
            refreshToken = "refresh_token_legacy"
        )

        assertNull(response.jwt)
        assertEquals("access_token_legacy", response.accessToken)
        assertEquals("refresh_token_legacy", response.refreshToken)
    }

    @Test
    fun test_authResponse_emptyResponse() {
        val response = AuthResponse()

        assertNull(response.jwt)
        assertNull(response.accessToken)
        assertNull(response.refreshToken)
    }

    @Test
    fun test_authResponse_jwtWithLongTokens() {
        val longAccess = "A".repeat(1000)
        val longRefresh = "B".repeat(1000)
        val jwt = JwtTokens(access = longAccess, refresh = longRefresh)
        val response = AuthResponse(jwt = jwt)

        assertEquals(1000, response.jwt?.access?.length)
        assertEquals(1000, response.jwt?.refresh?.length)
    }

    @Test
    fun test_authResponse_equality() {
        val jwt1 = JwtTokens("access1", "refresh1")
        val jwt2 = JwtTokens("access1", "refresh1")
        val response1 = AuthResponse(jwt = jwt1)
        val response2 = AuthResponse(jwt = jwt2)

        assertEquals(response1, response2)
    }

    @Test
    fun test_authResponse_inequality() {
        val jwt1 = JwtTokens("access1", "refresh1")
        val jwt2 = JwtTokens("access2", "refresh2")
        val response1 = AuthResponse(jwt = jwt1)
        val response2 = AuthResponse(jwt = jwt2)

        assertNotEquals(response1, response2)
    }

    @Test
    fun test_authResponse_copy() {
        val jwt = JwtTokens("access", "refresh")
        val original = AuthResponse(jwt = jwt)
        val modified = original.copy(accessToken = "new_access")

        assertNotNull(original.jwt)
        assertNull(original.accessToken)
        assertEquals("new_access", modified.accessToken)
    }

    @Test
    fun test_authResponse_nullJwt() {
        val response = AuthResponse(jwt = null)

        assertNull(response.jwt)
    }

    @Test
    fun test_authResponse_bothFormats() {
        val jwt = JwtTokens("jwt_access", "jwt_refresh")
        val response = AuthResponse(
            jwt = jwt,
            accessToken = "legacy_access",
            refreshToken = "legacy_refresh"
        )

        assertNotNull(response.jwt)
        assertEquals("jwt_access", response.jwt?.access)
        assertEquals("legacy_access", response.accessToken)
        assertEquals("legacy_refresh", response.refreshToken)
    }
}
