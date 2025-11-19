package com.fiveis.xend.data.repository

import com.fiveis.xend.data.model.AuthCodeRequest
import com.fiveis.xend.data.model.AuthResponse
import com.fiveis.xend.data.model.JwtTokens
import com.fiveis.xend.data.model.LogoutRequest
import com.fiveis.xend.data.model.LogoutResponse
import com.fiveis.xend.data.source.AuthApiService
import com.fiveis.xend.network.RetrofitClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class AuthRepositoryTest {

    private lateinit var authApiService: AuthApiService
    private lateinit var repository: AuthRepository

    @Before
    fun setup() {
        authApiService = mockk()

        mockkObject(RetrofitClient)
        every { RetrofitClient.authApiService } returns authApiService

        repository = AuthRepository()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun sendAuthCodeToServer_with_jwt_tokens_returns_Success() = runTest {
        val authCode = "test_auth_code_123"
        val accessToken = "access_token_123"
        val refreshToken = "refresh_token_123"

        val mockResponse = Response.success(
            AuthResponse(
                jwt = JwtTokens(
                    access = accessToken,
                    refresh = refreshToken
                )
            )
        )

        coEvery {
            authApiService.sendAuthCode(AuthCodeRequest(authCode = authCode))
        } returns mockResponse

        val result = repository.sendAuthCodeToServer(authCode)

        assertTrue(result is AuthResult.Success)
        assertEquals(accessToken, (result as AuthResult.Success).accessToken)
        assertEquals(refreshToken, result.refreshToken)

        coVerify {
            authApiService.sendAuthCode(
                match { request ->
                    request.authCode == authCode
                }
            )
        }
    }

    @Test
    fun sendAuthCodeToServer_with_legacy_tokens_returns_Success() = runTest {
        val authCode = "test_auth_code_123"
        val accessToken = "access_token_123"
        val refreshToken = "refresh_token_123"

        val mockResponse = Response.success(
            AuthResponse(
                jwt = null,
                accessToken = accessToken,
                refreshToken = refreshToken
            )
        )

        coEvery {
            authApiService.sendAuthCode(AuthCodeRequest(authCode = authCode))
        } returns mockResponse

        val result = repository.sendAuthCodeToServer(authCode)

        assertTrue(result is AuthResult.Success)
        assertEquals(accessToken, (result as AuthResult.Success).accessToken)
        assertEquals(refreshToken, result.refreshToken)
    }

    @Test
    fun sendAuthCodeToServer_with_null_body_returns_Failure() = runTest {
        val authCode = "test_auth_code_123"

        val mockResponse = Response.success<AuthResponse>(null)

        coEvery {
            authApiService.sendAuthCode(AuthCodeRequest(authCode = authCode))
        } returns mockResponse

        val result = repository.sendAuthCodeToServer(authCode)

        assertTrue(result is AuthResult.Failure)
        assertTrue((result as AuthResult.Failure).message.contains("비어있는 응답"))
    }

    @Test
    fun sendAuthCodeToServer_with_no_tokens_in_response_returns_Failure() = runTest {
        val authCode = "test_auth_code_123"

        val mockResponse = Response.success(
            AuthResponse(
                jwt = JwtTokens(access = null, refresh = null),
                accessToken = null,
                refreshToken = null
            )
        )

        coEvery {
            authApiService.sendAuthCode(AuthCodeRequest(authCode = authCode))
        } returns mockResponse

        val result = repository.sendAuthCodeToServer(authCode)

        assertTrue(result is AuthResult.Failure)
        assertTrue((result as AuthResult.Failure).message.contains("토큰이 없습니다"))
    }

    @Test
    fun sendAuthCodeToServer_with_error_response_returns_Failure() = runTest {
        val authCode = "test_auth_code_123"

        val mockResponse = Response.error<AuthResponse>(
            400,
            "Invalid auth code".toResponseBody()
        )

        coEvery {
            authApiService.sendAuthCode(AuthCodeRequest(authCode = authCode))
        } returns mockResponse

        val result = repository.sendAuthCodeToServer(authCode)

        assertTrue(result is AuthResult.Failure)
        assertTrue((result as AuthResult.Failure).message.contains("서버 오류"))
        assertTrue((result as AuthResult.Failure).message.contains("HTTP 400"))
    }

    @Test
    fun sendAuthCodeToServer_with_network_exception_returns_Failure() = runTest {
        val authCode = "test_auth_code_123"

        coEvery {
            authApiService.sendAuthCode(AuthCodeRequest(authCode = authCode))
        } throws Exception("Network error")

        val result = repository.sendAuthCodeToServer(authCode)

        assertTrue(result is AuthResult.Failure)
        assertTrue((result as AuthResult.Failure).message.contains("서버 연동 실패"))
        assertTrue(result.message.contains("Network error"))
    }

    @Test
    fun logout_with_valid_tokens_calls_api_service() = runTest {
        val refreshToken = "refresh_token_123"

        coEvery {
            authApiService.logout(LogoutRequest(refresh = refreshToken))
        } returns Response.success(LogoutResponse(detail = "Successfully logged out"))

        val result = repository.logout(refreshToken)

        assertTrue(result is LogoutResult.Success)
        coVerify {
            authApiService.logout(
                match { request ->
                    request.refresh == refreshToken
                }
            )
        }
    }

    @Test
    fun logout_with_null_refresh_token_returns_failure() = runTest {
        val result = repository.logout(null)

        assertTrue(result is LogoutResult.Failure)
        coVerify(exactly = 0) {
            authApiService.logout(any())
        }
    }

    @Test
    fun logout_with_api_success_returns_success() = runTest {
        val refreshToken = "refresh_token_123"

        coEvery {
            authApiService.logout(LogoutRequest(refresh = refreshToken))
        } returns Response.success(LogoutResponse(detail = "Successfully logged out"))

        val result = repository.logout(refreshToken)

        assertTrue(result is LogoutResult.Success)
    }

    @Test
    fun logout_with_api_failure_returns_failure() = runTest {
        val refreshToken = "refresh_token_123"

        coEvery {
            authApiService.logout(any())
        } throws Exception("Server error")

        val result = repository.logout(refreshToken)

        assertTrue(result is LogoutResult.Failure)
    }
}
