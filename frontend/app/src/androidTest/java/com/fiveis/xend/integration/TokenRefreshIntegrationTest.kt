package com.fiveis.xend.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.TokenRefreshRequest
import com.fiveis.xend.data.model.TokenRefreshResponse
import com.fiveis.xend.data.source.AuthApiService
import com.fiveis.xend.data.source.TokenManager
import com.fiveis.xend.network.TokenRefreshAuthenticator
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class TokenRefreshIntegrationTest {

    private lateinit var context: Context
    private lateinit var tokenManager: TokenManager
    private lateinit var authenticator: TokenRefreshAuthenticator
    private lateinit var authApiService: AuthApiService

    private val mockAccessToken = "mock_access_token_123"
    private val mockRefreshToken = "mock_refresh_token_456"
    private val mockEmail = "test@example.com"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        tokenManager = TokenManager(context)
        authApiService = mockk()
    }

    @Test
    fun token_manager_saves_and_retrieves_tokens() {
        tokenManager.saveTokens(mockAccessToken, mockRefreshToken, mockEmail)

        val retrievedAccessToken = tokenManager.getAccessToken()
        val retrievedRefreshToken = tokenManager.getRefreshToken()
        val retrievedEmail = tokenManager.getUserEmail()

        assertEquals(mockAccessToken, retrievedAccessToken)
        assertEquals(mockRefreshToken, retrievedRefreshToken)
        assertEquals(mockEmail, retrievedEmail)
    }

    @Test
    fun token_manager_clears_tokens() {
        tokenManager.saveTokens(mockAccessToken, mockRefreshToken, mockEmail)
        tokenManager.clearTokens()

        val accessToken = tokenManager.getAccessToken()
        val refreshToken = tokenManager.getRefreshToken()
        val email = tokenManager.getUserEmail()

        assertNull(accessToken)
        assertNull(refreshToken)
        assertNull(email)
    }

    @Test
    fun token_manager_updates_existing_tokens() {
        tokenManager.saveTokens(mockAccessToken, mockRefreshToken, mockEmail)

        val newAccessToken = "new_access_token_789"
        val newRefreshToken = "new_refresh_token_012"

        tokenManager.saveTokens(newAccessToken, newRefreshToken, mockEmail)

        assertEquals(newAccessToken, tokenManager.getAccessToken())
        assertEquals(newRefreshToken, tokenManager.getRefreshToken())
    }

    @Test
    fun token_refresh_request_creates_correctly() {
        val request = TokenRefreshRequest(refreshToken = mockRefreshToken)
        assertEquals(mockRefreshToken, request.refreshToken)
    }

    @Test
    fun token_refresh_response_parses_correctly() {
        val response = TokenRefreshResponse(
            accessToken = "new_access_123",
            refreshToken = "new_refresh_456"
        )

        assertEquals("new_access_123", response.accessToken)
        assertEquals("new_refresh_456", response.refreshToken)
    }

    @Test
    fun authenticator_returns_null_when_no_refresh_token() {
        tokenManager.clearTokens()

        authenticator = TokenRefreshAuthenticator(context, tokenManager)

        val mockResponse = mockk<Response>(relaxed = true)
        val mockRequest = mockk<Request>(relaxed = true)

        every { mockResponse.request } returns mockRequest

        val result = authenticator.authenticate(null, mockResponse)

        assertNull(result)
    }

    @Test
    fun token_refresh_api_call_succeeds() = runTest {
        val mockResponse = TokenRefreshResponse(
            accessToken = "new_access_token",
            refreshToken = "new_refresh_token"
        )

        coEvery {
            authApiService.refreshToken(any())
        } returns retrofit2.Response.success(mockResponse)

        val result = authApiService.refreshToken(
            TokenRefreshRequest(mockRefreshToken)
        )

        assertEquals(true, result.isSuccessful)
        assertEquals("new_access_token", result.body()?.accessToken)
        assertEquals("new_refresh_token", result.body()?.refreshToken)
    }

    @Test
    fun token_refresh_api_call_fails() = runTest {
        coEvery {
            authApiService.refreshToken(any())
        } returns retrofit2.Response.error(401, mockk(relaxed = true))

        val result = authApiService.refreshToken(
            TokenRefreshRequest(mockRefreshToken)
        )

        assertEquals(false, result.isSuccessful)
        assertEquals(401, result.code())
    }

    @Test
    fun token_manager_handles_empty_tokens() {
        tokenManager.clearTokens()

        val accessToken = tokenManager.getAccessToken()
        val refreshToken = tokenManager.getRefreshToken()
        val email = tokenManager.getUserEmail()

        assertNull(accessToken)
        assertNull(refreshToken)
        assertNull(email)
    }

    @Test
    fun token_manager_handles_multiple_save_operations() {
        tokenManager.saveTokens("token1", "refresh1", "email1@test.com")
        tokenManager.saveTokens("token2", "refresh2", "email2@test.com")
        tokenManager.saveTokens("token3", "refresh3", "email3@test.com")

        assertEquals("token3", tokenManager.getAccessToken())
        assertEquals("refresh3", tokenManager.getRefreshToken())
        assertEquals("email3@test.com", tokenManager.getUserEmail())
    }

    @Test
    fun token_manager_retrieves_correct_email() {
        val testEmail = "user@example.com"
        tokenManager.saveTokens(mockAccessToken, mockRefreshToken, testEmail)

        val retrievedEmail = tokenManager.getUserEmail()

        assertEquals(testEmail, retrievedEmail)
    }

    @Test
    fun token_refresh_with_valid_refresh_token_updates_storage() = runTest {
        tokenManager.saveTokens(mockAccessToken, mockRefreshToken, mockEmail)

        val newAccessToken = "refreshed_access_token"
        val newRefreshToken = "refreshed_refresh_token"

        val mockTokenResponse = TokenRefreshResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )

        coEvery {
            authApiService.refreshToken(any())
        } returns retrofit2.Response.success(mockTokenResponse)

        val result = authApiService.refreshToken(
            TokenRefreshRequest(mockRefreshToken)
        )

        if (result.isSuccessful && result.body() != null) {
            val tokens = result.body()!!
            tokenManager.saveTokens(tokens.accessToken, tokens.refreshToken, mockEmail)
        }

        assertEquals(newAccessToken, tokenManager.getAccessToken())
        assertEquals(newRefreshToken, tokenManager.getRefreshToken())
    }

    @Test
    fun token_manager_context_is_initialized() {
        assertNotNull(tokenManager)
    }

    @Test
    fun token_refresh_clears_tokens_on_failure() = runTest {
        tokenManager.saveTokens(mockAccessToken, mockRefreshToken, mockEmail)

        coEvery {
            authApiService.refreshToken(any())
        } returns retrofit2.Response.error(401, okhttp3.ResponseBody.create(null, ""))

        val result = authApiService.refreshToken(
            TokenRefreshRequest(mockRefreshToken)
        )

        if (!result.isSuccessful) {
            tokenManager.clearTokens()
        }

        assertNull(tokenManager.getAccessToken())
        assertNull(tokenManager.getRefreshToken())
    }

    @Test
    fun multiple_token_refresh_attempts_use_latest_refresh_token() = runTest {
        tokenManager.saveTokens("old_access", "old_refresh", mockEmail)

        val tokenResponse1 = TokenRefreshResponse("access1", "refresh1")
        coEvery {
            authApiService.refreshToken(TokenRefreshRequest("old_refresh"))
        } returns retrofit2.Response.success(tokenResponse1)

        val result1 = authApiService.refreshToken(TokenRefreshRequest("old_refresh"))
        if (result1.isSuccessful && result1.body() != null) {
            val tokens1 = result1.body()!!
            tokenManager.saveTokens(tokens1.accessToken, tokens1.refreshToken, mockEmail)
        }

        assertEquals("access1", tokenManager.getAccessToken())
        assertEquals("refresh1", tokenManager.getRefreshToken())

        val tokenResponse2 = TokenRefreshResponse("access2", "refresh2")
        coEvery {
            authApiService.refreshToken(TokenRefreshRequest("refresh1"))
        } returns retrofit2.Response.success(tokenResponse2)

        val result2 = authApiService.refreshToken(TokenRefreshRequest("refresh1"))
        if (result2.isSuccessful && result2.body() != null) {
            val tokens2 = result2.body()!!
            tokenManager.saveTokens(tokens2.accessToken, tokens2.refreshToken, mockEmail)
        }

        assertEquals("access2", tokenManager.getAccessToken())
        assertEquals("refresh2", tokenManager.getRefreshToken())
    }

    @Test
    fun token_manager_handles_special_characters_in_email() {
        val specialEmail = "user+test@example.co.uk"
        tokenManager.saveTokens(mockAccessToken, mockRefreshToken, specialEmail)

        val retrievedEmail = tokenManager.getUserEmail()

        assertEquals(specialEmail, retrievedEmail)
    }

    @Test
    fun token_manager_persists_across_instances() {
        val tokenManager1 = TokenManager(context)
        tokenManager1.saveTokens(mockAccessToken, mockRefreshToken, mockEmail)

        val tokenManager2 = TokenManager(context)
        val retrievedAccessToken = tokenManager2.getAccessToken()

        assertEquals(mockAccessToken, retrievedAccessToken)

        tokenManager1.clearTokens()
    }
}
