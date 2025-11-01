package com.fiveis.xend.network

import android.content.Context
import com.fiveis.xend.data.model.TokenRefreshRequest
import com.fiveis.xend.data.model.TokenRefreshResponse
import com.fiveis.xend.data.source.AuthApiService
import com.fiveis.xend.data.source.TokenManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Response as RetrofitResponse

class TokenRefreshAuthenticatorTest {

    private lateinit var context: Context
    private lateinit var tokenManager: TokenManager
    private lateinit var authenticator: TokenRefreshAuthenticator
    private lateinit var authApiService: AuthApiService

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        tokenManager = mockk(relaxed = true)
        authApiService = mockk()

        mockkObject(RetrofitClient)
        every { RetrofitClient.authApiService } returns authApiService

        authenticator = TokenRefreshAuthenticator(context, tokenManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun authenticate_with_valid_refresh_token_returns_new_request() {
        val refreshToken = "valid_refresh_token"
        val newAccessToken = "new_access_token"
        val newRefreshToken = "new_refresh_token"
        val userEmail = "test@example.com"

        every { tokenManager.getRefreshToken() } returns refreshToken
        every { tokenManager.getUserEmail() } returns userEmail
        every { tokenManager.saveTokens(any(), any(), any()) } returns Unit

        coEvery {
            authApiService.refreshToken(TokenRefreshRequest(refreshToken))
        } returns RetrofitResponse.success(
            TokenRefreshResponse(accessToken = newAccessToken, refreshToken = newRefreshToken)
        )

        val originalRequest = Request.Builder()
            .url("https://example.com/api/test")
            .build()
        val response = Response.Builder()
            .request(originalRequest)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody())
            .build()

        val newRequest = authenticator.authenticate(null, response)

        assertNotNull(newRequest)
        assertEquals("Bearer $newAccessToken", newRequest?.header("Authorization"))
        verify { tokenManager.saveTokens(newAccessToken, newRefreshToken, userEmail) }
    }

    @Test
    fun authenticate_with_legacy_token_format_returns_new_request() {
        val refreshToken = "valid_refresh_token"
        val newAccessToken = "new_access_token"
        val newRefreshToken = "new_refresh_token"
        val userEmail = "test@example.com"

        every { tokenManager.getRefreshToken() } returns refreshToken
        every { tokenManager.getUserEmail() } returns userEmail
        every { tokenManager.saveTokens(any(), any(), any()) } returns Unit

        coEvery {
            authApiService.refreshToken(TokenRefreshRequest(refreshToken))
        } returns RetrofitResponse.success(
            TokenRefreshResponse(accessToken = newAccessToken, refreshToken = newRefreshToken)
        )

        val originalRequest = Request.Builder()
            .url("https://example.com/api/test")
            .build()
        val response = Response.Builder()
            .request(originalRequest)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody())
            .build()

        val newRequest = authenticator.authenticate(null, response)

        assertNotNull(newRequest)
        assertEquals("Bearer $newAccessToken", newRequest?.header("Authorization"))
    }

    @Test
    fun authenticate_with_null_refresh_token_returns_null() {
        every { tokenManager.getRefreshToken() } returns null

        val originalRequest = Request.Builder()
            .url("https://example.com/api/test")
            .build()
        val response = Response.Builder()
            .request(originalRequest)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody())
            .build()

        val newRequest = authenticator.authenticate(null, response)

        assertNull(newRequest)
    }

    @Test
    fun authenticate_with_failed_token_refresh_clears_tokens_and_returns_null() {
        val refreshToken = "invalid_refresh_token"

        every { tokenManager.getRefreshToken() } returns refreshToken
        every { tokenManager.clearTokens() } returns Unit

        coEvery {
            authApiService.refreshToken(TokenRefreshRequest(refreshToken))
        } returns RetrofitResponse.error(
            401,
            "Invalid refresh token".toResponseBody()
        )

        every { context.startActivity(any()) } returns Unit

        val originalRequest = Request.Builder()
            .url("https://example.com/api/test")
            .build()
        val response = Response.Builder()
            .request(originalRequest)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody())
            .build()

        val newRequest = authenticator.authenticate(null, response)

        assertNull(newRequest)
        verify { tokenManager.clearTokens() }
        verify { context.startActivity(any()) }
    }

    @Test
    fun authenticate_with_null_response_body_clears_tokens() {
        val refreshToken = "refresh_token"

        every { tokenManager.getRefreshToken() } returns refreshToken
        every { tokenManager.clearTokens() } returns Unit

        coEvery {
            authApiService.refreshToken(TokenRefreshRequest(refreshToken))
        } returns RetrofitResponse.success(null)

        every { context.startActivity(any()) } returns Unit

        val originalRequest = Request.Builder()
            .url("https://example.com/api/test")
            .build()
        val response = Response.Builder()
            .request(originalRequest)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody())
            .build()

        val newRequest = authenticator.authenticate(null, response)

        assertNull(newRequest)
        verify { tokenManager.clearTokens() }
        verify { context.startActivity(any()) }
    }
}
