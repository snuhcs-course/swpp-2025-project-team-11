package com.fiveis.xend.ui.inbox

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.database.AppDatabase
import com.fiveis.xend.data.database.EmailDao
import com.fiveis.xend.data.repository.ContactBookRepository
import com.fiveis.xend.data.repository.InboxRepository
import com.fiveis.xend.network.MailApiService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InboxViewModelIntegrationTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var emailDao: EmailDao
    private lateinit var repository: InboxRepository
    private lateinit var contactRepository: ContactBookRepository
    private lateinit var viewModel: InboxViewModel
    private lateinit var mockApiService: MailApiService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries()
        .build()
        emailDao = database.emailDao()

        mockApiService = mockk(relaxed = true)
        repository = InboxRepository(mockApiService, emailDao)
        contactRepository = mockk()
        coEvery { contactRepository.observeGroups() } returns MutableStateFlow(emptyList())
        coEvery { contactRepository.observeContacts() } returns MutableStateFlow(emptyList())
        viewModel = InboxViewModel(repository, contactRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun viewModel_initial_state_is_correct() = runBlocking {
        withTimeout(3000) {
            // When
            val state = viewModel.uiState.first()

            // Then
            assertNotNull(state.emails)
        }
    }

    @Test
    fun refreshEmails_updates_state() = runBlocking {
        withTimeout(3000) {
            // When
            viewModel.refreshEmails()

            // Then
            val state = viewModel.uiState.first()
            assertNotNull(state.emails)
        }
    }

    @Test
    fun loadMoreEmails_without_token_does_nothing() = runBlocking {
        withTimeout(3000) {
            // Given
            val initialState = viewModel.uiState.first()

            // When
            viewModel.loadMoreEmails()

            // Then
            val afterLoadState = viewModel.uiState.first()
            assertEquals(initialState.emails.size, afterLoadState.emails.size)
        }
    }

    @Test
    fun onEmailClick_does_not_crash() = runBlocking {
        withTimeout(3000) {
            // Given
            val state = viewModel.uiState.first()

            // When
            if (state.emails.isNotEmpty()) {
                viewModel.onEmailClick(state.emails.first())
            }

            // Then - No crash
            assertNotNull(state)
        }
    }

    @Test
    fun loadMoreEmails_while_loading_does_nothing() = runBlocking {
        withTimeout(3000) {
            // When
            viewModel.loadMoreEmails()
            viewModel.loadMoreEmails()

            // Then
            val state = viewModel.uiState.first()
            assertNotNull(state.emails)
        }
    }

    @Test
    fun multiple_refreshes_handled_correctly() = runBlocking {
        withTimeout(3000) {
            // When
            viewModel.refreshEmails()
            viewModel.refreshEmails()

            // Then
            val state = viewModel.uiState.first()
            assertNotNull(state.emails)
        }
    }
}
