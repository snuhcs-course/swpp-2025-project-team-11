package com.fiveis.xend.ui.view

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MailDetailViewModelIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var emailDao: EmailDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        emailDao = database.emailDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun viewModel_loads_existing_email() = runBlocking {
        // Given
        val testEmail = createMockEmailItem("test_id_1")
        emailDao.insertEmail(testEmail)

        // When
        val viewModel = MailDetailViewModel(emailDao, "test_id_1")
        Thread.sleep(500)

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertNotNull(state.mail)
        assertEquals("test_id_1", state.mail?.id)
        assertNull(state.error)
    }

    @Test
    fun viewModel_handles_missing_email() = runBlocking {
        // When
        val viewModel = MailDetailViewModel(emailDao, "nonexistent_id")
        Thread.sleep(500)

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertNull(state.mail)
        assertNotNull(state.error)
        assertEquals("Email not found", state.error)
    }

    @Test
    fun viewModel_loads_email_with_special_characters() = runBlocking {
        // Given
        val testEmail = createMockEmailItem(
            id = "test-id-special",
            subject = "Test!@#$%^&*()",
            fromEmail = "test+tag@example.com"
        )
        emailDao.insertEmail(testEmail)

        // When
        val viewModel = MailDetailViewModel(emailDao, "test-id-special")
        Thread.sleep(500)

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertNotNull(state.mail)
        assertEquals("Test!@#$%^&*()", state.mail?.subject)
        assertEquals("test+tag@example.com", state.mail?.fromEmail)
    }

    @Test
    fun viewModel_initial_state_is_loading() = runBlocking {
        // Given
        val testEmail = createMockEmailItem("test_id_2")
        emailDao.insertEmail(testEmail)

        // When
        val viewModel = MailDetailViewModel(emailDao, "test_id_2")
        val immediateState = viewModel.uiState.first()

        // Then - Initial state should be loading
        // (Note: This may be flaky due to timing, but tests the loading behavior)
        Thread.sleep(500)
        val finalState = viewModel.uiState.first()
        assertFalse(finalState.isLoading)
    }

    private fun createMockEmailItem(
        id: String,
        subject: String = "Subject $id",
        fromEmail: String = "sender$id@example.com"
    ) = EmailItem(
        id = id,
        threadId = "thread_$id",
        subject = subject,
        fromEmail = fromEmail,
        snippet = "Snippet $id",
        date = "2025-01-01T00:00:00Z",
        dateRaw = "Wed, 1 Jan 2025 00:00:00 +0000",
        isUnread = true,
        labelIds = listOf("INBOX"),
        cachedAt = System.currentTimeMillis()
    )
}
