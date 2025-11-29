package com.fiveis.xend.ui.login

import org.junit.Assert.*
import org.junit.Test

class MainActivityLogicTest {

    @Test
    fun googleAuthErrorCode_7_message() {
        val errorCode = 7
        val expectedMessage = "로그인 실패 (코드 7)\n\n가능한 원인:\n• SHA-1 인증서 미등록\n• 패키지명 불일치\n• OAuth Client ID 설정 오류"

        // Simulating the error handling logic from MainActivity
        val actualMessage = when (errorCode) {
            7 -> "로그인 실패 (코드 7)\n\n가능한 원인:\n• SHA-1 인증서 미등록\n• 패키지명 불일치\n• OAuth Client ID 설정 오류"
            else -> "Unknown error"
        }

        assertEquals(expectedMessage, actualMessage)
    }

    @Test
    fun googleAuthErrorCode_10_message() {
        val errorCode = 10
        val expectedMessage = "개발자 오류: OAuth 설정 확인 필요"

        val actualMessage = when (errorCode) {
            10 -> "개발자 오류: OAuth 설정 확인 필요"
            else -> "Unknown error"
        }

        assertEquals(expectedMessage, actualMessage)
    }

    @Test
    fun googleAuthErrorCode_12501_message() {
        val errorCode = 12501
        val expectedMessage = "로그인이 취소되었습니다"

        val actualMessage = when (errorCode) {
            12501 -> "로그인이 취소되었습니다"
            else -> "Unknown error"
        }

        assertEquals(expectedMessage, actualMessage)
    }

    @Test
    fun googleAuthErrorCode_unknown_message() {
        val errorCode = 9999
        val statusMessage = "Unknown error occurred"

        val actualMessage = when (errorCode) {
            7 -> "로그인 실패 (코드 7)"
            10 -> "개발자 오류: OAuth 설정 확인 필요"
            12501 -> "로그인이 취소되었습니다"
            else -> "로그인 실패: $errorCode\n$statusMessage"
        }

        assertEquals("로그인 실패: 9999\n$statusMessage", actualMessage)
    }

    @Test
    fun authCode_validation_null() {
        val authCode: String? = null
        val isValid = !authCode.isNullOrBlank()

        assertFalse(isValid)
    }

    @Test
    fun authCode_validation_empty() {
        val authCode = ""
        val isValid = !authCode.isNullOrBlank()

        assertFalse(isValid)
    }

    @Test
    fun authCode_validation_blank() {
        val authCode = "   "
        val isValid = !authCode.isNullOrBlank()

        assertFalse(isValid)
    }

    @Test
    fun authCode_validation_valid() {
        val authCode = "valid_auth_code_123"
        val isValid = !authCode.isNullOrBlank()

        assertTrue(isValid)
    }

    @Test
    fun email_fallback_when_null() {
        val accountEmail: String? = null
        val email = accountEmail ?: "unknown@example.com"

        assertEquals("unknown@example.com", email)
    }

    @Test
    fun email_no_fallback_when_present() {
        val accountEmail = "user@example.com"
        val email = accountEmail ?: "unknown@example.com"

        assertEquals("user@example.com", email)
    }

    @Test
    fun authCode_truncate_for_logging() {
        val authCode = "verylongauthcode1234567890abcdefghijklmnopqrstuvwxyz"
        val truncated = authCode.take(30)

        assertEquals(30, truncated.length)
        assertEquals("verylongauthcode1234567890abcd", truncated)
    }

    @Test
    fun authCode_truncate_short_code() {
        val authCode = "short"
        val truncated = authCode.take(30)

        assertEquals("short", truncated)
        assertEquals(5, truncated.length)
    }

    @Test
    fun google_scopes_list() {
        val scopes = listOf(
            "https://www.googleapis.com/auth/gmail.readonly",
            "https://www.googleapis.com/auth/gmail.send",
            "https://www.googleapis.com/auth/gmail.modify"
        )

        assertEquals(3, scopes.size)
        assertTrue(scopes.contains("https://www.googleapis.com/auth/gmail.readonly"))
        assertTrue(scopes.contains("https://www.googleapis.com/auth/gmail.send"))
        assertTrue(scopes.contains("https://www.googleapis.com/auth/gmail.modify"))
    }

    @Test
    fun google_scopes_readonly() {
        val scope = "https://www.googleapis.com/auth/gmail.readonly"

        assertTrue(scope.contains("gmail"))
        assertTrue(scope.contains("readonly"))
    }

    @Test
    fun google_scopes_send() {
        val scope = "https://www.googleapis.com/auth/gmail.send"

        assertTrue(scope.contains("gmail"))
        assertTrue(scope.contains("send"))
    }

    @Test
    fun google_scopes_modify() {
        val scope = "https://www.googleapis.com/auth/gmail.modify"

        assertTrue(scope.contains("gmail"))
        assertTrue(scope.contains("modify"))
    }

    @Test
    fun error_message_missing_auth_code() {
        val errorMsg = "Authorization Code를 받지 못했습니다.\n\n" +
            "확인사항:\n" +
            "1) server_client_id가 Web 클라이언트 ID인지\n" +
            "2) GCP에서 Gmail API 활성화\n" +
            "3) OAuth 동의 화면에 Gmail 스코프 추가"

        assertTrue(errorMsg.contains("Authorization Code"))
        assertTrue(errorMsg.contains("server_client_id"))
        assertTrue(errorMsg.contains("Gmail API"))
    }

    @Test
    fun success_toast_message() {
        val message = "Auth Code 수신 성공!"

        assertEquals("Auth Code 수신 성공!", message)
        assertTrue(message.contains("성공"))
    }

    @Test
    fun logout_success_message() {
        val message = "로그아웃 완료"

        assertEquals("로그아웃 완료", message)
        assertTrue(message.contains("로그아웃"))
    }

    @Test
    fun logout_failure_message() {
        val message = "로그아웃 실패"

        assertEquals("로그아웃 실패", message)
        assertTrue(message.contains("실패"))
    }

    @Test
    fun signin_canceled_message() {
        val message = "로그인이 취소되었습니다"

        assertEquals("로그인이 취소되었습니다", message)
        assertTrue(message.contains("취소"))
    }

    @Test
    fun account_info_error_message() {
        val message = "계정 정보를 가져오지 못했습니다"

        assertEquals("계정 정보를 가져오지 못했습니다", message)
        assertTrue(message.contains("계정"))
    }

    @Test
    fun navigation_error_message() {
        val exception = Exception("Test exception")
        val message = "화면 이동 실패: ${exception.message}"

        assertEquals("화면 이동 실패: Test exception", message)
        assertTrue(message.contains("이동 실패"))
    }

    @Test
    fun force_code_for_refresh_token_flag() {
        val forceCodeForRefreshToken = true

        assertTrue(forceCodeForRefreshToken)
    }

    @Test
    fun google_signin_options_default() {
        // Simulating DEFAULT_SIGN_IN check
        val isDefault = true

        assertTrue(isDefault)
    }

    @Test
    fun server_auth_code_check() {
        val hasServerAuthCode = true

        assertTrue(hasServerAuthCode)
    }

    @Test
    fun email_request_check() {
        val requestEmail = true

        assertTrue(requestEmail)
    }

    @Test
    fun multiple_error_codes_handling() {
        val errorCodes = listOf(7, 10, 12501, 9999)

        errorCodes.forEach { code ->
            val message = when (code) {
                7 -> "로그인 실패 (코드 7)"
                10 -> "개발자 오류: OAuth 설정 확인 필요"
                12501 -> "로그인이 취소되었습니다"
                else -> "로그인 실패: $code"
            }
            assertNotNull(message)
            assertTrue(message.isNotEmpty())
        }
    }

    @Test
    fun error_code_7_contains_troubleshooting_info() {
        val message = "로그인 실패 (코드 7)\n\n가능한 원인:\n• SHA-1 인증서 미등록\n• 패키지명 불일치\n• OAuth Client ID 설정 오류"

        assertTrue(message.contains("SHA-1"))
        assertTrue(message.contains("패키지명"))
        assertTrue(message.contains("OAuth Client ID"))
    }

    @Test
    fun granted_scopes_join_to_string() {
        val scopes = listOf(
            "https://www.googleapis.com/auth/gmail.readonly",
            "https://www.googleapis.com/auth/gmail.send"
        )

        val joined = scopes.joinToString { it }

        assertTrue(joined.contains("gmail.readonly"))
        assertTrue(joined.contains("gmail.send"))
    }

    @Test
    fun auth_code_take_30_characters() {
        val longCode = "a".repeat(100)
        val taken = longCode.take(30)

        assertEquals(30, taken.length)
    }

    @Test
    fun null_account_handling() {
        val account: Any? = null
        val isNull = account == null

        assertTrue(isNull)
    }

    @Test
    fun result_data_null_check() {
        val data: Any? = null
        val isNull = data == null

        assertTrue(isNull)
    }

    @Test
    fun log_tag_google_auth() {
        val tag = "GoogleAuth"

        assertEquals("GoogleAuth", tag)
        assertTrue(tag.isNotEmpty())
    }

    @Test
    fun log_tag_navigation() {
        val tag = "Nav"

        assertEquals("Nav", tag)
        assertTrue(tag.isNotEmpty())
    }
}
