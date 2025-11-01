package com.fiveis.xend.network

import android.content.Context
import com.fiveis.xend.data.source.TokenManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MailReplySseClientTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var mockContext: Context
    private lateinit var mockTokenManager: TokenManager
    private lateinit var sseClient: MailReplySseClient

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        mockContext = mockk(relaxed = true)
        mockTokenManager = mockk(relaxed = true)

        every { mockTokenManager.getAccessToken() } returns "test_token"

        val url = mockWebServer.url("/api/ai/mail/reply/stream/").toString()
        sseClient = MailReplySseClient(
            context = mockContext,
            tokenManager = mockTokenManager,
            endpointUrl = url
        )
    }

    @After
    fun teardown() {
        sseClient.stop()
        mockWebServer.shutdown()
    }

    @Test
    fun `start should send POST request with payload`() = runBlocking {
        val sseResponse = """
            event: ready
            data: {}
            
            event: done
            data: {"reason":"finished"}
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(sseResponse).setResponseCode(200))

        var readyCalled = false
        var doneCalled = false

        sseClient.start(
            subject = "Test Subject",
            body = "Test Body",
            toEmail = "test@example.com",
            onReady = { readyCalled = true },
            onOptions = {},
            onOptionDelta = { _, _, _ -> },
            onOptionDone = { _, _ -> },
            onOptionError = { _, _ -> },
            onDone = { doneCalled = true },
            onError = {}
        )

        delay(1000)

        val request = mockWebServer.takeRequest()
        assert(request.path?.contains("/api/ai/mail/reply/stream/") == true)
        assert(request.method == "POST")
        verify { mockTokenManager.getAccessToken() }
    }

    @Test
    fun `start should handle options event`() = runBlocking {
        val sseResponse = """
            event: options
            data: {"count":2,"items":[{"id":1,"type":"formal","title":"Formal Reply"},{"id":2,"type":"casual","title":"Casual Reply"}]}
            
            event: done
            data: {"reason":"finished"}
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(sseResponse).setResponseCode(200))

        var optionsReceived: List<ReplyOptionInfo>? = null

        sseClient.start(
            subject = "Test",
            body = "Test",
            toEmail = "test@example.com",
            onReady = {},
            onOptions = { options -> optionsReceived = options },
            onOptionDelta = { _, _, _ -> },
            onOptionDone = { _, _ -> },
            onOptionError = { _, _ -> },
            onDone = {},
            onError = {}
        )

        delay(1000)
        assert(optionsReceived != null)
    }

    @Test
    fun `start should handle option delta event`() = runBlocking {
        val sseResponse = """
            event: option.delta
            data: {"id":1,"seq":0,"text":"Hello"}
            
            event: option.delta
            data: {"id":1,"seq":1,"text":" World"}
            
            event: done
            data: {"reason":"finished"}
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(sseResponse).setResponseCode(200))

        val deltas = mutableListOf<Triple<Int, Int, String>>()

        sseClient.start(
            subject = "Test",
            body = "Test",
            toEmail = "test@example.com",
            onReady = {},
            onOptions = {},
            onOptionDelta = { id, seq, text -> deltas.add(Triple(id, seq, text)) },
            onOptionDone = { _, _ -> },
            onOptionError = { _, _ -> },
            onDone = {},
            onError = {}
        )

        delay(1000)
        assert(deltas.isNotEmpty())
    }

    @Test
    fun `start should handle option done event`() = runBlocking {
        val sseResponse = """
            event: option.done
            data: {"id":1,"total_seq":10}
            
            event: done
            data: {"reason":"finished"}
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(sseResponse).setResponseCode(200))

        var optionDoneId: Int? = null
        var totalSeq: Int? = null

        sseClient.start(
            subject = "Test",
            body = "Test",
            toEmail = "test@example.com",
            onReady = {},
            onOptions = {},
            onOptionDelta = { _, _, _ -> },
            onOptionDone = { id, total ->
                optionDoneId = id
                totalSeq = total
            },
            onOptionError = { _, _ -> },
            onDone = {},
            onError = {}
        )

        delay(1000)
        assert(optionDoneId == 1)
        assert(totalSeq == 10)
    }

    @Test
    fun `start should handle option error event`() = runBlocking {
        val sseResponse = """
            event: option.error
            data: {"id":1,"message":"Generation failed"}
            
            event: done
            data: {"reason":"error"}
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(sseResponse).setResponseCode(200))

        var errorId: Int? = null
        var errorMessage: String? = null

        sseClient.start(
            subject = "Test",
            body = "Test",
            toEmail = "test@example.com",
            onReady = {},
            onOptions = {},
            onOptionDelta = { _, _, _ -> },
            onOptionDone = { _, _ -> },
            onOptionError = { id, message ->
                errorId = id
                errorMessage = message
            },
            onDone = {},
            onError = {}
        )

        delay(1000)
        assert(errorId == 1)
        assert(errorMessage == "Generation failed")
    }

    @Test
    fun `start should handle ping event`() = runBlocking {
        val sseResponse = """
            event: ping
            data: {}
            
            event: done
            data: {"reason":"finished"}
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(sseResponse).setResponseCode(200))

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
            onError = {}
        )

        delay(500)
    }

    @Test
    fun `start should handle HTTP error`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Server Error"))

        var errorReceived: String? = null

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
            onError = { error -> errorReceived = error }
        )

        delay(1000)
        assert(errorReceived?.contains("500") == true)
    }

    @Test
    fun `stop should cancel ongoing request`() = runBlocking {
        val sseResponse = """
            event: ready
            data: {}
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(sseResponse).setResponseCode(200))

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
            onError = {}
        )

        delay(200)
        sseClient.stop()
        delay(200)
    }

    @Test
    fun `start should stop previous connection before starting new one`() = runBlocking {
        val sseResponse1 = """
            event: ready
            data: {}
        """.trimIndent()

        val sseResponse2 = """
            event: done
            data: {"reason":"finished"}
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(sseResponse1).setResponseCode(200))
        mockWebServer.enqueue(MockResponse().setBody(sseResponse2).setResponseCode(200))

        sseClient.start(
            subject = "Test1",
            body = "Test1",
            toEmail = "test1@example.com",
            onReady = {},
            onOptions = {},
            onOptionDelta = { _, _, _ -> },
            onOptionDone = { _, _ -> },
            onOptionError = { _, _ -> },
            onDone = {},
            onError = {}
        )

        delay(300)

        sseClient.start(
            subject = "Test2",
            body = "Test2",
            toEmail = "test2@example.com",
            onReady = {},
            onOptions = {},
            onOptionDelta = { _, _, _ -> },
            onOptionDone = { _, _ -> },
            onOptionError = { _, _ -> },
            onDone = {},
            onError = {}
        )

        delay(500)

        assert(mockWebServer.requestCount == 2)
    }
}
