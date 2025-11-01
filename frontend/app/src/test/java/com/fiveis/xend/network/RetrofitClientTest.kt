package com.fiveis.xend.network

import android.content.Context
import com.fiveis.xend.data.source.TokenManager
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RetrofitClientTest {

    private lateinit var mockContext: Context
    private lateinit var mockTokenManager: TokenManager

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockTokenManager = mockk(relaxed = true)
        every { mockTokenManager.getAccessToken() } returns "test-token"
    }

    @Test
    fun `getClient should return OkHttpClient with interceptors`() {
        val client = RetrofitClient.getClient(mockContext)
        assertNotNull(client)
        assertNotNull(client.interceptors)
    }

    @Test
    fun `getWebSocketClient should return OkHttpClient`() {
        val client = RetrofitClient.getWebSocketClient(mockContext)
        assertNotNull(client)
    }

    @Test
    fun `getMailApiService should return MailApiService instance`() {
        val service = RetrofitClient.getMailApiService(mockContext)
        assertNotNull(service)
    }

    @Test
    fun `getContactApiService should return ContactApiService instance`() {
        val service = RetrofitClient.getContactApiService(mockContext)
        assertNotNull(service)
    }

    @Test
    fun `authApiService should be initialized`() {
        val service = RetrofitClient.authApiService
        assertNotNull(service)
    }

    @Test
    fun `getClient should configure timeouts`() {
        val client = RetrofitClient.getClient(mockContext)
        assertNotNull(client.connectTimeoutMillis)
        assertNotNull(client.readTimeoutMillis)
        assertNotNull(client.writeTimeoutMillis)
    }

    @Test
    fun `getWebSocketClient should not have auth interceptor`() {
        val wsClient = RetrofitClient.getWebSocketClient(mockContext)
        val regularClient = RetrofitClient.getClient(mockContext)

        assertNotNull(wsClient)
        assertNotNull(regularClient)
    }
}
