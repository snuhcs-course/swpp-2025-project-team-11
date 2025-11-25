package com.fiveis.xend.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LogoutRequestExtendedTest {

    @Test
    fun test_logoutRequest_basic() {
        val request = LogoutRequest(refresh = "refresh_token_xyz")

        assertEquals("refresh_token_xyz", request.refresh)
    }

    @Test
    fun test_logoutRequest_longToken() {
        val longToken = "A".repeat(1000)
        val request = LogoutRequest(refresh = longToken)

        assertEquals(1000, request.refresh.length)
    }

    @Test
    fun test_logoutRequest_jwtFormat() {
        val jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        val request = LogoutRequest(refresh = jwtToken)

        assertEquals(jwtToken, request.refresh)
    }

    @Test
    fun test_logoutRequest_equality() {
        val request1 = LogoutRequest("token123")
        val request2 = LogoutRequest("token123")

        assertEquals(request1, request2)
        assertEquals(request1.hashCode(), request2.hashCode())
    }

    @Test
    fun test_logoutRequest_inequality() {
        val request1 = LogoutRequest("token123")
        val request2 = LogoutRequest("token456")

        assertNotEquals(request1, request2)
    }

    @Test
    fun test_logoutRequest_copy() {
        val original = LogoutRequest("original_token")
        val modified = original.copy(refresh = "modified_token")

        assertEquals("original_token", original.refresh)
        assertEquals("modified_token", modified.refresh)
    }

    @Test
    fun test_logoutRequest_emptyToken() {
        val request = LogoutRequest(refresh = "")

        assertEquals("", request.refresh)
    }

    @Test
    fun test_logoutRequest_specialCharacters() {
        val token = "token-with_special.chars/123+456=789"
        val request = LogoutRequest(refresh = token)

        assertEquals(token, request.refresh)
    }
}
