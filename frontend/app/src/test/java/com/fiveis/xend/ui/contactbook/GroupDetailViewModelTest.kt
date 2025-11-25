package com.fiveis.xend.ui.contactbook

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fiveis.xend.data.model.Group
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GroupDetailViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var application: Application
    private lateinit var repository: ContactBookRepository
    private lateinit var viewModel: GroupDetailViewModel

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
    fun load_group_success_updates_state() = runTest {
        val groupId = 1L
        val mockGroup = Group(
            id = groupId,
            name = "VIP",
            description = "Important people"
        )

        every { repository.observeGroup(groupId) } returns kotlinx.coroutines.flow.flowOf(mockGroup)
        coEvery { repository.refreshGroupAndMembers(groupId) } returns Unit

        viewModel = GroupDetailViewModel(application, repository)

        viewModel.load(groupId)
        advanceUntilIdle()

        assertEquals(mockGroup, viewModel.uiState.value.group)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun load_group_failure_sets_error() = runTest {
        val groupId = 1L

        every { repository.observeGroup(groupId) } returns kotlinx.coroutines.flow.flowOf(null)
        coEvery { repository.refreshGroupAndMembers(groupId) } throws Exception("Network error")

        viewModel = GroupDetailViewModel(application, repository)

        viewModel.load(groupId)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun load_same_group_without_force_does_nothing() = runTest {
        val groupId = 1L
        val mockGroup = Group(
            id = groupId,
            name = "VIP",
            description = "Important people"
        )

        every { repository.observeGroup(groupId) } returns kotlinx.coroutines.flow.flowOf(mockGroup)
        coEvery { repository.refreshGroupAndMembers(groupId) } returns Unit

        viewModel = GroupDetailViewModel(application, repository)

        viewModel.load(groupId)
        advanceUntilIdle()

        viewModel.load(groupId, force = false)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.refreshGroupAndMembers(groupId) }
    }

    @Test
    fun load_same_group_with_force_reloads() = runTest {
        val groupId = 1L
        val mockGroup = Group(
            id = groupId,
            name = "VIP",
            description = "Important people"
        )

        every { repository.observeGroup(groupId) } returns kotlinx.coroutines.flow.flowOf(mockGroup)
        coEvery { repository.refreshGroupAndMembers(groupId) } returns Unit

        viewModel = GroupDetailViewModel(application, repository)

        viewModel.load(groupId)
        advanceUntilIdle()

        viewModel.load(groupId, force = true)
        advanceUntilIdle()

        coVerify(exactly = 2) { repository.refreshGroupAndMembers(groupId) }
    }

    @Test
    fun remove_member_from_group_invokes_repository() = runTest {
        viewModel = GroupDetailViewModel(application, repository)
        coEvery { repository.updateContactGroup(42L, null) } returns Unit

        viewModel.removeMemberFromGroup(42L)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.updateContactGroup(42L, null) }
    }

    @Test
    fun remove_member_from_group_failure_sets_error() = runTest {
        viewModel = GroupDetailViewModel(application, repository)
        coEvery { repository.updateContactGroup(42L, null) } throws Exception("Failed to remove member")

        viewModel.removeMemberFromGroup(42L)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun add_members_to_group_invokes_repository() = runTest {
        val groupId = 1L
        val mockGroup = Group(id = groupId, name = "VIP", description = "Important")

        every { repository.observeGroup(groupId) } returns kotlinx.coroutines.flow.flowOf(mockGroup)
        coEvery { repository.refreshGroupAndMembers(groupId) } returns Unit
        coEvery { repository.updateContactGroup(any(), any()) } returns Unit

        viewModel = GroupDetailViewModel(application, repository)
        viewModel.load(groupId)
        advanceUntilIdle()

        viewModel.addMembersToGroup(listOf(1L, 2L, 3L))
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.updateContactGroup(1L, groupId) }
        coVerify(exactly = 1) { repository.updateContactGroup(2L, groupId) }
        coVerify(exactly = 1) { repository.updateContactGroup(3L, groupId) }
    }

    @Test
    fun add_members_to_group_failure_sets_error() = runTest {
        val groupId = 1L
        val mockGroup = Group(id = groupId, name = "VIP", description = "Important")

        every { repository.observeGroup(groupId) } returns kotlinx.coroutines.flow.flowOf(mockGroup)
        coEvery { repository.refreshGroupAndMembers(groupId) } returns Unit
        coEvery { repository.updateContactGroup(any(), any()) } throws Exception("Failed to add member")

        viewModel = GroupDetailViewModel(application, repository)
        viewModel.load(groupId)
        advanceUntilIdle()

        viewModel.addMembersToGroup(listOf(1L))
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun rename_group_success() = runTest {
        val groupId = 1L
        val mockGroup = Group(id = groupId, name = "VIP", description = "Important")

        every { repository.observeGroup(groupId) } returns kotlinx.coroutines.flow.flowOf(mockGroup)
        coEvery { repository.refreshGroupAndMembers(groupId) } returns Unit
        coEvery { repository.updateGroup(groupId, "New Name", "New Description", "", true) } returns mockk()

        viewModel = GroupDetailViewModel(application, repository)
        viewModel.load(groupId)
        advanceUntilIdle()

        viewModel.renameGroup("New Name", "New Description", "")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRenaming)
        assertNull(viewModel.uiState.value.renameError)
        coVerify { repository.updateGroup(groupId, "New Name", "New Description", "", true) }
    }

    @Test
    fun rename_group_with_blank_name_sets_error() = runTest {
        val groupId = 1L
        val mockGroup = Group(id = groupId, name = "VIP", description = "Important")

        every { repository.observeGroup(groupId) } returns kotlinx.coroutines.flow.flowOf(mockGroup)
        coEvery { repository.refreshGroupAndMembers(groupId) } returns Unit

        viewModel = GroupDetailViewModel(application, repository)
        viewModel.load(groupId)
        advanceUntilIdle()

        viewModel.renameGroup("   ", "Description", "")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.renameError)
    }

    @Test
    fun rename_group_failure_sets_error() = runTest {
        val groupId = 1L
        val mockGroup = Group(id = groupId, name = "VIP", description = "Important")

        every { repository.observeGroup(groupId) } returns kotlinx.coroutines.flow.flowOf(mockGroup)
        coEvery { repository.refreshGroupAndMembers(groupId) } returns Unit
        coEvery { repository.updateGroup(any(), any(), any(), any(), any()) } throws Exception("Rename failed")

        viewModel = GroupDetailViewModel(application, repository)
        viewModel.load(groupId)
        advanceUntilIdle()

        viewModel.renameGroup("New Name", "New Description", "")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRenaming)
        assertNotNull(viewModel.uiState.value.renameError)
    }

    @Test
    fun clear_rename_error() = runTest {
        val groupId = 1L
        val mockGroup = Group(id = groupId, name = "VIP", description = "Important")

        every { repository.observeGroup(groupId) } returns kotlinx.coroutines.flow.flowOf(mockGroup)
        coEvery { repository.refreshGroupAndMembers(groupId) } returns Unit

        viewModel = GroupDetailViewModel(application, repository)
        viewModel.load(groupId)
        advanceUntilIdle()

        viewModel.renameGroup("", "Description", "")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.renameError)

        viewModel.clearRenameError()

        assertNull(viewModel.uiState.value.renameError)
    }

    @Test
    fun refresh_group_success() = runTest {
        val groupId = 1L
        val mockGroup = Group(id = groupId, name = "VIP", description = "Important")

        every { repository.observeGroup(groupId) } returns kotlinx.coroutines.flow.flowOf(mockGroup)
        coEvery { repository.refreshGroupAndMembers(groupId) } returns Unit

        viewModel = GroupDetailViewModel(application, repository)
        viewModel.load(groupId)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
        coVerify(exactly = 2) { repository.refreshGroupAndMembers(groupId) }
    }

    @Test
    fun refresh_group_failure_sets_error() = runTest {
        val groupId = 1L
        val mockGroup = Group(id = groupId, name = "VIP", description = "Important")

        every { repository.observeGroup(groupId) } returns kotlinx.coroutines.flow.flowOf(mockGroup)
        coEvery { repository.refreshGroupAndMembers(groupId) } returns Unit andThenThrows Exception("Refresh failed")

        viewModel = GroupDetailViewModel(application, repository)
        viewModel.load(groupId)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun update_group_prompt_options_success() = runTest {
        val groupId = 1L
        val mockGroup = Group(id = groupId, name = "VIP", description = "Important")

        every { repository.observeGroup(groupId) } returns kotlinx.coroutines.flow.flowOf(mockGroup)
        coEvery { repository.refreshGroupAndMembers(groupId) } returns Unit
        coEvery { repository.updateGroup(groupId = groupId, optionIds = listOf(1L, 2L)) } returns mockk()

        viewModel = GroupDetailViewModel(application, repository)
        viewModel.load(groupId)
        advanceUntilIdle()

        viewModel.updateGroupPromptOptions(listOf(1L, 2L))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isPromptSaving)
        assertNull(viewModel.uiState.value.promptOptionsError)
        coVerify { repository.updateGroup(groupId = groupId, optionIds = listOf(1L, 2L)) }
    }

    @Test
    fun update_group_prompt_options_failure_sets_error() = runTest {
        val groupId = 1L
        val mockGroup = Group(id = groupId, name = "VIP", description = "Important")

        every { repository.observeGroup(groupId) } returns kotlinx.coroutines.flow.flowOf(mockGroup)
        coEvery { repository.refreshGroupAndMembers(groupId) } returns Unit
        coEvery { repository.updateGroup(groupId = any(), optionIds = any()) } throws Exception("Update failed")

        viewModel = GroupDetailViewModel(application, repository)
        viewModel.load(groupId)
        advanceUntilIdle()

        viewModel.updateGroupPromptOptions(listOf(1L, 2L))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isPromptSaving)
        assertNotNull(viewModel.uiState.value.promptOptionsError)
    }

    @Test
    fun clear_prompt_options_error() = runTest {
        val groupId = 1L
        val mockGroup = Group(id = groupId, name = "VIP", description = "Important")

        every { repository.observeGroup(groupId) } returns kotlinx.coroutines.flow.flowOf(mockGroup)
        coEvery { repository.refreshGroupAndMembers(groupId) } returns Unit
        coEvery { repository.updateGroup(groupId = any(), optionIds = any()) } throws Exception("Update failed")

        viewModel = GroupDetailViewModel(application, repository)
        viewModel.load(groupId)
        advanceUntilIdle()

        viewModel.updateGroupPromptOptions(listOf(1L))
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.promptOptionsError)

        viewModel.clearPromptOptionsError()

        assertNull(viewModel.uiState.value.promptOptionsError)
    }
}
