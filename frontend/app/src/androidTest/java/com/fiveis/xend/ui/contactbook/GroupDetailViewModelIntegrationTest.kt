package com.fiveis.xend.ui.contactbook

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.repository.ContactBookRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GroupDetailViewModelIntegrationTest {

    private lateinit var application: Application
    private lateinit var repository: ContactBookRepository
    private lateinit var viewModel: GroupDetailViewModel

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        repository = mockk(relaxed = true)
    }

    @Test
    fun viewModel_initial_state_is_correct() = runBlocking {
        // Given
        viewModel = GroupDetailViewModel(application, repository)

        // When
        val state = viewModel.uiState.first()

        // Then
        assertFalse(state.isLoading)
        assertNull(state.group)
        assertNull(state.error)
    }

    @Test
    fun load_with_invalid_id_handles_error() = runBlocking {
        // Given
        every { repository.observeGroup(999999L) } returns flowOf(null)
        coEvery { repository.refreshGroupAndMembers(999999L) } throws Exception("Group not found")
        viewModel = GroupDetailViewModel(application, repository)

        // When
        viewModel.load(999999L)
        Thread.sleep(500)

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }

    @Test
    fun load_same_id_twice_without_force_does_not_reload() = runBlocking {
        // Given
        val mockGroup = Group(id = 1L, name = "Test Group")
        every { repository.observeGroup(1L) } returns flowOf(mockGroup)
        coEvery { repository.refreshGroupAndMembers(1L) } returns Unit
        viewModel = GroupDetailViewModel(application, repository)

        viewModel.load(1L)
        Thread.sleep(500)
        val firstState = viewModel.uiState.first()

        // When
        viewModel.load(1L, force = false)
        Thread.sleep(200)

        // Then
        val secondState = viewModel.uiState.first()
        assertEquals(firstState.isLoading, secondState.isLoading)
    }

    @Test
    fun refresh_without_loading_does_nothing() = runBlocking {
        // Given
        viewModel = GroupDetailViewModel(application, repository)
        val initialState = viewModel.uiState.first()

        // When
        viewModel.refresh()
        Thread.sleep(200)

        // Then
        val afterRefreshState = viewModel.uiState.first()
        assertEquals(initialState.isLoading, afterRefreshState.isLoading)
    }

    @Test
    fun load_with_force_reloads_data() = runBlocking {
        // Given
        val mockGroup = Group(id = 1L, name = "Test Group")
        every { repository.observeGroup(1L) } returns flowOf(mockGroup)
        coEvery { repository.refreshGroupAndMembers(1L) } returns Unit
        viewModel = GroupDetailViewModel(application, repository)

        viewModel.load(1L)
        Thread.sleep(500)

        // When
        viewModel.load(1L, force = true)
        Thread.sleep(500)

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertEquals(mockGroup, state.group)
    }
}
