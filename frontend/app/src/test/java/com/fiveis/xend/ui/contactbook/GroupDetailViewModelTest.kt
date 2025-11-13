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
}
