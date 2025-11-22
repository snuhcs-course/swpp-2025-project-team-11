package com.fiveis.xend.ui.contactbook

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fiveis.xend.data.model.PromptOption
import com.fiveis.xend.data.repository.ContactBookRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddGroupViewModelPromptOptionsTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var application: Application
    private lateinit var repository: ContactBookRepository
    private lateinit var viewModel: AddGroupViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        repository = mockk(relaxed = true)

        every { application.applicationContext } returns application
        every { repository.observePromptOptions() } returns kotlinx.coroutines.flow.flowOf(emptyList())
        coEvery { repository.refreshPromptOptions() } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun update_prompt_option_success_calls_success_callback() = runTest {
        val original = PromptOption(id = 1L, key = "tone", name = "Formal", prompt = "Be formal")
        val updated = PromptOption(id = 1L, key = "tone", name = "Very Formal", prompt = "Be very formal")

        coEvery {
            repository.updatePromptOption(1L, "Very Formal", "Be very formal")
        } returns updated

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        var successCalled = false
        var returnedOption: PromptOption? = null
        viewModel.updatePromptOption(
            1L,
            "Very Formal",
            "Be very formal",
            onSuccess = { option ->
                successCalled = true
                returnedOption = option
            },
            onError = {}
        )
        advanceUntilIdle()

        assertTrue(successCalled)
        assertEquals(updated, returnedOption)
        coVerify { repository.updatePromptOption(1L, "Very Formal", "Be very formal") }
    }

    @Test
    fun update_prompt_option_failure_calls_error_callback() = runTest {
        coEvery {
            repository.updatePromptOption(any(), any(), any())
        } throws Exception("Update failed")

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        var errorCalled = false
        var errorMessage = ""
        viewModel.updatePromptOption(
            1L,
            "Test",
            "Test prompt",
            onSuccess = {},
            onError = { msg ->
                errorCalled = true
                errorMessage = msg
            }
        )
        advanceUntilIdle()

        assertTrue(errorCalled)
        assertTrue(errorMessage.contains("Update failed"))
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun update_prompt_option_with_empty_name_sets_error() = runTest {
        coEvery {
            repository.updatePromptOption(any(), any(), any())
        } throws Exception("Name cannot be empty")

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        var errorMessage = ""
        viewModel.updatePromptOption(
            1L,
            "",
            "Prompt",
            onSuccess = {},
            onError = { errorMessage = it }
        )
        advanceUntilIdle()

        assertTrue(errorMessage.isNotEmpty())
    }

    @Test
    fun update_prompt_option_with_empty_prompt_sets_error() = runTest {
        coEvery {
            repository.updatePromptOption(any(), any(), any())
        } throws Exception("Prompt cannot be empty")

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        var errorMessage = ""
        viewModel.updatePromptOption(
            1L,
            "Name",
            "",
            onSuccess = {},
            onError = { errorMessage = it }
        )
        advanceUntilIdle()

        assertTrue(errorMessage.isNotEmpty())
    }

    @Test
    fun delete_prompt_option_success_calls_success_callback() = runTest {
        coEvery {
            repository.deletePromptOption(1L)
        } returns Unit

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        var successCalled = false
        viewModel.deletePromptOption(
            1L,
            onSuccess = { successCalled = true },
            onError = {}
        )
        advanceUntilIdle()

        assertTrue(successCalled)
        coVerify { repository.deletePromptOption(1L) }
    }

    @Test
    fun delete_prompt_option_failure_calls_error_callback() = runTest {
        coEvery {
            repository.deletePromptOption(any())
        } throws Exception("Delete failed")

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        var errorCalled = false
        var errorMessage = ""
        viewModel.deletePromptOption(
            1L,
            onSuccess = {},
            onError = { msg ->
                errorCalled = true
                errorMessage = msg
            }
        )
        advanceUntilIdle()

        assertTrue(errorCalled)
        assertTrue(errorMessage.contains("Delete failed"))
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun delete_nonexistent_prompt_option_handles_error() = runTest {
        coEvery {
            repository.deletePromptOption(999L)
        } throws Exception("Option not found")

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        var errorMessage = ""
        viewModel.deletePromptOption(
            999L,
            onSuccess = {},
            onError = { errorMessage = it }
        )
        advanceUntilIdle()

        assertTrue(errorMessage.contains("Option not found"))
    }

    @Test
    fun update_prompt_option_multiple_times_succeeds() = runTest {
        val option1 = PromptOption(id = 1L, key = "tone", name = "Formal", prompt = "Be formal")
        val option2 = PromptOption(id = 1L, key = "tone", name = "Very Formal", prompt = "Be very formal")
        val option3 = PromptOption(id = 1L, key = "tone", name = "Super Formal", prompt = "Be super formal")

        coEvery {
            repository.updatePromptOption(1L, "Very Formal", "Be very formal")
        } returns option2
        coEvery {
            repository.updatePromptOption(1L, "Super Formal", "Be super formal")
        } returns option3

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        var result1: PromptOption? = null
        viewModel.updatePromptOption(
            1L,
            "Very Formal",
            "Be very formal",
            onSuccess = { result1 = it },
            onError = {}
        )
        advanceUntilIdle()
        assertEquals(option2, result1)

        var result2: PromptOption? = null
        viewModel.updatePromptOption(
            1L,
            "Super Formal",
            "Be super formal",
            onSuccess = { result2 = it },
            onError = {}
        )
        advanceUntilIdle()
        assertEquals(option3, result2)
    }

    @Test
    fun delete_multiple_prompt_options_succeeds() = runTest {
        coEvery { repository.deletePromptOption(1L) } returns Unit
        coEvery { repository.deletePromptOption(2L) } returns Unit
        coEvery { repository.deletePromptOption(3L) } returns Unit

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        var successCount = 0
        listOf(1L, 2L, 3L).forEach { id ->
            viewModel.deletePromptOption(
                id,
                onSuccess = { successCount++ },
                onError = {}
            )
            advanceUntilIdle()
        }

        assertEquals(3, successCount)
        coVerify(exactly = 1) { repository.deletePromptOption(1L) }
        coVerify(exactly = 1) { repository.deletePromptOption(2L) }
        coVerify(exactly = 1) { repository.deletePromptOption(3L) }
    }

    @Test
    fun update_prompt_option_with_special_characters_succeeds() = runTest {
        val updated = PromptOption(
            id = 1L,
            key = "tone",
            name = "Formal!@#$%",
            prompt = "Be formal with special chars!@#"
        )

        coEvery {
            repository.updatePromptOption(1L, "Formal!@#$%", "Be formal with special chars!@#")
        } returns updated

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        var result: PromptOption? = null
        viewModel.updatePromptOption(
            1L,
            "Formal!@#$%",
            "Be formal with special chars!@#",
            onSuccess = { result = it },
            onError = {}
        )
        advanceUntilIdle()

        assertEquals(updated, result)
    }

    @Test
    fun update_prompt_option_with_long_text_succeeds() = runTest {
        val longName = "Very long name ".repeat(10)
        val longPrompt = "Very long prompt ".repeat(50)
        val updated = PromptOption(id = 1L, key = "tone", name = longName, prompt = longPrompt)

        coEvery {
            repository.updatePromptOption(1L, longName, longPrompt)
        } returns updated

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        var result: PromptOption? = null
        viewModel.updatePromptOption(
            1L,
            longName,
            longPrompt,
            onSuccess = { result = it },
            onError = {}
        )
        advanceUntilIdle()

        assertEquals(updated, result)
    }

    @Test
    fun update_prompt_option_with_unicode_characters_succeeds() = runTest {
        val updated = PromptOption(
            id = 1L,
            key = "tone",
            name = "한글 이름",
            prompt = "한국어로 작성하세요 🇰🇷"
        )

        coEvery {
            repository.updatePromptOption(1L, "한글 이름", "한국어로 작성하세요 🇰🇷")
        } returns updated

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        var result: PromptOption? = null
        viewModel.updatePromptOption(
            1L,
            "한글 이름",
            "한국어로 작성하세요 🇰🇷",
            onSuccess = { result = it },
            onError = {}
        )
        advanceUntilIdle()

        assertEquals(updated, result)
    }

    @Test
    fun delete_prompt_option_with_various_ids_succeeds() = runTest {
        val ids = listOf(1L, 100L, 999L, 12345L, Long.MAX_VALUE - 1)

        ids.forEach { id ->
            coEvery { repository.deletePromptOption(id) } returns Unit
        }

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        var successCount = 0
        ids.forEach { id ->
            viewModel.deletePromptOption(
                id,
                onSuccess = { successCount++ },
                onError = {}
            )
            advanceUntilIdle()
        }

        assertEquals(ids.size, successCount)
    }

    @Test
    fun update_then_delete_prompt_option_succeeds() = runTest {
        val updated = PromptOption(id = 1L, key = "tone", name = "Updated", prompt = "Updated prompt")

        coEvery {
            repository.updatePromptOption(1L, "Updated", "Updated prompt")
        } returns updated
        coEvery { repository.deletePromptOption(1L) } returns Unit

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        var updateSuccess = false
        viewModel.updatePromptOption(
            1L,
            "Updated",
            "Updated prompt",
            onSuccess = { updateSuccess = true },
            onError = {}
        )
        advanceUntilIdle()
        assertTrue(updateSuccess)

        var deleteSuccess = false
        viewModel.deletePromptOption(
            1L,
            onSuccess = { deleteSuccess = true },
            onError = {}
        )
        advanceUntilIdle()
        assertTrue(deleteSuccess)
    }

    @Test
    fun update_prompt_option_network_error_sets_error_state() = runTest {
        coEvery {
            repository.updatePromptOption(any(), any(), any())
        } throws Exception("Network unavailable")

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        viewModel.updatePromptOption(
            1L,
            "Test",
            "Test",
            onSuccess = {},
            onError = {}
        )
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error?.contains("Network") == true)
    }

    @Test
    fun delete_prompt_option_network_error_sets_error_state() = runTest {
        coEvery {
            repository.deletePromptOption(any())
        } throws Exception("Network unavailable")

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        viewModel.deletePromptOption(
            1L,
            onSuccess = {},
            onError = {}
        )
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error?.contains("Network") == true)
    }
}
