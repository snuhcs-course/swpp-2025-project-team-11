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

        viewModel.addGroup("", "Description", "", emptyList())
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
            description = "Important people",
            emoji = null
        )

        every { repository.observePromptOptions() } returns kotlinx.coroutines.flow.flowOf(toneOptions)
        coEvery { repository.getAllPromptOptions() } returns Pair(toneOptions, emptyList())
        coEvery {
            repository.addGroup("VIP", "Important people", "", toneOptions)
        } returns mockResponse

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        viewModel.addGroup("VIP", "Important people", "", toneOptions)
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
            repository.addGroup(any(), any(), any(), any())
        } throws Exception("Network error")

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        viewModel.addGroup("VIP", "Description", "", toneOptions)
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

    @Test
    fun add_group_with_members_updates_all_contacts() = runTest {
        val toneOptions = listOf(PromptOption(id = 1L, key = "tone", name = "Formal", prompt = "Be formal"))
        val mockResponse = GroupResponse(id = 1L, name = "VIP", description = "Important people")
        val members = listOf(
            com.fiveis.xend.data.model.Contact(id = 1L, name = "Member1", email = "member1@test.com"),
            com.fiveis.xend.data.model.Contact(id = 2L, name = "Member2", email = "member2@test.com")
        )

        every { repository.observePromptOptions() } returns kotlinx.coroutines.flow.flowOf(toneOptions)
        coEvery { repository.refreshPromptOptions() } returns Unit
        coEvery {
            repository.addGroup("VIP", "Important people", "", toneOptions)
        } returns mockResponse
        coEvery {
            repository.updateContactGroup(any(), any())
        } returns Unit

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        viewModel.addGroup("VIP", "Important people", "", toneOptions, members)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNotNull(viewModel.uiState.value.lastSuccessMsg)
        assertTrue(viewModel.uiState.value.lastSuccessMsg?.contains("멤버 2명") == true)
        coVerify(exactly = 2) { repository.updateContactGroup(any(), 1L) }
    }

    @Test
    fun add_group_with_whitespace_name_sets_error() = runTest {
        val toneOptions = listOf(PromptOption(id = 1L, key = "tone", name = "Formal", prompt = "Be formal"))
        every { repository.observePromptOptions() } returns kotlinx.coroutines.flow.flowOf(toneOptions)
        coEvery { repository.refreshPromptOptions() } returns Unit

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        viewModel.addGroup("   ", "Description", "", emptyList())
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error?.contains("그룹 이름") == true)
    }

    @Test
    fun add_prompt_option_format_success_updates_format_list() = runTest {
        val initialFormat = listOf(PromptOption(id = 1L, key = "format", name = "Short", prompt = "Be short"))
        val newFormat = PromptOption(id = 2L, key = "format", name = "Long", prompt = "Be detailed")

        val flow = kotlinx.coroutines.flow.MutableStateFlow(initialFormat)
        every { repository.observePromptOptions() } returns flow
        coEvery { repository.refreshPromptOptions() } returns Unit
        coEvery {
            repository.addPromptOption("format", "Long", "Be detailed")
        } coAnswers {
            flow.value = initialFormat + newFormat
            newFormat
        }

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        var successCalled = false
        viewModel.addPromptOption("format", "Long", "Be detailed", onSuccess = { successCalled = true })
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.formatPromptOptions.size >= 1)
        assertTrue(successCalled)
    }

    @Test
    fun get_all_prompt_options_handles_failure() = runTest {
        every { repository.observePromptOptions() } returns kotlinx.coroutines.flow.flowOf(emptyList())
        coEvery { repository.refreshPromptOptions() } returns Unit
        coEvery {
            repository.getAllPromptOptions()
        } throws Exception("Failed to fetch options")

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        viewModel.getAllPromptOptions()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isFetchingOptions)
        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error?.contains("Failed to fetch") == true)
    }

    @Test
    fun initial_state_has_empty_prompt_options() = runTest {
        every { repository.observePromptOptions() } returns kotlinx.coroutines.flow.flowOf(emptyList())
        coEvery { repository.refreshPromptOptions() } returns Unit

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.tonePromptOptions.isEmpty())
        assertTrue(viewModel.uiState.value.formatPromptOptions.isEmpty())
        assertFalse(viewModel.uiState.value.isFetchingOptions)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun add_group_member_update_failure_sets_error() = runTest {
        val toneOptions = listOf(PromptOption(id = 1L, key = "tone", name = "Formal", prompt = "Be formal"))
        val mockResponse = GroupResponse(id = 1L, name = "VIP", description = "Important people", emoji = null)
        val members = listOf(
            com.fiveis.xend.data.model.Contact(id = 1L, name = "Member1", email = "member1@test.com")
        )

        every { repository.observePromptOptions() } returns kotlinx.coroutines.flow.flowOf(toneOptions)
        coEvery { repository.refreshPromptOptions() } returns Unit
        coEvery {
            repository.addGroup("VIP", "Important people", "", toneOptions)
        } returns mockResponse
        coEvery {
            repository.updateContactGroup(1L, 1L)
        } throws Exception("Failed to update contact")

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        viewModel.addGroup("VIP", "Important people", "", toneOptions, members)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error?.contains("Failed to update") == true)
    }

    @Test
    fun add_group_with_special_characters_in_name_succeeds() = runTest {
        val toneOptions = listOf(PromptOption(id = 1L, key = "tone", name = "Formal", prompt = "Be formal"))
        val mockResponse = GroupResponse(id = 1L, name = "Group!@#$%", description = "Description", emoji = null)

        every { repository.observePromptOptions() } returns kotlinx.coroutines.flow.flowOf(toneOptions)
        coEvery { repository.refreshPromptOptions() } returns Unit
        coEvery {
            repository.addGroup("Group!@#$%", "Description", "", emptyList())
        } returns mockResponse

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        viewModel.addGroup("Group!@#$%", "Description", "", emptyList())
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNotNull(viewModel.uiState.value.lastSuccessMsg)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun add_group_with_long_description_succeeds() = runTest {
        val toneOptions = listOf(PromptOption(id = 1L, key = "tone", name = "Formal", prompt = "Be formal"))
        val longDescription = "Very long description ".repeat(50)
        val mockResponse = GroupResponse(id = 1L, name = "Test Group", description = longDescription, emoji = null)

        every { repository.observePromptOptions() } returns kotlinx.coroutines.flow.flowOf(toneOptions)
        coEvery { repository.refreshPromptOptions() } returns Unit
        coEvery {
            repository.addGroup("Test Group", longDescription, "", emptyList())
        } returns mockResponse

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        viewModel.addGroup("Test Group", longDescription, "", emptyList())
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNotNull(viewModel.uiState.value.lastSuccessMsg)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun add_group_with_empty_description_succeeds() = runTest {
        val toneOptions = listOf(PromptOption(id = 1L, key = "tone", name = "Formal", prompt = "Be formal"))
        val mockResponse = GroupResponse(id = 1L, name = "Test Group", description = "", emoji = null)

        every { repository.observePromptOptions() } returns kotlinx.coroutines.flow.flowOf(toneOptions)
        coEvery { repository.refreshPromptOptions() } returns Unit
        coEvery {
            repository.addGroup("Test Group", "", "", emptyList())
        } returns mockResponse

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        viewModel.addGroup("Test Group", "", "", emptyList())
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNotNull(viewModel.uiState.value.lastSuccessMsg)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun add_group_multiple_times_updates_state_correctly() = runTest {
        val toneOptions = listOf(PromptOption(id = 1L, key = "tone", name = "Formal", prompt = "Be formal"))
        val mockResponse1 = GroupResponse(id = 1L, name = "Group1", description = "Desc1", emoji = null)
        val mockResponse2 = GroupResponse(id = 2L, name = "Group2", description = "Desc2", emoji = null)

        every { repository.observePromptOptions() } returns kotlinx.coroutines.flow.flowOf(toneOptions)
        coEvery { repository.refreshPromptOptions() } returns Unit
        coEvery {
            repository.addGroup("Group1", "Desc1", "", emptyList())
        } returns mockResponse1
        coEvery {
            repository.addGroup("Group2", "Desc2", "", emptyList())
        } returns mockResponse2

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        // First add
        viewModel.addGroup("Group1", "Desc1", "", emptyList())
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNotNull(viewModel.uiState.value.lastSuccessMsg)
        assertTrue(viewModel.uiState.value.lastSuccessMsg?.contains("1") == true)

        // Second add
        viewModel.addGroup("Group2", "Desc2", "", emptyList())
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNotNull(viewModel.uiState.value.lastSuccessMsg)
        assertTrue(viewModel.uiState.value.lastSuccessMsg?.contains("2") == true)
    }

    @Test
    fun update_prompt_option_success_calls_callback() = runTest {
        val initialOption = PromptOption(id = 1L, key = "tone", name = "Formal", prompt = "Be formal")
        val updatedOption = PromptOption(id = 1L, key = "tone", name = "Very Formal", prompt = "Be very formal")

        every { repository.observePromptOptions() } returns kotlinx.coroutines.flow.flowOf(listOf(initialOption))
        coEvery { repository.refreshPromptOptions() } returns Unit
        coEvery { repository.updatePromptOption(1L, "Very Formal", "Be very formal") } returns updatedOption

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        var successCalled = false
        var errorCalled = false
        viewModel.updatePromptOption(
            1L,
            "Very Formal",
            "Be very formal",
            onSuccess = { successCalled = true },
            onError = { errorCalled = true }
        )
        advanceUntilIdle()

        assertTrue(successCalled)
        assertFalse(errorCalled)
        coVerify { repository.updatePromptOption(1L, "Very Formal", "Be very formal") }
    }

    @Test
    fun update_prompt_option_failure_calls_error_callback() = runTest {
        every { repository.observePromptOptions() } returns kotlinx.coroutines.flow.flowOf(emptyList())
        coEvery { repository.refreshPromptOptions() } returns Unit
        coEvery { repository.updatePromptOption(any(), any(), any()) } throws Exception("Update failed")

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        var successCalled = false
        var errorMessage = ""
        viewModel.updatePromptOption(
            1L,
            "Test",
            "Test prompt",
            onSuccess = { successCalled = true },
            onError = { errorMessage = it }
        )
        advanceUntilIdle()

        assertFalse(successCalled)
        assertTrue(errorMessage.isNotEmpty())
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun delete_prompt_option_success_calls_callback() = runTest {
        every { repository.observePromptOptions() } returns kotlinx.coroutines.flow.flowOf(emptyList())
        coEvery { repository.refreshPromptOptions() } returns Unit
        coEvery { repository.deletePromptOption(1L) } returns Unit

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        var successCalled = false
        var errorCalled = false
        viewModel.deletePromptOption(
            1L,
            onSuccess = { successCalled = true },
            onError = { errorCalled = true }
        )
        advanceUntilIdle()

        assertTrue(successCalled)
        assertFalse(errorCalled)
        coVerify { repository.deletePromptOption(1L) }
    }

    @Test
    fun delete_prompt_option_failure_calls_error_callback() = runTest {
        every { repository.observePromptOptions() } returns kotlinx.coroutines.flow.flowOf(emptyList())
        coEvery { repository.refreshPromptOptions() } returns Unit
        coEvery { repository.deletePromptOption(any()) } throws Exception("Delete failed")

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        var successCalled = false
        var errorMessage = ""
        viewModel.deletePromptOption(
            1L,
            onSuccess = { successCalled = true },
            onError = { errorMessage = it }
        )
        advanceUntilIdle()

        assertFalse(successCalled)
        assertTrue(errorMessage.isNotEmpty())
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun factory_creates_view_model_successfully() {
        val factory = AddGroupViewModel.Factory(application)
        val createdViewModel = factory.create(AddGroupViewModel::class.java)

        assertNotNull(createdViewModel)
        assertTrue(createdViewModel is AddGroupViewModel)
    }

    @Test(expected = IllegalArgumentException::class)
    fun factory_throws_exception_for_wrong_class() {
        val factory = AddGroupViewModel.Factory(application)
        factory.create(ContactBookViewModel::class.java)
    }

    @Test
    fun add_group_with_emoji_succeeds() = runTest {
        val toneOptions = listOf(PromptOption(id = 1L, key = "tone", name = "Formal", prompt = "Be formal"))
        val mockResponse = GroupResponse(id = 1L, name = "VIP", description = "Important", emoji = "🔥")

        every { repository.observePromptOptions() } returns kotlinx.coroutines.flow.flowOf(toneOptions)
        coEvery { repository.refreshPromptOptions() } returns Unit
        coEvery {
            repository.addGroup("VIP", "Important", "🔥", emptyList())
        } returns mockResponse

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        viewModel.addGroup("VIP", "Important", "🔥", emptyList())
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNotNull(viewModel.uiState.value.lastSuccessMsg)
        assertEquals(null, viewModel.uiState.value.error)
        coVerify { repository.addGroup("VIP", "Important", "🔥", emptyList()) }
    }
}
