package com.fiveis.xend.data.source

import com.fiveis.xend.data.model.AuthCodeRequest
import com.fiveis.xend.data.model.LogoutRequest
import com.fiveis.xend.data.model.TokenRefreshRequest
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AuthApiServiceTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: AuthApiService

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        apiService = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun test_sendAuthCode_success() = runTest {
        val responseBody = """
            {
                "jwt": {
                    "access": "access_token_123",
                    "refresh": "refresh_token_123"
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json")
        )

        val request = AuthCodeRequest(authCode = "auth_code_123")
        val response = apiService.sendAuthCode(request)

        assertEquals(true, response.isSuccessful)
        assertEquals("access_token_123", response.body()?.jwt?.access)
        assertEquals("refresh_token_123", response.body()?.jwt?.refresh)
    }

    @Test
    fun test_refreshToken_success() = runTest {
        val responseBody = """
            {
                "access": "new_access_token",
                "refresh": "new_refresh_token"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json")
        )

        val request = TokenRefreshRequest(refreshToken = "refresh_token_123")
        val response = apiService.refreshToken(request)

        assertEquals(true, response.isSuccessful)
        assertEquals("new_access_token", response.body()?.accessToken)
    }

    @Test
    fun test_logout_success() = runTest {
        val responseBody = """
            {
                "detail": "Successfully logged out"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json")
        )

        val request = LogoutRequest(refresh = "refresh_token_123")
        val response = apiService.logout(request)

        assertEquals(true, response.isSuccessful)
        assertEquals("Successfully logged out", response.body()?.detail)
    }

    @Test
    fun test_sendAuthCode_error() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("{\"error\": \"Invalid auth code\"}")
                .addHeader("Content-Type", "application/json")
        )

        val request = AuthCodeRequest(authCode = "invalid_code")
        val response = apiService.sendAuthCode(request)

        assertEquals(false, response.isSuccessful)
        assertEquals(401, response.code())
    }

    @Test
    fun test_refreshToken_error() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("{\"error\": \"Invalid refresh token\"}")
                .addHeader("Content-Type", "application/json")
        )

        val request = TokenRefreshRequest(refreshToken = "invalid_token")
        val response = apiService.refreshToken(request)

        assertEquals(false, response.isSuccessful)
        assertEquals(401, response.code())
    }
}
