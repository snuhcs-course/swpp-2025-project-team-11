package com.fiveis.xend.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AuthCodeRequestExtendedTest {

    @Test
    fun test_authCodeRequest_basic() {
        val request = AuthCodeRequest(authCode = "auth_code_12345")

        assertEquals("auth_code_12345", request.authCode)
    }

    @Test
    fun test_authCodeRequest_longCode() {
        val longCode = "A".repeat(500)
        val request = AuthCodeRequest(authCode = longCode)

        assertEquals(500, request.authCode.length)
    }

    @Test
    fun test_authCodeRequest_specialCharacters() {
        val code = "code-with_special.chars/123+456=789"
        val request = AuthCodeRequest(authCode = code)

        assertEquals(code, request.authCode)
    }

    @Test
    fun test_authCodeRequest_emptyCode() {
        val request = AuthCodeRequest(authCode = "")

        assertEquals("", request.authCode)
    }

    @Test
    fun test_authCodeRequest_equality() {
        val request1 = AuthCodeRequest("code123")
        val request2 = AuthCodeRequest("code123")

        assertEquals(request1, request2)
        assertEquals(request1.hashCode(), request2.hashCode())
    }

    @Test
    fun test_authCodeRequest_inequality() {
        val request1 = AuthCodeRequest("code123")
        val request2 = AuthCodeRequest("code456")

        assertNotEquals(request1, request2)
    }

    @Test
    fun test_authCodeRequest_copy() {
        val original = AuthCodeRequest("original_code")
        val modified = original.copy(authCode = "modified_code")

        assertEquals("original_code", original.authCode)
        assertEquals("modified_code", modified.authCode)
    }

    @Test
    fun test_authCodeRequest_urlEncodedCode() {
        val urlEncodedCode = "4%2F0AeanS0a-xFZ2kQ_code%3D%3D"
        val request = AuthCodeRequest(authCode = urlEncodedCode)

        assertEquals(urlEncodedCode, request.authCode)
    }

    @Test
    fun test_authCodeRequest_base64Code() {
        val base64Code = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXo="
        val request = AuthCodeRequest(authCode = base64Code)

        assertEquals(base64Code, request.authCode)
    }
}
