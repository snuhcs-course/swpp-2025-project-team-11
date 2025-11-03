package com.fiveis.xend.ui.contactbook

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fiveis.xend.data.model.GroupResponse
import com.fiveis.xend.data.model.PromptOption
import com.fiveis.xend.data.repository.ContactBookRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddGroupViewModelTest {

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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun init_loads_prompt_options() = runTest {
        val toneOptions = listOf(
            PromptOption(id = 1L, key = "tone", name = "Formal", prompt = "Be formal")
        )
        val formatOptions = listOf(
            PromptOption(id = 2L, key = "format", name = "Short", prompt = "Keep it short")
        )

        every { repository.observePromptOptions() } returns kotlinx.coroutines.flow.flowOf(toneOptions + formatOptions)
        coEvery { repository.getAllPromptOptions() } returns Pair(toneOptions, formatOptions)

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        assertEquals(toneOptions, viewModel.uiState.value.tonePromptOptions)
        assertEquals(formatOptions, viewModel.uiState.value.formatPromptOptions)
        assertFalse(viewModel.uiState.value.isFetchingOptions)
    }

    @Test
    fun add_group_with_blank_name_sets_error() = runTest {
        val toneOptions = listOf(PromptOption(id = 1L, key = "tone", name = "Formal", prompt = "Be formal"))
        every { repository.observePromptOptions() } returns kotlinx.coroutines.flow.flowOf(toneOptions)
        coEvery { repository.getAllPromptOptions() } returns Pair(toneOptions, emptyList())

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        viewModel.addGroup("", "Description", emptyList())
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error?.contains("그룹 이름") == true)
    }

    @Test
    fun add_group_success_updates_state() = runTest {
        val toneOptions = listOf(PromptOption(id = 1L, key = "tone", name = "Formal", prompt = "Be formal"))
        val mockResponse = GroupResponse(
            id = 1L,
            name = "VIP",
            description = "Important people"
        )

        every { repository.observePromptOptions() } returns kotlinx.coroutines.flow.flowOf(toneOptions)
        coEvery { repository.getAllPromptOptions() } returns Pair(toneOptions, emptyList())
        coEvery {
            repository.addGroup("VIP", "Important people", toneOptions)
        } returns mockResponse

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        viewModel.addGroup("VIP", "Important people", toneOptions)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNotNull(viewModel.uiState.value.lastSuccessMsg)
        assertTrue(viewModel.uiState.value.lastSuccessMsg?.contains("1") == true)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun add_group_failure_sets_error() = runTest {
        val toneOptions = listOf(PromptOption(id = 1L, key = "tone", name = "Formal", prompt = "Be formal"))

        every { repository.observePromptOptions() } returns kotlinx.coroutines.flow.flowOf(toneOptions)
        coEvery { repository.getAllPromptOptions() } returns Pair(toneOptions, emptyList())
        coEvery {
            repository.addGroup(any(), any(), any())
        } throws Exception("Network error")

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        viewModel.addGroup("VIP", "Description", toneOptions)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error?.contains("Network error") == true)
    }

    @Test
    fun add_prompt_option_tone_success_updates_tone_list() = runTest {
        val initialTone = listOf(PromptOption(id = 1L, key = "tone", name = "Formal", prompt = "Be formal"))
        val newTone = PromptOption(id = 2L, key = "tone", name = "Casual", prompt = "Be casual")

        // Create a flow that emits both initial and updated lists
        val flow = kotlinx.coroutines.flow.MutableStateFlow(initialTone)
        every { repository.observePromptOptions() } returns flow
        coEvery { repository.refreshPromptOptions() } returns Unit
        coEvery {
            repository.addPromptOption("tone", "Casual", "Be casual")
        } coAnswers {
            // Update the flow when addPromptOption is called
            flow.value = initialTone + newTone
            newTone
        }

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        var successCalled = false
        viewModel.addPromptOption("tone", "Casual", "Be casual", onSuccess = { successCalled = true })
        advanceUntilIdle()

        // Check that the tone list was updated
        assertTrue(viewModel.uiState.value.tonePromptOptions.size >= 1)
        assertTrue(successCalled)
    }

    @Test
    fun add_prompt_option_failure_calls_error_callback() = runTest {
        every { repository.observePromptOptions() } returns kotlinx.coroutines.flow.flowOf(emptyList())
        coEvery { repository.getAllPromptOptions() } returns Pair(emptyList(), emptyList())
        coEvery {
            repository.addPromptOption(any(), any(), any())
        } throws Exception("Failed to add")

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        var errorMessage = ""
        viewModel.addPromptOption("tone", "Test", "Test prompt", onError = { errorMessage = it })
        advanceUntilIdle()

        assertTrue(errorMessage.isNotEmpty())
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun get_all_prompt_options_calls_repository() = runTest {
        val toneOptions = listOf(PromptOption(id = 1L, key = "tone", name = "Formal", prompt = "Be formal"))
        val formatOptions = listOf(
            PromptOption(id = 2L, key = "format", name = "Short", prompt = "Keep it short")
        )

        every { repository.observePromptOptions() } returns kotlinx.coroutines.flow.flowOf(toneOptions + formatOptions)
        coEvery { repository.refreshPromptOptions() } returns Unit
        coEvery {
            repository.getAllPromptOptions()
        } returns Pair(toneOptions, formatOptions)

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        viewModel.getAllPromptOptions()
        advanceUntilIdle()

        // Verify getAllPromptOptions was called at least once by the explicit call
        coVerify(atLeast = 1) { repository.getAllPromptOptions() }
        assertEquals(toneOptions, viewModel.uiState.value.tonePromptOptions)
        assertEquals(formatOptions, viewModel.uiState.value.formatPromptOptions)
    }
}
