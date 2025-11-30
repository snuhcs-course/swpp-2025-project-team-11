package com.fiveis.xend.data.repository

import android.content.Context
import com.fiveis.xend.data.database.AppDatabase
import com.fiveis.xend.data.database.ProfileDao
import com.fiveis.xend.data.model.ProfileData
import com.fiveis.xend.data.model.UpdateProfileRequest
import com.fiveis.xend.network.ProfileApiService
import com.fiveis.xend.network.RetrofitClient
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class ProfileRepositoryTest {

    private lateinit var context: Context
    private lateinit var apiService: ProfileApiService
    private lateinit var profileDao: ProfileDao
    private lateinit var repository: ProfileRepository

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        apiService = mockk()
        profileDao = mockk(relaxed = true)

        mockkObject(RetrofitClient)
        mockkObject(AppDatabase)
        every { RetrofitClient.getProfileApiService(any()) } returns apiService
        every { AppDatabase.getDatabase(any()).profileDao() } returns profileDao

        repository = ProfileRepository(context)
    }

    @Test
    fun test_getProfile_success() = runTest {
        val mockProfileData = ProfileData(
            displayName = "Test User",
            info = "Test info"
        )

        coEvery { apiService.getProfile() } returns Response.success(mockProfileData)

        val result = repository.getProfile()

        assertTrue(result is ProfileResult.Success)
        assertEquals(mockProfileData, (result as ProfileResult.Success).data)
    }

    @Test
    fun test_getProfile_successWithNullBody() = runTest {
        coEvery { apiService.getProfile() } returns Response.success(null)

        val result = repository.getProfile()

        assertTrue(result is ProfileResult.Failure)
        assertEquals("프로필 정보가 없습니다", (result as ProfileResult.Failure).message)
    }

    @Test
    fun test_getProfile_failure() = runTest {
        coEvery { apiService.getProfile() } returns Response.error(
            404,
            "Not Found".toResponseBody()
        )

        val result = repository.getProfile()

        assertTrue(result is ProfileResult.Failure)
        assertTrue((result as ProfileResult.Failure).message.contains("HTTP 404"))
    }

    @Test
    fun test_getProfile_exception() = runTest {
        coEvery { apiService.getProfile() } throws Exception("Network error")

        val result = repository.getProfile()

        assertTrue(result is ProfileResult.Failure)
        assertTrue((result as ProfileResult.Failure).message.contains("Network error"))
    }

    @Test
    fun test_updateProfile_success() = runTest {
        val mockProfileData = ProfileData(
            displayName = "Updated User",
            info = "Updated info"
        )

        coEvery {
            apiService.patchProfile(any())
        } returns Response.success(mockProfileData)

        val result = repository.updateProfile("Updated User", "Updated info", null)

        assertTrue(result is ProfileResult.Success)
        assertEquals(mockProfileData, (result as ProfileResult.Success).data)
    }

    @Test
    fun test_updateProfile_successWithNullBody() = runTest {
        coEvery { apiService.patchProfile(any()) } returns Response.success(null)

        val result = repository.updateProfile("Test", "Test", null)

        assertTrue(result is ProfileResult.Failure)
        assertEquals("프로필 업데이트 응답이 없습니다", (result as ProfileResult.Failure).message)
    }

    @Test
    fun test_updateProfile_failure() = runTest {
        coEvery { apiService.patchProfile(any()) } returns Response.error(
            400,
            "Bad Request".toResponseBody()
        )

        val result = repository.updateProfile("Test", "Test", null)

        assertTrue(result is ProfileResult.Failure)
        assertTrue((result as ProfileResult.Failure).message.contains("HTTP 400"))
    }

    @Test
    fun test_updateProfile_exception() = runTest {
        coEvery { apiService.patchProfile(any()) } throws Exception("Connection timeout")

        val result = repository.updateProfile("Test", "Test", null)

        assertTrue(result is ProfileResult.Failure)
        assertTrue((result as ProfileResult.Failure).message.contains("Connection timeout"))
    }

    @Test
    fun test_updateProfile_withNullDisplayName() = runTest {
        val mockProfileData = ProfileData(
            displayName = null,
            info = "Info only"
        )

        coEvery {
            apiService.patchProfile(any())
        } returns Response.success(mockProfileData)

        val result = repository.updateProfile(null, "Info only", null)

        assertTrue(result is ProfileResult.Success)
    }

    @Test
    fun test_updateProfile_withNullInfo() = runTest {
        val mockProfileData = ProfileData(
            displayName = "Name only",
            info = null
        )

        coEvery {
            apiService.patchProfile(any())
        } returns Response.success(mockProfileData)

        val result = repository.updateProfile("Name only", null, null)

        assertTrue(result is ProfileResult.Success)
    }
}
