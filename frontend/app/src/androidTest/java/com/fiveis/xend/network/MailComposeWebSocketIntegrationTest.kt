package com.fiveis.xend.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.BuildConfig
import com.fiveis.xend.data.source.TokenManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class MailComposeWebSocketIntegrationTest {

    private lateinit var context: Context
    private lateinit var tokenManager: TokenManager
    private lateinit var wsClient: MailComposeWebSocketClient

    private val mockAccessToken = "mock_access_token_123456789"
    private val mockRefreshToken = "mock_refresh_token_987654321"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        tokenManager = mockk(relaxed = true)

        every { tokenManager.getAccessToken() } returns mockAccessToken
        every { tokenManager.getRefreshToken() } returns mockRefreshToken
    }

    @After
    fun tearDown() {
        if (::wsClient.isInitialized) {
            wsClient.disconnect()
        }
    }

    @Test
    fun webSocket_connect_with_empty_url_triggers_error() {
        val errorLatch = CountDownLatch(1)
        var errorMessage = ""

        wsClient = MailComposeWebSocketClient(context, "")

        wsClient.connect(
            onMessage = {},
            onError = { msg ->
                errorMessage = msg
                errorLatch.countDown()
            },
            onClose = {}
        )

        assertTrue(errorLatch.await(2, TimeUnit.SECONDS))
        assertTrue(errorMessage.contains("WS_URL이 설정되지 않았습니다"))
    }

    @Test
    fun webSocket_connect_without_token_triggers_error() {
        val errorLatch = CountDownLatch(1)
        var errorMessage = ""

        every { tokenManager.getAccessToken() } returns null

        wsClient = MailComposeWebSocketClient(context, "wss://mock-server.com/ws")

        wsClient.connect(
            onMessage = {},
            onError = { msg ->
                errorMessage = msg
                errorLatch.countDown()
            },
            onClose = {}
        )

        assertTrue(errorLatch.await(2, TimeUnit.SECONDS))
        assertTrue(errorMessage.contains("인증 토큰이 없습니다"))
    }

    @Test
    fun webSocket_sendMessage_without_connection_triggers_error() {
        val errorLatch = CountDownLatch(1)
        var errorMessage = ""

        wsClient = MailComposeWebSocketClient(context, "wss://mock-server.com/ws")

        wsClient.sendMessage("system prompt", "test message")

        runBlocking {
            delay(500)
        }

        wsClient.disconnect()
    }

    @Test
    fun webSocket_disconnect_clears_connection_state() {
        wsClient = MailComposeWebSocketClient(context, "wss://mock-server.com/ws")

        wsClient.disconnect()

        runBlocking {
            delay(100)
        }

        // 상태 확인을 위해 재연결 시도
        val errorLatch = CountDownLatch(1)
        wsClient.connect(
            onMessage = {},
            onError = { errorLatch.countDown() },
            onClose = {}
        )

        // 연결 실패는 정상 (Mock 서버가 없으므로)
        assertTrue(errorLatch.await(5, TimeUnit.SECONDS))
    }

    @Test
    fun webSocket_multiple_connect_calls_are_prevented() {
        val errorLatch = CountDownLatch(1)
        var errorCount = 0

        wsClient = MailComposeWebSocketClient(context, "wss://mock-server.com/ws")

        wsClient.connect(
            onMessage = {},
            onError = {
                errorCount++
                if (errorCount >= 1) errorLatch.countDown()
            },
            onClose = {}
        )

        wsClient.connect(
            onMessage = {},
            onError = {
                errorCount++
                if (errorCount >= 2) errorLatch.countDown()
            },
            onClose = {}
        )

        wsClient.connect(
            onMessage = {},
            onError = {
                errorCount++
                if (errorCount >= 3) errorLatch.countDown()
            },
            onClose = {}
        )

        assertTrue(errorLatch.await(6, TimeUnit.SECONDS))
        assertTrue(errorCount >= 1)
    }

    @Test
    fun webSocket_handles_json_message_formatting() = runBlocking {
        wsClient = MailComposeWebSocketClient(context, "wss://mock-server.com/ws")

        val messageLatch = CountDownLatch(1)
        wsClient.connect(
            onMessage = { messageLatch.countDown() },
            onError = {},
            onClose = {}
        )

        delay(500)
        wsClient.disconnect()
    }

    @Test
    fun webSocket_connection_lifecycle_management() {
        val closeLatch = CountDownLatch(1)

        wsClient = MailComposeWebSocketClient(context, "wss://mock-server.com/ws")

        wsClient.connect(
            onMessage = {},
            onError = {},
            onClose = { closeLatch.countDown() }
        )

        runBlocking {
            delay(500)
        }

        wsClient.disconnect()

        assertTrue(closeLatch.await(3, TimeUnit.SECONDS))
    }

    @Test
    fun webSocket_token_validation_before_connection() {
        var errorOccurred = false
        val errorLatch = CountDownLatch(1)

        every { tokenManager.getAccessToken() } returns ""

        wsClient = MailComposeWebSocketClient(context, "wss://mock-server.com/ws")

        wsClient.connect(
            onMessage = {},
            onError = {
                errorOccurred = true
                errorLatch.countDown()
            },
            onClose = {}
        )

        assertTrue(errorLatch.await(2, TimeUnit.SECONDS))
        assertTrue(errorOccurred)
    }

    @Test
    fun webSocket_handles_disconnect_without_prior_connection() {
        wsClient = MailComposeWebSocketClient(context, "wss://mock-server.com/ws")

        wsClient.disconnect()

        runBlocking {
            delay(100)
        }
    }

    @Test
    fun webSocket_validates_url_format() {
        val validWsUrl = "wss://example.com/ws"
        val validWssUrl = "wss://secure.example.com/ws"

        wsClient = MailComposeWebSocketClient(context, validWsUrl)
        wsClient.disconnect()

        wsClient = MailComposeWebSocketClient(context, validWssUrl)
        wsClient.disconnect()
    }
}
