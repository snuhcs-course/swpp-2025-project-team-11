package com.fiveis.xend.network

import android.content.Context
import io.mockk.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MailComposeWebSocketClientTest {

    private lateinit var mockContext: Context
    private lateinit var mockClient: OkHttpClient
    private lateinit var mockWebSocket: WebSocket
    private lateinit var client: MailComposeWebSocketClient

    private val wsUrl = "ws://fake.url/ws"

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockClient = mockk()
        mockWebSocket = mockk(relaxed = true)

        mockkObject(RetrofitClient)
        every { RetrofitClient.getWebSocketClient(any()) } returns mockClient
        // Default relaxed mock for authApiService to avoid IllegalStateException
        val mockAuthService = mockk<com.fiveis.xend.data.source.AuthApiService>(relaxed = true)
        every { RetrofitClient.authApiService } returns mockAuthService

        // Mock TokenManager to return empty tokens (will trigger error)
        mockkConstructor(com.fiveis.xend.data.source.TokenManager::class)
        every { anyConstructed<com.fiveis.xend.data.source.TokenManager>().getAccessToken() } returns null

        client = MailComposeWebSocketClient(mockContext, wsUrl)
    }

    @After
    fun tear_down() {
        unmockkAll()
    }

    @Test
    fun connect_with_blank_url_invokes_error() {
        client = MailComposeWebSocketClient(mockContext, "")
        var error: String? = null

        client.connect({}, { e -> error = e }, {})

        assertNotNull(error)
        assertTrue(error!!.contains("WS_URL"))
    }

    @Test
    fun connect_creates_new_websocket() {
        // Mock TokenManager to return a valid token
        every { anyConstructed<com.fiveis.xend.data.source.TokenManager>().getAccessToken() } returns "valid-token"

        val listenerSlot = slot<WebSocketListener>()
        every { mockClient.newWebSocket(any(), capture(listenerSlot)) } returns mockWebSocket

        client.connect({}, {}, {})

        verify { mockClient.newWebSocket(any(), any()) }
    }

    @Test
    fun send_message_when_not_connected_invokes_error() {
        every { mockClient.newWebSocket(any(), any()) } returns mockWebSocket

        var error: String? = null
        client.connect({}, { e -> error = e }, {})

        client.sendMessage(systemPrompt = "prompt", text = "text", subject = "")

        assertNotNull(error)
        // Check if error message contains "WebSocket" or "연결"
        assertTrue(error!!.contains("WebSocket") || error.contains("연결"))
    }

    @Test
    fun send_message_when_connected_sends_json() {
        // Mock TokenManager to return a valid token
        every { anyConstructed<com.fiveis.xend.data.source.TokenManager>().getAccessToken() } returns "valid-token"

        val listenerSlot = slot<WebSocketListener>()
        every { mockClient.newWebSocket(any(), capture(listenerSlot)) } returns mockWebSocket
        every { mockWebSocket.send(any<String>()) } returns true

        client.connect({}, {}, {})
        listenerSlot.captured.onOpen(mockWebSocket, mockk(relaxed = true))

        client.sendMessage(systemPrompt = "prompt", text = "user text", subject = "test subject")

        val expectedJson = JSONObject()
            .put("system_prompt", "prompt")
            .put("text", "user text")
            .put("subject", "test subject")
            .put("max_tokens", 50)
            .toString()

        verify { mockWebSocket.send(expectedJson) }
    }

    @Test
    fun disconnect_closes_websocket() {
        // Mock TokenManager to return a valid token
        every { anyConstructed<com.fiveis.xend.data.source.TokenManager>().getAccessToken() } returns "valid-token"

        every { mockClient.newWebSocket(any(), any()) } returns mockWebSocket
        client.connect({}, {}, {})

        client.disconnect()

        verify { mockWebSocket.close(1000, "User closed") }
    }

    @Test
    fun listener_on_message_invokes_callback() {
        // Mock TokenManager to return a valid token
        every { anyConstructed<com.fiveis.xend.data.source.TokenManager>().getAccessToken() } returns "valid-token"

        val listenerSlot = slot<WebSocketListener>()
        every { mockClient.newWebSocket(any(), capture(listenerSlot)) } returns mockWebSocket

        var receivedMessage: String? = null
        client.connect({ m -> receivedMessage = m }, {}, {})

        listenerSlot.captured.onMessage(mockWebSocket, "Hello, world!")

        assertEquals("Hello, world!", receivedMessage)
    }

    @Test
    fun listener_on_failure_invokes_error_callback() {
        // Mock TokenManager to return a valid token
        every { anyConstructed<com.fiveis.xend.data.source.TokenManager>().getAccessToken() } returns "valid-token"

        val listenerSlot = slot<WebSocketListener>()
        every { mockClient.newWebSocket(any(), capture(listenerSlot)) } returns mockWebSocket

        var error: String? = null
        client.connect({}, { e -> error = e }, {})

        listenerSlot.captured.onFailure(mockWebSocket, Exception("Connection failed"), null)

        // The error callback should be invoked with the failure message
        assertNotNull(error)
        assertTrue(error!!.contains("Connection failed"))
    }

    @Test
    fun listener_on_closed_invokes_close_callback() {
        // Mock TokenManager to return a valid token
        every { anyConstructed<com.fiveis.xend.data.source.TokenManager>().getAccessToken() } returns "valid-token"

        val listenerSlot = slot<WebSocketListener>()
        every { mockClient.newWebSocket(any(), capture(listenerSlot)) } returns mockWebSocket

        var closed = false
        client.connect({}, {}, { closed = true })

        listenerSlot.captured.onClosed(mockWebSocket, 1000, "Normal closure")

        assertTrue(closed)
    }

    @Test
    fun onMessage_with_token_invalid_error_and_successful_refresh_reconnects() {
        // Mock TokenManager
        every { anyConstructed<com.fiveis.xend.data.source.TokenManager>().getAccessToken() } returns "valid-token"
        every { anyConstructed<com.fiveis.xend.data.source.TokenManager>().getRefreshToken() } returns "refresh-token"
        every { anyConstructed<com.fiveis.xend.data.source.TokenManager>().getUserEmail() } returns "test@example.com"
        every { anyConstructed<com.fiveis.xend.data.source.TokenManager>().saveTokens(any(), any(), any()) } just Runs

        val mockTokenResponse = com.fiveis.xend.data.model.TokenRefreshResponse(
            accessToken = "new-access-token",
            refreshToken = "new-refresh-token"
        )
        // Mock the behavior of the GLOBAL mock defined in setup()
        coEvery { RetrofitClient.authApiService.refreshToken(any()) } returns retrofit2.Response.success(mockTokenResponse)

        val listenerSlot = slot<WebSocketListener>()
        every { mockClient.newWebSocket(any(), capture(listenerSlot)) } returns mockWebSocket
        every { mockWebSocket.close(any(), any()) } returns true

        client.connect({}, {}, {})

        // Simulate token_invalid error message
        val errorJson = """{"type":"error","message":"token_invalid"}"""

        listenerSlot.captured.onMessage(mockWebSocket, errorJson)

        // Should save new tokens
        verify { anyConstructed<com.fiveis.xend.data.source.TokenManager>().saveTokens("new-access-token", "new-refresh-token", "test@example.com") }
    }

    @Test
    fun onMessage_with_token_invalid_error_and_no_refresh_token_fails() {
        every { anyConstructed<com.fiveis.xend.data.source.TokenManager>().getAccessToken() } returns "valid-token"
        every { anyConstructed<com.fiveis.xend.data.source.TokenManager>().getRefreshToken() } returns null

        val listenerSlot = slot<WebSocketListener>()
        every { mockClient.newWebSocket(any(), capture(listenerSlot)) } returns mockWebSocket
        every { mockWebSocket.close(any(), any()) } returns true

        var errorMessage: String? = null
        client.connect({}, { err -> errorMessage = err }, {})

        val errorJson = """{"type":"error","message":"token_invalid"}"""
        listenerSlot.captured.onMessage(mockWebSocket, errorJson)

        assertNotNull(errorMessage)
        assertTrue(errorMessage!!.contains("인증이 만료되었습니다") || errorMessage.contains("다시 로그인"))
    }

    @Test
    fun onClosing_with_1008_code_and_successful_refresh_reconnects() {
        every { anyConstructed<com.fiveis.xend.data.source.TokenManager>().getAccessToken() } returns "valid-token"
        every { anyConstructed<com.fiveis.xend.data.source.TokenManager>().getRefreshToken() } returns "refresh-token"
        every { anyConstructed<com.fiveis.xend.data.source.TokenManager>().getUserEmail() } returns "test@example.com"
        every { anyConstructed<com.fiveis.xend.data.source.TokenManager>().saveTokens(any(), any(), any()) } just Runs

        val mockTokenResponse = com.fiveis.xend.data.model.TokenRefreshResponse(
            accessToken = "new-access-token",
            refreshToken = "new-refresh-token"
        )
        coEvery { RetrofitClient.authApiService.refreshToken(any()) } returns retrofit2.Response.success(mockTokenResponse)

        val listenerSlot = slot<WebSocketListener>()
        every { mockClient.newWebSocket(any(), capture(listenerSlot)) } returns mockWebSocket
        every { mockWebSocket.close(any(), any()) } returns true

        client.connect({}, {}, {})

        listenerSlot.captured.onClosing(mockWebSocket, 1008, "Policy Violation")

        verify { anyConstructed<com.fiveis.xend.data.source.TokenManager>().saveTokens("new-access-token", "new-refresh-token", "test@example.com") }
    }

    @Test
    fun onMessage_with_normal_json_invokes_message_callback() {
        every { anyConstructed<com.fiveis.xend.data.source.TokenManager>().getAccessToken() } returns "valid-token"

        val listenerSlot = slot<WebSocketListener>()
        every { mockClient.newWebSocket(any(), capture(listenerSlot)) } returns mockWebSocket

        var receivedMessage: String? = null
        client.connect({ msg -> receivedMessage = msg }, {}, {})

        val normalMessage = """{"type":"data","content":"hello"}"""
        listenerSlot.captured.onMessage(mockWebSocket, normalMessage)

        assertEquals(normalMessage, receivedMessage)
    }

    @Test
    fun onMessage_with_error_type_but_not_token_invalid_invokes_error_callback() {
        every { anyConstructed<com.fiveis.xend.data.source.TokenManager>().getAccessToken() } returns "valid-token"

        val listenerSlot = slot<WebSocketListener>()
        every { mockClient.newWebSocket(any(), capture(listenerSlot)) } returns mockWebSocket

        var errorMessage: String? = null
        client.connect({}, { err -> errorMessage = err }, {})

        val errorJson = """{"type":"error","message":"some_other_error"}"""
        listenerSlot.captured.onMessage(mockWebSocket, errorJson)

        assertNotNull(errorMessage)
        assertTrue(errorMessage!!.contains("some_other_error"))
    }

    @Test
    fun onMessage_with_non_json_invokes_message_callback() {
        every { anyConstructed<com.fiveis.xend.data.source.TokenManager>().getAccessToken() } returns "valid-token"

        val listenerSlot = slot<WebSocketListener>()
        every { mockClient.newWebSocket(any(), capture(listenerSlot)) } returns mockWebSocket

        var receivedMessage: String? = null
        client.connect({ msg -> receivedMessage = msg }, {}, {})

        val plainText = "plain text message"
        listenerSlot.captured.onMessage(mockWebSocket, plainText)

        assertEquals(plainText, receivedMessage)
    }

    @Test
    fun onFailure_with_401_and_successful_refresh_reconnects() {
        every { anyConstructed<com.fiveis.xend.data.source.TokenManager>().getAccessToken() } returns "valid-token"
        every { anyConstructed<com.fiveis.xend.data.source.TokenManager>().getRefreshToken() } returns "refresh-token"
        every { anyConstructed<com.fiveis.xend.data.source.TokenManager>().getUserEmail() } returns "test@example.com"
        every { anyConstructed<com.fiveis.xend.data.source.TokenManager>().saveTokens(any(), any(), any()) } just Runs

        val mockTokenResponse = com.fiveis.xend.data.model.TokenRefreshResponse(
            accessToken = "new-access-token",
            refreshToken = "new-refresh-token"
        )
        coEvery { RetrofitClient.authApiService.refreshToken(any()) } returns retrofit2.Response.success(mockTokenResponse)

        val listenerSlot = slot<WebSocketListener>()
        every { mockClient.newWebSocket(any(), capture(listenerSlot)) } returns mockWebSocket

        client.connect({}, {}, {})

        val mockResponse = mockk<okhttp3.Response>(relaxed = true)
        every { mockResponse.code } returns 401

        listenerSlot.captured.onFailure(mockWebSocket, Exception("Unauthorized"), mockResponse)

        verify { anyConstructed<com.fiveis.xend.data.source.TokenManager>().saveTokens("new-access-token", "new-refresh-token", "test@example.com") }
    }

    @Test
    fun onFailure_without_401_invokes_error_callback() {
        every { anyConstructed<com.fiveis.xend.data.source.TokenManager>().getAccessToken() } returns "valid-token"

        val listenerSlot = slot<WebSocketListener>()
        every { mockClient.newWebSocket(any(), capture(listenerSlot)) } returns mockWebSocket

        var errorMessage: String? = null
        client.connect({}, { err -> errorMessage = err }, {})

        val mockResponse = mockk<okhttp3.Response>(relaxed = true)
        every { mockResponse.code } returns 500

        listenerSlot.captured.onFailure(mockWebSocket, Exception("Server Error"), mockResponse)

        assertNotNull(errorMessage)
        assertTrue(errorMessage!!.contains("연결 실패"))
    }
}
