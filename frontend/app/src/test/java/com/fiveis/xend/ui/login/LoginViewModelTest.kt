package com.fiveis.xend.ui.login

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fiveis.xend.data.repository.AuthRepository
import com.fiveis.xend.data.repository.AuthResult
import com.fiveis.xend.data.source.TokenManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var tokenManager: TokenManager
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        tokenManager = mockk(relaxed = true)
        authRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_with_saved_tokens_sets_logged_in_state() = runTest {
        every { tokenManager.getAccessToken() } returns "access_token"
        every { tokenManager.getUserEmail() } returns "test@example.com"

        viewModel = LoginViewModel(tokenManager, authRepository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLoggedIn)
        assertEquals("test@example.com", viewModel.uiState.value.userEmail)
    }

    @Test
    fun init_without_saved_tokens_sets_logged_out_state() = runTest {
        every { tokenManager.getAccessToken() } returns null
        every { tokenManager.getUserEmail() } returns null

        viewModel = LoginViewModel(tokenManager, authRepository)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoggedIn)
        assertEquals("", viewModel.uiState.value.userEmail)
    }

    @Test
    fun handle_auth_code_success_saves_tokens_and_updates_state() = runTest {
        val authCode = "test_auth_code"
        val email = "test@example.com"
        val accessToken = "access_token_123"
        val refreshToken = "refresh_token_123"

        every { tokenManager.getAccessToken() } returns null
        every { tokenManager.getUserEmail() } returns null
        coEvery { authRepository.sendAuthCodeToServer(authCode) } returns AuthResult.Success(
            accessToken = accessToken,
            refreshToken = refreshToken
        )

        viewModel = LoginViewModel(tokenManager, authRepository)
        advanceUntilIdle()

        viewModel.handleAuthCodeReceived(authCode, email)
        advanceUntilIdle()

        verify {
            tokenManager.saveTokens(
                access = accessToken,
                refresh = refreshToken,
                email = email
            )
        }
        assertTrue(viewModel.uiState.value.isLoggedIn)
        assertEquals(email, viewModel.uiState.value.userEmail)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun handle_auth_code_failure_sets_error_message() = runTest {
        val authCode = "test_auth_code"
        val email = "test@example.com"
        val errorMessage = "서버 오류"

        every { tokenManager.getAccessToken() } returns null
        every { tokenManager.getUserEmail() } returns null
        coEvery { authRepository.sendAuthCodeToServer(authCode) } returns AuthResult.Failure(
            message = errorMessage
        )

        viewModel = LoginViewModel(tokenManager, authRepository)
        advanceUntilIdle()

        viewModel.handleAuthCodeReceived(authCode, email)
        advanceUntilIdle()

        assertEquals(errorMessage, viewModel.uiState.value.messages)
        assertFalse(viewModel.uiState.value.isLoggedIn)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun update_messages_changes_message_state() = runTest {
        every { tokenManager.getAccessToken() } returns null
        every { tokenManager.getUserEmail() } returns null

        viewModel = LoginViewModel(tokenManager, authRepository)
        advanceUntilIdle()

        val testMessage = "Test message"
        viewModel.updateMessages(testMessage)

        assertEquals(testMessage, viewModel.uiState.value.messages)
    }
}
