package com.fiveis.xend.data.model

import org.junit.Assert.*
import org.junit.Test

class AuthCodeRequestTest {

    @Test
    fun create_auth_code_request_with_auth_code() {
        val request = AuthCodeRequest(authCode = "test-auth-code-123")

        assertEquals("test-auth-code-123", request.authCode)
    }

    @Test
    fun auth_code_request_copy_updates_auth_code() {
        val original = AuthCodeRequest(authCode = "original-code")
        val updated = original.copy(authCode = "updated-code")

        assertEquals("updated-code", updated.authCode)
    }

    @Test
    fun auth_code_requests_with_same_values_are_equal() {
        val request1 = AuthCodeRequest(authCode = "same-code")
        val request2 = AuthCodeRequest(authCode = "same-code")

        assertEquals(request1, request2)
        assertEquals(request1.hashCode(), request2.hashCode())
    }

    @Test
    fun auth_code_requests_with_different_values_are_not_equal() {
        val request1 = AuthCodeRequest(authCode = "code-1")
        val request2 = AuthCodeRequest(authCode = "code-2")

        assertNotEquals(request1, request2)
    }

    @Test
    fun auth_code_request_to_string_contains_auth_code() {
        val request = AuthCodeRequest(authCode = "my-code")
        val toString = request.toString()

        assertTrue(toString.contains("my-code"))
    }
}
