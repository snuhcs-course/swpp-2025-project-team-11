package com.fiveis.xend.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TokenRefreshRequestExtendedTest {

    @Test
    fun test_tokenRefreshRequest_basic() {
        val request = TokenRefreshRequest(refreshToken = "refresh_token_xyz")

        assertEquals("refresh_token_xyz", request.refreshToken)
    }

    @Test
    fun test_tokenRefreshRequest_longToken() {
        val longToken = "refresh_" + "A".repeat(500)
        val request = TokenRefreshRequest(refreshToken = longToken)

        assert(request.refreshToken.length > 500)
    }

    @Test
    fun test_tokenRefreshRequest_jwtFormat() {
        val jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U"
        val request = TokenRefreshRequest(refreshToken = jwtToken)

        assertEquals(jwtToken, request.refreshToken)
    }

    @Test
    fun test_tokenRefreshRequest_equality() {
        val request1 = TokenRefreshRequest("token123")
        val request2 = TokenRefreshRequest("token123")

        assertEquals(request1, request2)
        assertEquals(request1.hashCode(), request2.hashCode())
    }

    @Test
    fun test_tokenRefreshRequest_inequality() {
        val request1 = TokenRefreshRequest("token123")
        val request2 = TokenRefreshRequest("token456")

        assertNotEquals(request1, request2)
    }

    @Test
    fun test_tokenRefreshRequest_copy() {
        val original = TokenRefreshRequest("original_token")
        val modified = original.copy(refreshToken = "modified_token")

        assertEquals("original_token", original.refreshToken)
        assertEquals("modified_token", modified.refreshToken)
    }

    @Test
    fun test_tokenRefreshRequest_emptyToken() {
        val request = TokenRefreshRequest(refreshToken = "")

        assertEquals("", request.refreshToken)
    }

    @Test
    fun test_tokenRefreshRequest_specialCharacters() {
        val token = "token-with_special.chars/123+456=789"
        val request = TokenRefreshRequest(refreshToken = token)

        assertEquals(token, request.refreshToken)
    }
}
