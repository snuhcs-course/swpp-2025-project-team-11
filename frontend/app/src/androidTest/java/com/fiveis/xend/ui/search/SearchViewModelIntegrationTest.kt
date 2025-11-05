package com.fiveis.xend.ui.search

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.database.AppDatabase
import com.fiveis.xend.data.database.EmailDao
import com.fiveis.xend.data.model.EmailItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchViewModelIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var emailDao: EmailDao
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        emailDao = database.emailDao()
        viewModel = SearchViewModel(emailDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun viewModel_initial_state_is_empty() = runBlocking {
        // When
        val state = viewModel.uiState.first()

        // Then
        assertEquals("", state.query)
        assertTrue(state.results.isEmpty())
    }

    @Test
    fun onQueryChange_updates_query_in_state() = runBlocking {
        // When
        viewModel.onQueryChange("test")
        Thread.sleep(100)

        // Then
        val state = viewModel.uiState.first()
        assertEquals("test", state.query)
    }

    @Test
    fun onQueryChange_with_empty_string_returns_empty_results() = runBlocking {
        // Given
        insertTestEmails()

        // When
        viewModel.onQueryChange("")
        Thread.sleep(500)

        // Then
        val state = viewModel.uiState.first()
        assertTrue(state.results.isEmpty())
    }

    @Test
    fun onQueryChange_with_matching_query_returns_results() = runBlocking {
        // Given
        insertTestEmails()

        // When
        viewModel.onQueryChange("Important")
        Thread.sleep(500)

        // Then
        val state = viewModel.uiState.first()
        assertTrue(state.results.isNotEmpty())
    }

    @Test
    fun onQueryChange_debounces_quickly_typed_queries() = runBlocking {
        // Given
        insertTestEmails()

        // When - Type quickly
        viewModel.onQueryChange("I")
        Thread.sleep(50)
        viewModel.onQueryChange("Im")
        Thread.sleep(50)
        viewModel.onQueryChange("Imp")
        Thread.sleep(500)

        // Then - Only last query should be processed
        val state = viewModel.uiState.first()
        assertEquals("Imp", state.query)
    }

    @Test
    fun onQueryChange_is_case_insensitive() = runBlocking {
        // Given
        insertTestEmails()

        // When
        viewModel.onQueryChange("important")
        Thread.sleep(500)

        // Then
        val state = viewModel.uiState.first()
        assertTrue(state.results.isNotEmpty())
    }

    private suspend fun insertTestEmails() {
        val emails = listOf(
            createMockEmailItem("1", subject = "Important Meeting"),
            createMockEmailItem("2", subject = "Weekly Report"),
            createMockEmailItem("3", subject = "Important Update")
        )
        emailDao.insertEmails(emails)
    }

    private fun createMockEmailItem(
        id: String,
        subject: String = "Subject $id"
    ) = EmailItem(
        id = id,
        threadId = "thread_$id",
        subject = subject,
        fromEmail = "sender$id@example.com",
        snippet = "Snippet $id",
        date = "2025-01-01T00:00:00Z",
        dateRaw = "Wed, 1 Jan 2025 00:00:00 +0000",
        isUnread = true,
        labelIds = listOf("INBOX"),
        cachedAt = System.currentTimeMillis()
    )
}
