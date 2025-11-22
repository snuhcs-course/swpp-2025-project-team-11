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
class AddGroupViewModelEmojiTest {

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
    fun add_group_with_emoji_passes_emoji_to_repository() = runTest {
        val emoji = "🎉"
        val mockResponse = GroupResponse(id = 1L, name = "Party Group", description = "Fun group", emoji = emoji)

        coEvery {
            repository.addGroup("Party Group", "Fun group", emoji, emptyList())
        } returns mockResponse

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        viewModel.addGroup("Party Group", "Fun group", emoji, emptyList())
        advanceUntilIdle()

        coVerify { repository.addGroup("Party Group", "Fun group", emoji, emptyList()) }
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNotNull(viewModel.uiState.value.lastSuccessMsg)
    }

    @Test
    fun add_group_with_null_emoji_succeeds() = runTest {
        val mockResponse = GroupResponse(id = 1L, name = "No Emoji Group", description = "Description", emoji = null)

        coEvery {
            repository.addGroup("No Emoji Group", "Description", "", emptyList())
        } returns mockResponse

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        viewModel.addGroup("No Emoji Group", "Description", "", emptyList())
        advanceUntilIdle()

        coVerify { repository.addGroup("No Emoji Group", "Description", "", emptyList()) }
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNotNull(viewModel.uiState.value.lastSuccessMsg)
    }

    @Test
    fun add_group_with_multiple_emojis_succeeds() = runTest {
        val emojis = "🎉🎊🎈"
        val mockResponse = GroupResponse(id = 1L, name = "Party Group", description = "Fun", emoji = emojis)

        coEvery {
            repository.addGroup("Party Group", "Fun", emojis, emptyList())
        } returns mockResponse

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        viewModel.addGroup("Party Group", "Fun", emojis, emptyList())
        advanceUntilIdle()

        coVerify { repository.addGroup("Party Group", "Fun", emojis, emptyList()) }
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun add_group_with_emoji_and_members_succeeds() = runTest {
        val emoji = "👥"
        val mockResponse = GroupResponse(id = 1L, name = "Team", description = "Team group", emoji = emoji)
        val members = listOf(
            com.fiveis.xend.data.model.Contact(id = 1L, name = "Member1", email = "m1@test.com")
        )

        coEvery {
            repository.addGroup("Team", "Team group", emoji, emptyList())
        } returns mockResponse
        coEvery { repository.updateContactGroup(any(), any()) } returns Unit

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        viewModel.addGroup("Team", "Team group", emoji, emptyList(), members)
        advanceUntilIdle()

        coVerify { repository.addGroup("Team", "Team group", emoji, emptyList()) }
        coVerify { repository.updateContactGroup(1L, 1L) }
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertTrue(viewModel.uiState.value.lastSuccessMsg?.contains("멤버 1명") == true)
    }

    @Test
    fun add_group_with_unicode_emoji_succeeds() = runTest {
        val emoji = "\uD83D\uDE80" // 🚀
        val mockResponse = GroupResponse(id = 1L, name = "Rocket", description = "Space", emoji = emoji)

        coEvery {
            repository.addGroup("Rocket", "Space", emoji, emptyList())
        } returns mockResponse

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        viewModel.addGroup("Rocket", "Space", emoji, emptyList())
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNotNull(viewModel.uiState.value.lastSuccessMsg)
    }

    @Test
    fun add_group_with_skin_tone_emoji_succeeds() = runTest {
        val emoji = "👋🏻" // Wave with skin tone
        val mockResponse = GroupResponse(id = 1L, name = "Hello", description = "Greeting", emoji = emoji)

        coEvery {
            repository.addGroup("Hello", "Greeting", emoji, emptyList())
        } returns mockResponse

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        viewModel.addGroup("Hello", "Greeting", emoji, emptyList())
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNotNull(viewModel.uiState.value.lastSuccessMsg)
    }

    @Test
    fun add_group_with_composite_emoji_succeeds() = runTest {
        val emoji = "👨‍👩‍👧‍👦" // Family emoji
        val mockResponse = GroupResponse(id = 1L, name = "Family", description = "Family group", emoji = emoji)

        coEvery {
            repository.addGroup("Family", "Family group", emoji, emptyList())
        } returns mockResponse

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        viewModel.addGroup("Family", "Family group", emoji, emptyList())
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNotNull(viewModel.uiState.value.lastSuccessMsg)
    }

    @Test
    fun add_group_with_flag_emoji_succeeds() = runTest {
        val emoji = "🇰🇷" // South Korea flag
        val mockResponse = GroupResponse(id = 1L, name = "Korea", description = "Korean contacts", emoji = emoji)

        coEvery {
            repository.addGroup("Korea", "Korean contacts", emoji, emptyList())
        } returns mockResponse

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        viewModel.addGroup("Korea", "Korean contacts", emoji, emptyList())
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNotNull(viewModel.uiState.value.lastSuccessMsg)
    }

    @Test
    fun add_group_with_various_category_emojis_succeeds() = runTest {
        val emojis = listOf("😀", "🎨", "⚽", "🍕", "🏠", "💼", "📱", "🌟", "🎵", "📚")

        emojis.forEachIndexed { index, emoji ->
            val mockResponse = GroupResponse(id = index.toLong() + 1, name = "Group$index", description = "Desc", emoji = emoji)
            coEvery {
                repository.addGroup("Group$index", "Desc", emoji, emptyList())
            } returns mockResponse
        }

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        emojis.forEachIndexed { index, emoji ->
            viewModel.addGroup("Group$index", "Desc", emoji, emptyList())
            advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.lastSuccessMsg)
        }
    }

    @Test
    fun add_group_emoji_persists_through_multiple_operations() = runTest {
        val emoji1 = "🔥"
        val emoji2 = "⭐"
        val mockResponse1 = GroupResponse(id = 1L, name = "Hot", description = "Hot group", emoji = emoji1)
        val mockResponse2 = GroupResponse(id = 2L, name = "Star", description = "Star group", emoji = emoji2)

        coEvery {
            repository.addGroup("Hot", "Hot group", emoji1, emptyList())
        } returns mockResponse1
        coEvery {
            repository.addGroup("Star", "Star group", emoji2, emptyList())
        } returns mockResponse2

        viewModel = AddGroupViewModel(application, repository)
        advanceUntilIdle()

        viewModel.addGroup("Hot", "Hot group", emoji1, emptyList())
        advanceUntilIdle()
        coVerify { repository.addGroup("Hot", "Hot group", emoji1, emptyList()) }

        viewModel.addGroup("Star", "Star group", emoji2, emptyList())
        advanceUntilIdle()
        coVerify { repository.addGroup("Star", "Star group", emoji2, emptyList()) }
    }
}
