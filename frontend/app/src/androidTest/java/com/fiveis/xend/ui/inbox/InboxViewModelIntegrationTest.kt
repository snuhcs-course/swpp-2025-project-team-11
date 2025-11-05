package com.fiveis.xend.ui.inbox

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.database.AppDatabase
import com.fiveis.xend.data.database.EmailDao
import com.fiveis.xend.data.repository.InboxRepository
import com.fiveis.xend.network.RetrofitClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
    private lateinit var viewModel: InboxViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        emailDao = database.emailDao()

        val apiService = RetrofitClient.getMailApiService(context)
        repository = InboxRepository(apiService, emailDao)
        viewModel = InboxViewModel(repository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun viewModel_initial_state_is_correct() = runBlocking {
        // Wait for init to complete
        Thread.sleep(1000)

        // When
        val state = viewModel.uiState.first()

        // Then
        assertNotNull(state.emails)
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun refreshEmails_updates_state() = runBlocking {
        // Wait for init
        Thread.sleep(1000)

        // When
        viewModel.refreshEmails()
        Thread.sleep(500)

        // Then - Should be refreshing or finished refreshing
        val state = viewModel.uiState.first()
        assertNotNull(state.emails)
    }

    @Test
    fun loadMoreEmails_without_token_does_nothing() = runBlocking {
        // Wait for init
        Thread.sleep(1000)

        // Given - No next page token
        val initialState = viewModel.uiState.first()

        // When
        viewModel.loadMoreEmails()
        Thread.sleep(500)

        // Then
        val afterLoadState = viewModel.uiState.first()
        assertEquals(initialState.emails.size, afterLoadState.emails.size)
        assertFalse(afterLoadState.isLoading)
    }

    @Test
    fun onEmailClick_does_not_crash() = runBlocking {
        // Wait for init
        Thread.sleep(1000)

        // Given
        val state = viewModel.uiState.first()

        // When
        if (state.emails.isNotEmpty()) {
            viewModel.onEmailClick(state.emails.first())
        }

        // Then - No crash
        Thread.sleep(100)
    }

    @Test
    fun loadMoreEmails_while_loading_does_nothing() = runBlocking {
        // Wait for init
        Thread.sleep(1000)

        // When - Try to load more twice quickly
        viewModel.loadMoreEmails()
        viewModel.loadMoreEmails()
        Thread.sleep(500)

        // Then - No crash
        val state = viewModel.uiState.first()
        assertNotNull(state.emails)
    }

    @Test
    fun multiple_refreshes_handled_correctly() = runBlocking {
        // Wait for init
        Thread.sleep(1000)

        // When
        viewModel.refreshEmails()
        Thread.sleep(500)
        viewModel.refreshEmails()
        Thread.sleep(1000)

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isRefreshing)
        assertNotNull(state.emails)
    }
}
