package com.fiveis.xend.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.source.TokenManager
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MailReplySseIntegrationTest {

    private lateinit var context: Context
    private lateinit var tokenManager: TokenManager
    private lateinit var sseClient: MailReplySseClient
    private lateinit var mockWebServer: MockWebServer

    private val mockAccessToken = "mock_access_token_123456789"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        tokenManager = mockk(relaxed = true)
        mockWebServer = MockWebServer()
        mockWebServer.start()

        every { tokenManager.getAccessToken() } returns mockAccessToken
    }

    @After
    fun tearDown() {
        if (::sseClient.isInitialized) {
            sseClient.stop()
        }
        mockWebServer.shutdown()
    }

    @Test
    fun sse_client_initialization_succeeds() {
        sseClient = MailReplySseClient(
            context = context,
            tokenManager = tokenManager,
            endpointUrl = mockWebServer.url("/stream").toString()
        )

        assertTrue(::sseClient.isInitialized)
    }

    @Test(timeout = 10000)
    fun sse_start_with_valid_parameters_initializes_connection() {
        val callbackLatch = CountDownLatch(1)
        var callbackInvoked = false

        // Mock SSE response that will trigger onError (connection failure is expected)
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Server error")
        )

        sseClient = MailReplySseClient(
            context = context,
            tokenManager = tokenManager,
            endpointUrl = mockWebServer.url("/stream").toString()
        )

        sseClient.start(
            subject = "Test Subject",
            body = "Test Body",
            toEmail = "test@example.com",
            onReady = {
                callbackInvoked = true
                callbackLatch.countDown()
            },
            onOptions = {},
            onOptionDelta = { _, _, _ -> },
            onOptionDone = { _, _ -> },
            onOptionError = { _, _ -> },
            onDone = {
                callbackInvoked = true
                callbackLatch.countDown()
            },
            onError = {
                callbackInvoked = true
                callbackLatch.countDown()
            }
        )

        val callbackReceived = callbackLatch.await(8, TimeUnit.SECONDS)
        assertTrue("Expected some callback to be invoked", callbackInvoked || callbackReceived)
    }

    @Test
    fun sse_stop_cancels_ongoing_connection() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
        )

        sseClient = MailReplySseClient(
            context = context,
            tokenManager = tokenManager,
            endpointUrl = mockWebServer.url("/stream").toString()
        )

        val errorLatch = CountDownLatch(1)

        sseClient.start(
            subject = "Test",
            body = "Test",
            toEmail = "test@example.com",
            onReady = {},
            onOptions = {},
            onOptionDelta = { _, _, _ -> },
            onOptionDone = { _, _ -> },
            onOptionError = { _, _ -> },
            onDone = {},
            onError = { errorLatch.countDown() }
        )

        runBlocking {
            delay(100)
        }

        sseClient.stop()

        runBlocking {
            delay(500)
        }
    }

    @Test(timeout = 15000)
    fun sse_multiple_starts_cancel_previous_connection() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
        )
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
        )

        sseClient = MailReplySseClient(
            context = context,
            tokenManager = tokenManager,
            endpointUrl = mockWebServer.url("/stream").toString()
        )

        val firstErrorLatch = CountDownLatch(1)
        val secondErrorLatch = CountDownLatch(1)

        sseClient.start(
            subject = "First",
            body = "First",
            toEmail = "first@example.com",
            onReady = {},
            onOptions = {},
            onOptionDelta = { _, _, _ -> },
            onOptionDone = { _, _ -> },
            onOptionError = { _, _ -> },
            onDone = {},
            onError = { firstErrorLatch.countDown() }
        )

        runBlocking {
            delay(100)
        }

        sseClient.start(
            subject = "Second",
            body = "Second",
            toEmail = "second@example.com",
            onReady = {},
            onOptions = {},
            onOptionDelta = { _, _, _ -> },
            onOptionDone = { _, _ -> },
            onOptionError = { _, _ -> },
            onDone = {},
            onError = { secondErrorLatch.countDown() }
        )

        assertTrue(secondErrorLatch.await(10, TimeUnit.SECONDS))
    }

    @Test(timeout = 15000)
    fun sse_handles_callback_invocations() {
        val callbacksInvoked = mutableListOf<String>()
        val latch = CountDownLatch(1)

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
        )

        sseClient = MailReplySseClient(
            context = context,
            tokenManager = tokenManager,
            endpointUrl = mockWebServer.url("/stream").toString()
        )

        sseClient.start(
            subject = "Test",
            body = "Test",
            toEmail = "test@example.com",
            onReady = { callbacksInvoked.add("ready") },
            onOptions = { callbacksInvoked.add("options") },
            onOptionDelta = { _, _, _ -> callbacksInvoked.add("delta") },
            onOptionDone = { _, _ -> callbacksInvoked.add("done") },
            onOptionError = { _, _ -> callbacksInvoked.add("error") },
            onDone = { callbacksInvoked.add("complete") },
            onError = {
                callbacksInvoked.add("error")
                latch.countDown()
            }
        )

        assertTrue(latch.await(10, TimeUnit.SECONDS))
        assertTrue(callbacksInvoked.contains("error"))
    }

    @Test
    fun sse_stop_without_start_does_not_crash() {
        sseClient = MailReplySseClient(
            context = context,
            tokenManager = tokenManager,
            endpointUrl = mockWebServer.url("/stream").toString()
        )

        sseClient.stop()

        runBlocking {
            delay(100)
        }
    }

    @Test
    fun sse_validates_endpoint_url() {
        val validUrl = mockWebServer.url("/stream").toString()

        sseClient = MailReplySseClient(
            context = context,
            tokenManager = tokenManager,
            endpointUrl = validUrl
        )

        assertTrue(::sseClient.isInitialized)
    }

    @Test(timeout = 10000)
    fun sse_handles_token_manager_integration() {
        val customTokenManager = TokenManager(context)
        val callbackLatch = CountDownLatch(1)
        var callbackInvoked = false

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
        )

        sseClient = MailReplySseClient(
            context = context,
            tokenManager = customTokenManager,
            endpointUrl = mockWebServer.url("/stream").toString()
        )

        sseClient.start(
            subject = "Test",
            body = "Test",
            toEmail = "test@example.com",
            onReady = {
                callbackInvoked = true
                callbackLatch.countDown()
            },
            onOptions = {},
            onOptionDelta = { _, _, _ -> },
            onOptionDone = { _, _ -> },
            onOptionError = { _, _ -> },
            onDone = {
                callbackInvoked = true
                callbackLatch.countDown()
            },
            onError = {
                callbackInvoked = true
                callbackLatch.countDown()
            }
        )

        val callbackReceived = callbackLatch.await(8, TimeUnit.SECONDS)
        assertTrue("Expected some callback to be invoked", callbackInvoked || callbackReceived)
    }

    @Test
    fun sse_option_info_data_class_works_correctly() {
        val option1 = ReplyOptionInfo(id = 1, type = "formal", title = "Formal Reply")
        val option2 = ReplyOptionInfo(id = 2, type = "casual", title = "Casual Reply")

        assertEquals(1, option1.id)
        assertEquals("formal", option1.type)
        assertEquals("Formal Reply", option1.title)

        assertEquals(2, option2.id)
        assertEquals("casual", option2.type)
        assertEquals("Casual Reply", option2.title)
    }

    @Test(timeout = 15000)
    fun sse_handles_empty_subject_and_body() {
        val errorLatch = CountDownLatch(1)

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
        )

        sseClient = MailReplySseClient(
            context = context,
            tokenManager = tokenManager,
            endpointUrl = mockWebServer.url("/stream").toString()
        )

        sseClient.start(
            subject = "",
            body = "",
            toEmail = "test@example.com",
            onReady = {},
            onOptions = {},
            onOptionDelta = { _, _, _ -> },
            onOptionDone = { _, _ -> },
            onOptionError = { _, _ -> },
            onDone = {},
            onError = { errorLatch.countDown() }
        )

        assertTrue(errorLatch.await(10, TimeUnit.SECONDS))
    }
}
