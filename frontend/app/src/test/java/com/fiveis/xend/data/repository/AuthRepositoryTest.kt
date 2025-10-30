package com.fiveis.xend.data.repository

import com.fiveis.xend.data.model.AuthCodeRequest
import com.fiveis.xend.data.model.AuthResponse
import com.fiveis.xend.data.model.JwtTokens
import com.fiveis.xend.data.model.LogoutRequest
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
    fun `sendAuthCodeToServer with jwt tokens returns Success`() = runTest {
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
    fun `sendAuthCodeToServer with legacy tokens returns Success`() = runTest {
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
    fun `sendAuthCodeToServer with null body returns Failure`() = runTest {
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
    fun `sendAuthCodeToServer with no tokens in response returns Failure`() = runTest {
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
    fun `sendAuthCodeToServer with error response returns Failure`() = runTest {
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
    fun `sendAuthCodeToServer with network exception returns Failure`() = runTest {
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
    fun `logout with valid tokens calls api service`() = runTest {
        val accessToken = "access_token_123"
        val refreshToken = "refresh_token_123"

        coEvery {
            authApiService.logout(
                accessToken = "Bearer $accessToken",
                request = LogoutRequest(refresh = refreshToken)
            )
        } returns Response.success(Unit)

        repository.logout(accessToken, refreshToken)

        coVerify {
            authApiService.logout(
                accessToken = "Bearer $accessToken",
                request = match { request ->
                    request.refresh == refreshToken
                }
            )
        }
    }

    @Test
    fun `logout with null access token does not call api`() = runTest {
        val refreshToken = "refresh_token_123"

        repository.logout(null, refreshToken)

        coVerify(exactly = 0) {
            authApiService.logout(any(), any())
        }
    }

    @Test
    fun `logout with null refresh token uses empty string`() = runTest {
        val accessToken = "access_token_123"

        coEvery {
            authApiService.logout(
                accessToken = "Bearer $accessToken",
                request = LogoutRequest(refresh = "")
            )
        } returns Response.success(Unit)

        repository.logout(accessToken, null)

        coVerify {
            authApiService.logout(
                accessToken = "Bearer $accessToken",
                request = match { request ->
                    request.refresh == ""
                }
            )
        }
    }

    @Test
    fun `logout with api failure does not throw exception`() = runTest {
        val accessToken = "access_token_123"
        val refreshToken = "refresh_token_123"

        coEvery {
            authApiService.logout(any(), any())
        } throws Exception("Server error")

        repository.logout(accessToken, refreshToken)
    }
}
