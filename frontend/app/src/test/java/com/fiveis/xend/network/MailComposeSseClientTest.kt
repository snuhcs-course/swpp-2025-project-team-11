package com.fiveis.xend.network

import android.content.Context
import com.fiveis.xend.data.source.TokenManager
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.Call
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class MailComposeSseClientTest {

    private lateinit var mockContext: Context
    private lateinit var mockTokenManager: TokenManager
    private lateinit var mockOkHttpClient: OkHttpClient
    private lateinit var sseClient: MailComposeSseClient

    private val endpointUrl = "http://fake.url/compose"

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockTokenManager = mockk(relaxed = true)
        mockOkHttpClient = mockk(relaxed = true)

        every { mockTokenManager.getAccessToken() } returns "fake_access_token"
        every { mockTokenManager.getRefreshToken() } returns "fake_refresh_token"
        every { mockTokenManager.getUserEmail() } returns "test@example.com"

        sseClient = MailComposeSseClient(
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

        val payload = JSONObject().apply {
            put("test", "value")
        }
        sseClient.start(payload, {}, { _, _ -> }, {}, {})

        verify {
            mockOkHttpClient.newCall(match { req ->
                req.url.toString() == endpointUrl &&
                req.method == "POST"
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

        val payload = JSONObject().apply {
            put("test", "value")
        }
        sseClient.start(payload, {}, { _, _ -> }, {}, {})
        sseClient.stop()

        verify { mockCall.cancel() }
    }

    @Test
    fun callback_on_failure_triggers_error_lambda() {
        val mockCall = mockk<okhttp3.Call>(relaxed = true)
        val callbackSlot = slot<Callback>()
        every { mockOkHttpClient.newCall(any()) } returns mockCall
        every { mockCall.enqueue(capture(callbackSlot)) } answers {
            callbackSlot.captured.onFailure(mockCall, IOException("Test failure"))
        }

        var errorMessage = ""
        val payload = JSONObject().apply {
            put("test", "value")
        }
        sseClient.start(payload, {}, { _, _ -> }, {}, { error -> errorMessage = error })

        assertTrue(errorMessage.contains("Test failure"))
    }
    
    @Test
    fun on_response_with_error_code_triggers_error_lambda() {
        val mockCall = mockk<okhttp3.Call>(relaxed = true)
        val callbackSlot = slot<Callback>()
        val mockResponse = mockk<Response>(relaxed = true)

        every { mockOkHttpClient.newCall(any()) } returns mockCall
        every { mockCall.enqueue(capture(callbackSlot)) } answers {
            callbackSlot.captured.onResponse(mockCall, mockResponse)
        }
        every { mockResponse.isSuccessful } returns false
        every { mockResponse.code } returns 500
        every { mockResponse.message } returns "Server Error"
        every { mockResponse.body } returns "Error".toResponseBody()
        every { mockResponse.close() } just Runs

        var errorMessage = ""
        val payload = JSONObject().apply {
            put("test", "value")
        }
        sseClient.start(payload, {}, { _, _ -> }, {}, { error -> errorMessage = error })

        assertTrue(errorMessage.contains("HTTP 500 Server Error"))
    }


    @Test
    fun on_response_parses_sse_subject_event_correctly() = runTest {
        val sseStream = "event: subject\ndata: {\"title\": \"Test Subject\"}\n\n"
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

        var subject = ""
        val payload = JSONObject().apply {
            put("test", "value")
        }
        sseClient.start(payload, { s -> subject = s }, { _, _ -> }, {}, {})

        // Let the parser run
        Thread.sleep(100)

        assertEquals("Test Subject", subject)
    }

    @Test
    fun on_response_parses_sse_body_delta_event_correctly() = runTest {
        val sseStream = "event: body.delta\ndata: {\"seq\": 1, \"text\": \" a test\"}\n\n"
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

        var bodyDelta = ""
        var seq = -1
        val payload = JSONObject().apply {
            put("test", "value")
        }
        sseClient.start(payload, { }, { s, d -> seq = s; bodyDelta = d }, { }, { })

        Thread.sleep(100)

        assertEquals(1, seq)
        assertEquals(" a test", bodyDelta)
    }

    @Test
    fun on_response_parses_sse_done_event_correctly() = runTest {
        val sseStream = "event: done\ndata: {}\n\n"
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

        var doneCalled = false
        val payload = JSONObject().apply {
            put("test", "value")
        }
        sseClient.start(payload, { }, { _, _ -> }, { doneCalled = true }, { })

        Thread.sleep(100)

        assertTrue(doneCalled)
    }
}
