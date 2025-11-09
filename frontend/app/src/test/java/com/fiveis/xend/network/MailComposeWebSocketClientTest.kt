package com.fiveis.xend.network

import android.content.Context
import io.mockk.*
import okhttp3.*
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

        client.sendMessage("system", "text")

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

        client.sendMessage("system prompt", "user text")

        val expectedJson = JSONObject()
            .put("system_prompt", "system prompt")
            .put("text", "user text")
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
}
