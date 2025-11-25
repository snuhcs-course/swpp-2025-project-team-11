package com.fiveis.xend.network

import com.fiveis.xend.data.model.AddContactRequest
import com.fiveis.xend.data.model.AddGroupRequest
import com.fiveis.xend.data.model.PromptOptionRequest
import com.fiveis.xend.data.model.PromptOptionUpdateRequest
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ContactApiServiceTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: ContactApiService

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        apiService = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ContactApiService::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun test_addContact_success() = runTest {
        val responseBody = """
            {
                "id": 1,
                "name": "Test Contact",
                "email": "test@example.com"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json")
        )

        val request = AddContactRequest(name = "Test Contact", email = "test@example.com")
        val response = apiService.addContact(request)

        assertEquals(true, response.isSuccessful)
        assertEquals(1L, response.body()?.id)
    }

    @Test
    fun test_getAllContacts_success() = runTest {
        val responseBody = """
            [
                {
                    "id": 1,
                    "name": "Contact 1",
                    "email": "contact1@example.com"
                }
            ]
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json")
        )

        val response = apiService.getAllContacts()

        assertEquals(true, response.isSuccessful)
        assertEquals(1, response.body()?.size)
    }

    @Test
    fun test_getContact_success() = runTest {
        val responseBody = """
            {
                "id": 1,
                "name": "Test Contact",
                "email": "test@example.com"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json")
        )

        val response = apiService.getContact(1L)

        assertEquals(true, response.isSuccessful)
        assertEquals("Test Contact", response.body()?.name)
    }

    @Test
    fun test_deleteContact_success() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(204)
        )

        val response = apiService.deleteContact(1L)

        assertEquals(true, response.isSuccessful)
    }

    @Test
    fun test_updateContact_success() = runTest {
        val responseBody = """
            {
                "id": 1,
                "name": "Updated Contact",
                "email": "updated@example.com"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json")
        )

        val payload = mapOf("name" to "Updated Contact")
        val response = apiService.updateContact(1L, payload)

        assertEquals(true, response.isSuccessful)
        assertEquals("Updated Contact", response.body()?.name)
    }

    @Test
    fun test_addGroup_success() = runTest {
        val responseBody = """
            {
                "id": 1,
                "name": "Test Group",
                "contactIds": []
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json")
        )

        val request = AddGroupRequest(name = "Test Group", description = "Test Description")
        val response = apiService.addGroup(request)

        assertEquals(true, response.isSuccessful)
        assertEquals("Test Group", response.body()?.name)
    }

    @Test
    fun test_getAllGroups_success() = runTest {
        val responseBody = """
            [
                {
                    "id": 1,
                    "name": "Group 1",
                    "contactIds": []
                }
            ]
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json")
        )

        val response = apiService.getAllGroups()

        assertEquals(true, response.isSuccessful)
        assertEquals(1, response.body()?.size)
    }

    @Test
    fun test_getGroup_success() = runTest {
        val responseBody = """
            {
                "id": 1,
                "name": "Test Group",
                "contactIds": []
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json")
        )

        val response = apiService.getGroup(1L)

        assertEquals(true, response.isSuccessful)
        assertEquals("Test Group", response.body()?.name)
    }

    @Test
    fun test_deleteGroup_success() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(204)
        )

        val response = apiService.deleteGroup(1L)

        assertEquals(true, response.isSuccessful)
    }

    @Test
    fun test_updateGroup_success() = runTest {
        val responseBody = """
            {
                "id": 1,
                "name": "Updated Group",
                "contactIds": []
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json")
        )

        val payload = mapOf("name" to "Updated Group")
        val response = apiService.updateGroup(1L, payload)

        assertEquals(true, response.isSuccessful)
        assertEquals("Updated Group", response.body()?.name)
    }

    @Test
    fun test_addPromptOption_success() = runTest {
        val responseBody = """
            {
                "id": 1,
                "key": "test_key",
                "name": "Test Option",
                "prompt": "Test Prompt"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json")
        )

        val request = PromptOptionRequest(key = "test_key", name = "Test Option", prompt = "Test Prompt")
        val response = apiService.addPromptOption(request)

        assertEquals(true, response.isSuccessful)
        assertEquals("Test Option", response.body()?.name)
    }

    @Test
    fun test_getAllPromptOptions_success() = runTest {
        val responseBody = """
            [
                {
                    "id": 1,
                    "key": "test_key",
                    "name": "Option 1",
                    "prompt": "Test Prompt"
                }
            ]
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json")
        )

        val response = apiService.getAllPromptOptions()

        assertEquals(true, response.isSuccessful)
        assertEquals(1, response.body()?.size)
    }

    @Test
    fun test_updatePromptOption_success() = runTest {
        val responseBody = """
            {
                "id": 1,
                "key": "test_key",
                "name": "Updated Option",
                "prompt": "Updated Prompt"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json")
        )

        val request = PromptOptionUpdateRequest(id = 1L, name = "Updated Option", prompt = "Updated Prompt")
        val response = apiService.updatePromptOption(1L, request)

        assertEquals(true, response.isSuccessful)
        assertEquals("Updated Option", response.body()?.name)
    }

    @Test
    fun test_deletePromptOption_success() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(204)
        )

        val response = apiService.deletePromptOption(1L)

        assertEquals(true, response.isSuccessful)
    }
}
