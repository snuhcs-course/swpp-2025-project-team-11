package com.fiveis.xend.network

import com.fiveis.xend.data.model.AttachmentAnalysisRequest
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MailApiServiceTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: MailApiService

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        apiService = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MailApiService::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun test_sendEmail_success() = runTest {
        val responseBody = """
            {
                "id": "msg123",
                "threadId": "thread123",
                "labelIds": ["SENT"]
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json")
        )

        val parts = listOf(
            MultipartBody.Part.createFormData("to", "test@example.com"),
            MultipartBody.Part.createFormData("subject", "Test Subject")
        )

        val response = apiService.sendEmail(parts)

        assertEquals(true, response.isSuccessful)
        assertEquals("msg123", response.body()?.id)
    }

    @Test
    fun test_getEmails_success() = runTest {
        val responseBody = """
            {
                "messages": [],
                "next_page_token": null,
                "result_size_estimate": 0
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json")
        )

        val response = apiService.getEmails(labels = "INBOX", maxResults = 20)

        assertEquals(true, response.isSuccessful)
        assertEquals(0, response.body()?.messages?.size)
    }

    @Test
    fun test_getMail_success() = runTest {
        val responseBody = """
            {
                "id": "msg123",
                "thread_id": "thread123",
                "subject": "Test Subject",
                "from_email": "sender@example.com",
                "to_email": "recipient@example.com",
                "to": "recipient@example.com",
                "date": "2024-01-01 12:00:00",
                "date_raw": "Mon, 1 Jan 2024 12:00:00 +0000",
                "body": "Test body",
                "snippet": "Test snippet",
                "is_unread": true,
                "label_ids": ["INBOX"],
                "attachments": []
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json")
        )

        val response = apiService.getMail("msg123")

        assertEquals(true, response.isSuccessful)
        assertEquals("msg123", response.body()?.id)
        assertEquals("Test Subject", response.body()?.subject)
    }

    @Test
    fun test_downloadAttachment_success() = runTest {
        val fileContent = "file content"

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(fileContent)
                .addHeader("Content-Type", "application/octet-stream")
        )

        val response = apiService.downloadAttachment(
            messageId = "msg123",
            attachmentId = "att123",
            filename = "test.txt",
            mimeType = "text/plain"
        )

        assertEquals(true, response.isSuccessful)
        assertEquals(fileContent, response.body()?.string())
    }

    @Test
    fun test_analyzeAttachment_success() = runTest {
        val responseBody = """
            {
                "summary": "Test summary",
                "keywords": ["test", "keyword"]
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json")
        )

        val request = AttachmentAnalysisRequest(
            messageId = "msg123",
            attachmentId = "att123",
            filename = "test.txt",
            mimeType = "text/plain"
        )

        val response = apiService.analyzeAttachment(request)

        assertEquals(true, response.isSuccessful)
        assertEquals("Test summary", response.body()?.summary)
    }
}
