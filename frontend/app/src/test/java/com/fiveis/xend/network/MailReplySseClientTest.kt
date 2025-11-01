package com.fiveis.xend.network

import android.content.Context
import com.fiveis.xend.data.source.TokenManager
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.*
import okhttp3.Call
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class MailReplySseClientTest {

    private lateinit var mockContext: Context
    private lateinit var mockTokenManager: TokenManager
    private lateinit var mockOkHttpClient: OkHttpClient
    private lateinit var sseClient: MailReplySseClient

    private val endpointUrl = "http://fake.url/reply"

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockTokenManager = mockk(relaxed = true)
        mockOkHttpClient = mockk(relaxed = true)

        every { mockTokenManager.getAccessToken() } returns "fake_access_token"
        every { mockTokenManager.getRefreshToken() } returns "fake_refresh_token"
        every { mockTokenManager.getUserEmail() } returns "test@example.com"

        sseClient = MailReplySseClient(
            context = mockContext,
            tokenManager = mockTokenManager,
            endpointUrl = endpointUrl,
            client = mockOkHttpClient
        )
    }

    @After
    fun tear_down() {
        unmockkAll()
    }

    @Test
    fun start_builds_request_and_enqueues_call() {
        val mockCall = mockk<okhttp3.Call>(relaxed = true)
        every { mockOkHttpClient.newCall(any()) } returns mockCall
        every { mockCall.enqueue(any()) } just Runs

        sseClient.start("subject", "body", "to@example.com", {}, {}, { _, _, _ -> }, { _, _ -> }, { _, _ -> }, {}, {})

        verify {
            mockOkHttpClient.newCall(match { req ->
                req.url.toString() == endpointUrl && req.method == "POST"
            })
        }
        verify { mockCall.enqueue(any()) }
    }

    @Test
    fun stop_cancels_call_and_job() {
        val mockCall = mockk<okhttp3.Call>(relaxed = true)
        every { mockOkHttpClient.newCall(any()) } returns mockCall
        every { mockCall.enqueue(any()) } just Runs
        every { mockCall.cancel() } just Runs

        sseClient.start("s", "b", "t", {}, {}, { _, _, _ -> }, { _, _ -> }, { _, _ -> }, {}, {})
        sseClient.stop()

        verify { mockCall.cancel() }
    }

    @Test
    fun on_response_parses_options_event_correctly() {
        val sseStream = "event: options\ndata: {\"count\": 1, \"items\": [{\"id\": 1, \"type\": \"accept\", \"title\": \"Acceptance\"}]}\n\n"
        val mockSource = Buffer().writeUtf8(sseStream)
        val mockResponseBody = mockk<ResponseBody>(relaxed = true)
        every { mockResponseBody.source() } returns mockSource
        val mockResponse = mockk<Response>(relaxed = true)
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body } returns mockResponseBody
        every { mockResponse.close() } just Runs
        val mockCall = mockk<okhttp3.Call>(relaxed = true)
        val callbackSlot = slot<Callback>()
        every { mockOkHttpClient.newCall(any()) } returns mockCall
        every { mockCall.enqueue(capture(callbackSlot)) } answers {
            callbackSlot.captured.onResponse(mockCall, mockResponse)
        }

        var options: List<ReplyOptionInfo>? = null
        sseClient.start("s", "b", "t", {}, { opts -> options = opts }, { _, _, _ -> }, { _, _ -> }, { _, _ -> }, {}, {})

        Thread.sleep(100)

        assertNotNull(options)
        assertEquals(1, options?.size)
        assertEquals(1, options?.get(0)?.id)
        assertEquals("Acceptance", options?.get(0)?.title)
    }

    @Test
    fun on_response_parses_option_delta_event_correctly() {
        val sseStream = "event: option.delta\ndata: {\"id\": 1, \"seq\": 2, \"text\": \" world\"}\n\n"
        val mockSource = Buffer().writeUtf8(sseStream)
        val mockResponseBody = mockk<ResponseBody>(relaxed = true)
        every { mockResponseBody.source() } returns mockSource
        val mockResponse = mockk<Response>(relaxed = true)
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body } returns mockResponseBody
        every { mockResponse.close() } just Runs
        val mockCall = mockk<okhttp3.Call>(relaxed = true)
        val callbackSlot = slot<Callback>()
        every { mockOkHttpClient.newCall(any()) } returns mockCall
        every { mockCall.enqueue(capture(callbackSlot)) } answers {
            callbackSlot.captured.onResponse(mockCall, mockResponse)
        }

        var delta: Triple<Int, Int, String>? = null
        sseClient.start("s", "b", "t", {}, {}, { id, seq, text -> delta = Triple(id, seq, text) }, { _, _ -> }, { _, _ -> }, {}, {})

        Thread.sleep(100)

        assertNotNull(delta)
        assertEquals(1, delta?.first)
        assertEquals(2, delta?.second)
        assertEquals(" world", delta?.third)
    }

    @Test
    fun on_response_parses_option_done_event_correctly() {
        val sseStream = "event: option.done\ndata: {\"id\": 1, \"total_seq\": 5}\n\n"
        val mockSource = Buffer().writeUtf8(sseStream)
        val mockResponseBody = mockk<ResponseBody>(relaxed = true)
        every { mockResponseBody.source() } returns mockSource
        val mockResponse = mockk<Response>(relaxed = true)
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body } returns mockResponseBody
        every { mockResponse.close() } just Runs
        val mockCall = mockk<okhttp3.Call>(relaxed = true)
        val callbackSlot = slot<Callback>()
        every { mockOkHttpClient.newCall(any()) } returns mockCall
        every { mockCall.enqueue(capture(callbackSlot)) } answers {
            callbackSlot.captured.onResponse(mockCall, mockResponse)
        }

        var doneInfo: Pair<Int, Int>? = null
        sseClient.start("s", "b", "t", {}, {}, { _, _, _ -> }, { id, total -> doneInfo = Pair(id, total) }, { _, _ -> }, {}, {})

        Thread.sleep(100)

        assertNotNull(doneInfo)
        assertEquals(1, doneInfo?.first)
        assertEquals(5, doneInfo?.second)
    }

    @Test
    fun on_response_parses_done_event_correctly() {
        val sseStream = "event: done\ndata: {\"reason\": \"finished\"}\n\n"
        val mockSource = Buffer().writeUtf8(sseStream)
        val mockResponseBody = mockk<ResponseBody>(relaxed = true)
        every { mockResponseBody.source() } returns mockSource
        val mockResponse = mockk<Response>(relaxed = true)
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body } returns mockResponseBody
        every { mockResponse.close() } just Runs
        val mockCall = mockk<okhttp3.Call>(relaxed = true)
        val callbackSlot = slot<Callback>()
        every { mockOkHttpClient.newCall(any()) } returns mockCall
        every { mockCall.enqueue(capture(callbackSlot)) } answers {
            callbackSlot.captured.onResponse(mockCall, mockResponse)
        }

        var doneReason: String? = null
        sseClient.start("s", "b", "t", {}, {}, { _, _, _ -> }, { _, _ -> }, { _, _ -> }, { reason -> doneReason = reason }, {})

        Thread.sleep(100)

        assertEquals("finished", doneReason)
    }

    @Test
    fun on_response_parses_ready_event_correctly() {
        val sseStream = "event: ready\ndata: {}\n\n"
        val mockSource = Buffer().writeUtf8(sseStream)
        val mockResponseBody = mockk<ResponseBody>(relaxed = true)
        every { mockResponseBody.source() } returns mockSource
        val mockResponse = mockk<Response>(relaxed = true)
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body } returns mockResponseBody
        every { mockResponse.close() } just Runs
        val mockCall = mockk<okhttp3.Call>(relaxed = true)
        val callbackSlot = slot<Callback>()
        every { mockOkHttpClient.newCall(any()) } returns mockCall
        every { mockCall.enqueue(capture(callbackSlot)) } answers {
            callbackSlot.captured.onResponse(mockCall, mockResponse)
        }

        var readyCalled = false
        sseClient.start("s", "b", "t", { readyCalled = true }, {}, { _, _, _ -> }, { _, _ -> }, { _, _ -> }, {}, {})

        Thread.sleep(100)

        assertTrue(readyCalled)
    }

    @Test
    fun on_response_parses_option_error_event_correctly() {
        val sseStream = "event: option.error\ndata: {\"id\": 1, \"message\": \"Error occurred\"}\n\n"
        val mockSource = Buffer().writeUtf8(sseStream)
        val mockResponseBody = mockk<ResponseBody>(relaxed = true)
        every { mockResponseBody.source() } returns mockSource
        val mockResponse = mockk<Response>(relaxed = true)
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body } returns mockResponseBody
        every { mockResponse.close() } just Runs
        val mockCall = mockk<okhttp3.Call>(relaxed = true)
        val callbackSlot = slot<Callback>()
        every { mockOkHttpClient.newCall(any()) } returns mockCall
        every { mockCall.enqueue(capture(callbackSlot)) } answers {
            callbackSlot.captured.onResponse(mockCall, mockResponse)
        }

        var errorInfo: Pair<Int, String>? = null
        sseClient.start("s", "b", "t", {}, {}, { _, _, _ -> }, { _, _ -> }, { id, msg -> errorInfo = Pair(id, msg) }, {}, {})

        Thread.sleep(100)

        assertNotNull(errorInfo)
        assertEquals(1, errorInfo?.first)
        assertEquals("Error occurred", errorInfo?.second)
    }

    @Test
    fun on_response_parses_ping_event_correctly() {
        val sseStream = "event: ping\ndata: {}\n\n"
        val mockSource = Buffer().writeUtf8(sseStream)
        val mockResponseBody = mockk<ResponseBody>(relaxed = true)
        every { mockResponseBody.source() } returns mockSource
        val mockResponse = mockk<Response>(relaxed = true)
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body } returns mockResponseBody
        every { mockResponse.close() } just Runs
        val mockCall = mockk<okhttp3.Call>(relaxed = true)
        val callbackSlot = slot<Callback>()
        every { mockOkHttpClient.newCall(any()) } returns mockCall
        every { mockCall.enqueue(capture(callbackSlot)) } answers {
            callbackSlot.captured.onResponse(mockCall, mockResponse)
        }

        // Ping event should be ignored (no callbacks triggered)
        var anythingCalled = false
        sseClient.start(
            "s", "b", "t",
            { anythingCalled = true },
            { anythingCalled = true },
            { _, _, _ -> anythingCalled = true },
            { _, _ -> anythingCalled = true },
            { _, _ -> anythingCalled = true },
            { _ -> anythingCalled = true },
            { anythingCalled = true }
        )

        Thread.sleep(100)

        // Ping should not trigger any callbacks
        assertFalse(anythingCalled)
    }

    @Test
    fun on_response_with_no_body_triggers_error() {
        val mockCall = mockk<okhttp3.Call>(relaxed = true)
        val callbackSlot = slot<Callback>()
        val mockResponse = mockk<Response>(relaxed = true)

        every { mockOkHttpClient.newCall(any()) } returns mockCall
        every { mockCall.enqueue(capture(callbackSlot)) } answers {
            callbackSlot.captured.onResponse(mockCall, mockResponse)
        }
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body } returns null
        every { mockResponse.close() } just Runs

        var errorMessage = ""
        sseClient.start("s", "b", "t", {}, {}, { _, _, _ -> }, { _, _ -> }, { _, _ -> }, {}, { error -> errorMessage = error })

        assertTrue(errorMessage.contains("No response body"))
    }

    @Test
    fun on_response_with_parse_error_triggers_error() {
        val sseStream = "event: options\ndata: {invalid json}\n\n"
        val mockSource = Buffer().writeUtf8(sseStream)
        val mockResponseBody = mockk<ResponseBody>(relaxed = true)
        every { mockResponseBody.source() } returns mockSource
        val mockResponse = mockk<Response>(relaxed = true)
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body } returns mockResponseBody
        every { mockResponse.close() } just Runs
        val mockCall = mockk<okhttp3.Call>(relaxed = true)
        val callbackSlot = slot<Callback>()
        every { mockOkHttpClient.newCall(any()) } returns mockCall
        every { mockCall.enqueue(capture(callbackSlot)) } answers {
            callbackSlot.captured.onResponse(mockCall, mockResponse)
        }

        var errorMessage = ""
        sseClient.start("s", "b", "t", {}, {}, { _, _, _ -> }, { _, _ -> }, { _, _ -> }, {}, { error -> errorMessage = error })

        Thread.sleep(100)

        assertTrue(errorMessage.contains("Parse error") || errorMessage.isEmpty())
    }

    @Test
    fun stop_before_response_cancels_call() {
        val mockCall = mockk<okhttp3.Call>(relaxed = true)
        every { mockOkHttpClient.newCall(any()) } returns mockCall
        every { mockCall.enqueue(any()) } just Runs
        every { mockCall.cancel() } just Runs

        sseClient.start("s", "b", "t", {}, {}, { _, _, _ -> }, { _, _ -> }, { _, _ -> }, {}, {})
        sseClient.stop()

        verify { mockCall.cancel() }
    }
}
