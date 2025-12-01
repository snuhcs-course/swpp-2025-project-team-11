package com.fiveis.xend.network

import com.fiveis.xend.data.model.ProfileData
import com.fiveis.xend.data.model.UpdateProfileRequest
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ProfileApiServiceTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: ProfileApiService

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        apiService = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ProfileApiService::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun test_getProfile_success() = runTest {
        val responseBody = """
            {
                "display_name": "Test User",
                "info": "Test Info"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json")
        )

        val response = apiService.getProfile()

        assertEquals(true, response.isSuccessful)
        assertEquals("Test User", response.body()?.displayName)
        assertEquals("Test Info", response.body()?.info)
    }

    @Test
    fun test_updateProfile_success() = runTest {
        val responseBody = """
            {
                "display_name": "Updated User",
                "info": "Updated Info"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json")
        )

        val request = UpdateProfileRequest(displayName = "Updated User")
        val response = apiService.updateProfile(request)

        assertEquals(true, response.isSuccessful)
        assertEquals("Updated User", response.body()?.displayName)
    }

    @Test
    fun test_patchProfile_success() = runTest {
        val responseBody = """
            {
                "display_name": "Patched User",
                "info": "Patched Info"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json")
        )

        val request = UpdateProfileRequest(displayName = "Patched User")
        val response = apiService.patchProfile(request)

        assertEquals(true, response.isSuccessful)
        assertEquals("Patched User", response.body()?.displayName)
    }
}
