package com.fiveis.xend.data.repository

import android.content.Context
import com.fiveis.xend.data.model.SendResponse
import com.fiveis.xend.network.MailApiService
import com.fiveis.xend.network.RetrofitClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import okhttp3.MultipartBody
import okio.Buffer
import retrofit2.Response

class MailSendRepositoryTest {

    private lateinit var context: Context
    private lateinit var mailApiService: MailApiService
    private lateinit var repository: MailSendRepository

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        mailApiService = mockk()

        mockkObject(RetrofitClient)
        every { RetrofitClient.getMailApiService(context) } returns mailApiService

        repository = MailSendRepository(context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun sendEmail_with_successful_response_returns_SendResponse() = runTest {
        val to = listOf("test@example.com")
        val subject = "Test Subject"
        val body = "Test Body"
        val expectedResponse = SendResponse(
            id = "12345",
            threadId = "thread123",
            labelIds = listOf("SENT")
        )

        val mockResponse = Response.success(201, expectedResponse)
        val partsSlot = slot<List<MultipartBody.Part>>()
        coEvery {
            mailApiService.sendEmail(parts = capture(partsSlot))
        } returns mockResponse

        val result = repository.sendEmail(to, subject, body)

        assertEquals(expectedResponse, result)
        val captured = partsSlot.captured
        assertEquals(subject, captured.valueFor("subject"))
        assertEquals(body, captured.valueFor("body"))
        assertEquals("true", captured.valueFor("is_html"))
        assertEquals(listOf("test@example.com"), captured.valuesFor("to"))
        coVerify { mailApiService.sendEmail(parts = any()) }
    }

    @Test
    fun sendEmail_with_multiple_recipients_sends_correct_request() = runTest {
        val to = listOf("test1@example.com", "test2@example.com")
        val subject = "Test Subject"
        val body = "Test Body"
        val expectedResponse = SendResponse(
            id = "12345",
            threadId = "thread123",
            labelIds = listOf("SENT")
        )

        val mockResponse = Response.success(201, expectedResponse)
        val partsSlot = slot<List<MultipartBody.Part>>()
        coEvery {
            mailApiService.sendEmail(parts = capture(partsSlot))
        } returns mockResponse

        val result = repository.sendEmail(to, subject, body)

        assertEquals(expectedResponse, result)
        val captured = partsSlot.captured
        assertEquals(2, captured.valuesFor("to").size)
        assertEquals(to, captured.valuesFor("to"))
        coVerify { mailApiService.sendEmail(parts = any()) }
    }

    @Test
    fun sendEmail_throws_exception_when_response_body_is_null() = runTest {
        val to = listOf("test@example.com")
        val subject = "Test Subject"
        val body = "Test Body"

        val mockResponse = Response.success<SendResponse>(201, null)
        coEvery { mailApiService.sendEmail(parts = any()) } returns mockResponse

        val exception = try {
            repository.sendEmail(to, subject, body)
            null
        } catch (e: IllegalStateException) {
            e
        }

        assertTrue(exception != null)
        assertTrue(exception?.message?.contains("Success response but body is null") == true)
    }

    @Test
    fun sendEmail_throws_exception_when_response_code_is_not_201() = runTest {
        val to = listOf("test@example.com")
        val subject = "Test Subject"
        val body = "Test Body"
        val expectedResponse = SendResponse(
            id = "12345",
            threadId = "thread123",
            labelIds = listOf("SENT")
        )

        val mockResponse = Response.success(200, expectedResponse)
        coEvery { mailApiService.sendEmail(parts = any()) } returns mockResponse

        val exception = try {
            repository.sendEmail(to, subject, body)
            null
        } catch (e: IllegalStateException) {
            e
        }

        assertTrue(exception != null)
        assertTrue(exception?.message?.contains("Send failed") == true)
    }

    @Test
    fun sendEmail_throws_exception_when_response_is_not_successful() = runTest {
        val to = listOf("test@example.com")
        val subject = "Test Subject"
        val body = "Test Body"

        val mockResponse = Response.error<SendResponse>(
            400,
            "Bad request".toResponseBody()
        )
        coEvery { mailApiService.sendEmail(parts = any()) } returns mockResponse

        val exception = try {
            repository.sendEmail(to, subject, body)
            null
        } catch (e: IllegalStateException) {
            e
        }

        assertTrue(exception != null)
        assertTrue(exception?.message?.contains("Send failed: HTTP 400") == true)
    }

    @Test
    fun sendEmail_throws_exception_with_server_error_message() = runTest {
        val to = listOf("test@example.com")
        val subject = "Test Subject"
        val body = "Test Body"
        val errorMessage = "Server error: Invalid recipient"

        val mockResponse = Response.error<SendResponse>(
            500,
            errorMessage.toResponseBody()
        )
        coEvery { mailApiService.sendEmail(parts = any()) } returns mockResponse

        val exception = try {
            repository.sendEmail(to, subject, body)
            null
        } catch (e: IllegalStateException) {
            e
        }

        assertTrue(exception != null)
        assertTrue(exception?.message?.contains("HTTP 500") == true)
        assertTrue(exception?.message?.contains(errorMessage) == true)
    }

    private fun List<MultipartBody.Part>.valuesFor(name: String): List<String> =
        filter { it.partName() == name }
            .map { it.bodyAsString() }

    private fun List<MultipartBody.Part>.valueFor(name: String): String? =
        valuesFor(name).firstOrNull()

    private fun MultipartBody.Part.partName(): String? {
        val header = headers?.get("Content-Disposition") ?: return null
        return header.split(";")
            .map { it.trim() }
            .firstOrNull { it.startsWith("name=") }
            ?.substringAfter("name=\"")
            ?.substringBefore("\"")
    }

    private fun MultipartBody.Part.bodyAsString(): String {
        val buffer = Buffer()
        body.writeTo(buffer)
        return buffer.readUtf8()
    }
}
