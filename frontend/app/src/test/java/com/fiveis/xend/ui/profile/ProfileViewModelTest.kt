package com.fiveis.xend.ui.profile

import android.content.Context
import app.cash.turbine.test
import com.fiveis.xend.data.model.ProfileData
import com.fiveis.xend.data.repository.AuthRepository
import com.fiveis.xend.data.repository.LogoutResult
import com.fiveis.xend.data.repository.ProfileRepository
import com.fiveis.xend.data.repository.ProfileResult
import com.fiveis.xend.data.source.TokenManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var authRepository: AuthRepository
    private lateinit var profileRepository: ProfileRepository
    private lateinit var tokenManager: TokenManager
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        context = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        profileRepository = mockk(relaxed = true)

        mockkConstructor(TokenManager::class)
        every { anyConstructed<TokenManager>().getUserEmail() } returns "test@example.com"
        every { anyConstructed<TokenManager>().getRefreshToken() } returns "refresh_token"
        every { anyConstructed<TokenManager>().clearTokens() } returns Unit

        coEvery { profileRepository.getProfile() } returns ProfileResult.Success(
            ProfileData(
                displayName = "Test User",
                info = "Test info"
            )
        )
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun test_loadProfile_success() = runTest {
        viewModel = ProfileViewModel(context, authRepository, profileRepository)

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Test User", state.displayName)
            assertEquals("Test info", state.info)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun test_loadProfile_failure() = runTest {
        coEvery { profileRepository.getProfile() } returns ProfileResult.Failure("Network error")

        viewModel = ProfileViewModel(context, authRepository, profileRepository)

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Network error", state.profileError)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun test_toggleEditMode_enterEdit() = runTest {
        viewModel = ProfileViewModel(context, authRepository, profileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleEditMode()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isEditing)
            assertEquals("Test User", state.originalDisplayName)
        }
    }

    @Test
    fun test_toggleEditMode_exitEdit() = runTest {
        viewModel = ProfileViewModel(context, authRepository, profileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleEditMode()
        viewModel.updateDisplayName("New Name")
        viewModel.toggleEditMode()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isEditing)
            assertEquals("Test User", state.displayName)
        }
    }

    @Test
    fun test_updateDisplayName() = runTest {
        viewModel = ProfileViewModel(context, authRepository, profileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateDisplayName("New Name")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("New Name", state.displayName)
        }
    }

    @Test
    fun test_updateInfo() = runTest {
        viewModel = ProfileViewModel(context, authRepository, profileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateInfo("New Info")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("New Info", state.info)
        }
    }

    @Test
    fun test_saveProfile_success() = runTest {
        val updatedProfile = ProfileData(
            displayName = "Updated Name",
            info = "Updated Info"
        )
        coEvery {
            profileRepository.updateProfile(any(), any())
        } returns ProfileResult.Success(updatedProfile)

        viewModel = ProfileViewModel(context, authRepository, profileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateDisplayName("Updated Name")
        viewModel.updateInfo("Updated Info")
        viewModel.saveProfile()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Updated Name", state.displayName)
            assertTrue(state.saveSuccess)
            assertFalse(state.isEditing)
        }
    }

    @Test
    fun test_saveProfile_failure() = runTest {
        coEvery {
            profileRepository.updateProfile(any(), any())
        } returns ProfileResult.Failure("Save failed")

        viewModel = ProfileViewModel(context, authRepository, profileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveProfile()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Save failed", state.profileError)
            assertFalse(state.isSaving)
        }
    }

    @Test
    fun test_dismissProfileError() = runTest {
        coEvery { profileRepository.getProfile() } returns ProfileResult.Failure("Error")

        viewModel = ProfileViewModel(context, authRepository, profileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.dismissProfileError()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.profileError)
        }
    }

    @Test
    fun test_logout_success() = runTest {
        coEvery { authRepository.logout(any()) } returns LogoutResult.Success

        viewModel = ProfileViewModel(context, authRepository, profileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.logout()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.logoutSuccess)
            assertFalse(state.isLoggingOut)
        }

        coVerify { anyConstructed<TokenManager>().clearTokens() }
    }

    @Test
    fun test_logout_failure() = runTest {
        coEvery { authRepository.logout(any()) } returns LogoutResult.Failure("Logout failed")

        viewModel = ProfileViewModel(context, authRepository, profileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.logout()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Logout failed", state.logoutError)
            assertTrue(state.showLogoutFailureDialog)
            assertFalse(state.isLoggingOut)
        }
    }

    @Test
    fun test_forceLogout() = runTest {
        viewModel = ProfileViewModel(context, authRepository, profileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.forceLogout()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.logoutSuccess)
            assertFalse(state.isLoggingOut)
        }

        coVerify { anyConstructed<TokenManager>().clearTokens() }
    }

    @Test
    fun test_dismissLogoutFailureDialog() = runTest {
        viewModel = ProfileViewModel(context, authRepository, profileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.dismissLogoutFailureDialog()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showLogoutFailureDialog)
            assertNull(state.logoutError)
        }
    }
}
