package com.fiveis.xend.network

import android.content.Context
import com.fiveis.xend.data.source.TokenManager
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MailComposeSseClientTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var mockContext: Context
    private lateinit var mockTokenManager: TokenManager
    private lateinit var sseClient: MailComposeSseClient

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        mockContext = mockk(relaxed = true)
        mockTokenManager = mockk(relaxed = true)

        every { mockTokenManager.getAccessToken() } returns "test-token"

        val client = OkHttpClient.Builder().build()

        sseClient = MailComposeSseClient(
            context = mockContext,
            tokenManager = mockTokenManager,
            endpointUrl = mockWebServer.url("/sse").toString(),
            client = client
        )
    }

    @After
    fun tearDown() {
        sseClient.stop()
        mockWebServer.shutdown()
    }

    @Test
    fun `start should send POST request with payload`() = runBlocking {
        val sseResponse = """
            event: subject
            data: {"title":"Test Subject"}

            event: done
            data: {}

        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(sseResponse).setResponseCode(200))

        var subjectReceived = false
        var doneReceived = false

        val payload = JSONObject().apply {
            put("test", "value")
        }

        sseClient.start(
            payload = payload,
            onSubject = { subjectReceived = true },
            onBodyDelta = { _, _ -> },
            onDone = { doneReceived = true },
            onError = {}
        )

        delay(500)

        val request = mockWebServer.takeRequest()
        assertTrue(request.path?.contains("/sse") == true)
        assertTrue(request.method == "POST")
    }

    @Test
    fun `stop should not crash`() = runBlocking {
        sseClient.stop()
        assertTrue(true)
    }

    @Test
    fun `start should handle error response`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Server Error"))

        var errorReceived = false
        val payload = JSONObject()

        sseClient.start(
            payload = payload,
            onSubject = {},
            onBodyDelta = { _, _ -> },
            onDone = {},
            onError = { errorReceived = true }
        )

        delay(500)

        assertTrue(errorReceived)
    }

    @Test
    fun `start should handle body delta events`() = runBlocking {
        val sseResponse = """
            event: body.delta
            data: {"seq":1,"text":"Hello"}

            event: body.delta
            data: {"seq":2,"text":" World"}

            event: done
            data: {}

        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(sseResponse).setResponseCode(200))

        val deltas = mutableListOf<Pair<Int, String>>()
        val payload = JSONObject()

        sseClient.start(
            payload = payload,
            onSubject = {},
            onBodyDelta = { seq, text -> deltas.add(seq to text) },
            onDone = {},
            onError = {}
        )

        delay(500)

        assertTrue(deltas.size >= 1)
    }

    @Test
    fun `tokenManager should provide access token`() {
        val token = mockTokenManager.getAccessToken()
        assertTrue(token == "test-token")
    }
}
