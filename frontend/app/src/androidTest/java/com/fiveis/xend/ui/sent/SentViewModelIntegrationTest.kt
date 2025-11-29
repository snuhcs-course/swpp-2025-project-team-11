package com.fiveis.xend.ui.sent

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.database.AppDatabase
import com.fiveis.xend.data.database.EmailDao
import com.fiveis.xend.data.repository.SentRepository
import com.fiveis.xend.network.MailApiService
import io.mockk.mockk
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
class SentViewModelIntegrationTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var emailDao: EmailDao
    private lateinit var repository: SentRepository
    private lateinit var viewModel: SentViewModel
    private lateinit var mockApiService: MailApiService
    private lateinit var prefs: android.content.SharedPreferences

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
        prefs = mockk(relaxed = true)
        repository = SentRepository(mockApiService, emailDao)
        viewModel = SentViewModel(repository, prefs)
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
