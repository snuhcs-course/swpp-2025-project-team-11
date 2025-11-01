package com.fiveis.xend.integration

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.database.AppDatabase
import com.fiveis.xend.data.database.EmailDao
import com.fiveis.xend.data.model.EmailItem
import com.fiveis.xend.data.model.MailListResponse
import com.fiveis.xend.data.repository.InboxRepository
import com.fiveis.xend.network.MailApiService
import com.fiveis.xend.ui.inbox.InboxViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class InboxViewModelRepositoryIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var emailDao: EmailDao
    private lateinit var mailApiService: MailApiService
    private lateinit var repository: InboxRepository
    private lateinit var viewModel: InboxViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()

        emailDao = database.emailDao()
        mailApiService = mockk()

        repository = InboxRepository(mailApiService, emailDao)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun viewModel_loads_emails_from_repository_and_displays_them() = runTest {
        val mockEmails = listOf(
            createMockEmailItem("1"),
            createMockEmailItem("2")
        )

        emailDao.insertEmails(mockEmails)

        val mockResponse = Response.success(
            MailListResponse(
                messages = emptyList(),
                nextPageToken = null,
                resultSizeEstimate = 0
            )
        )
        coEvery { mailApiService.getEmails(any(), any(), any()) } returns mockResponse

        viewModel = InboxViewModel(repository)
        advanceUntilIdle()

        val emails = viewModel.uiState.value.emails
        assertEquals(2, emails.size)
        assertEquals("1", emails[0].id)
        assertEquals("2", emails[1].id)
    }

    @Test
    fun viewModel_refresh_fetches_new_emails_and_saves_to_database() = runTest {
        val newEmails = listOf(
            createMockEmailItem("3"),
            createMockEmailItem("4")
        )

        val mockResponse = Response.success(
            MailListResponse(
                messages = newEmails,
                nextPageToken = "token123",
                resultSizeEstimate = 2
            )
        )
        coEvery { mailApiService.getEmails(any(), any(), any()) } returns mockResponse

        viewModel = InboxViewModel(repository)
        advanceUntilIdle()

        val dbEmails = emailDao.getAllEmails().first()
        assertEquals(2, dbEmails.size)
        assertEquals("3", dbEmails[0].id)
        assertEquals("4", dbEmails[1].id)
    }

    @Test
    fun viewModel_loadMore_appends_emails_to_existing_list() = runTest {
        val firstResponse = Response.success(
            MailListResponse(
                messages = listOf(createMockEmailItem("1")),
                nextPageToken = "token123",
                resultSizeEstimate = 1
            )
        )
        val secondResponse = Response.success(
            MailListResponse(
                messages = listOf(createMockEmailItem("2")),
                nextPageToken = null,
                resultSizeEstimate = 1
            )
        )

        coEvery { mailApiService.getEmails("INBOX", 20, null) } returns firstResponse
        coEvery { mailApiService.getEmails("INBOX", 20, "token123") } returns secondResponse

        viewModel = InboxViewModel(repository)
        advanceUntilIdle()

        viewModel.loadMoreEmails()
        advanceUntilIdle()

        val dbEmails = emailDao.getAllEmails().first()
        assertEquals(2, dbEmails.size)
        assertTrue(dbEmails.any { it.id == "1" })
        assertTrue(dbEmails.any { it.id == "2" })
    }

    @Test
    fun repository_caches_emails_and_viewModel_displays_cached_data() = runTest {
        val cachedEmails = listOf(
            createMockEmailItem("1"),
            createMockEmailItem("2")
        )

        emailDao.insertEmails(cachedEmails)

        val mockResponse = Response.success(
            MailListResponse(
                messages = emptyList(),
                nextPageToken = null,
                resultSizeEstimate = 0
            )
        )
        coEvery { mailApiService.getEmails(any(), any(), any()) } returns mockResponse

        viewModel = InboxViewModel(repository)
        advanceUntilIdle()

        val uiEmails = viewModel.uiState.value.emails
        assertEquals(2, uiEmails.size)
        assertEquals("1", uiEmails[0].id)
        assertEquals("2", uiEmails[1].id)
    }

    @Test
    fun viewModel_handles_empty_cache_and_successful_api_response() = runTest {
        val newEmails = listOf(createMockEmailItem("1"))

        val mockResponse = Response.success(
            MailListResponse(
                messages = newEmails,
                nextPageToken = null,
                resultSizeEstimate = 1
            )
        )
        coEvery { mailApiService.getEmails(any(), any(), any()) } returns mockResponse

        viewModel = InboxViewModel(repository)
        advanceUntilIdle()

        val uiEmails = viewModel.uiState.value.emails
        assertEquals(1, uiEmails.size)
        assertEquals("1", uiEmails[0].id)

        val dbEmails = emailDao.getAllEmails().first()
        assertEquals(1, dbEmails.size)
        assertEquals("1", dbEmails[0].id)
    }

    @Test
    fun viewModel_stops_refreshing_after_completing() = runTest {
        val mockResponse = Response.success(
            MailListResponse(
                messages = emptyList(),
                nextPageToken = null,
                resultSizeEstimate = 0
            )
        )
        coEvery { mailApiService.getEmails(any(), any(), any()) } returns mockResponse

        viewModel = InboxViewModel(repository)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun repository_updates_read_status_in_database() = runTest {
        val email = createMockEmailItem("1", isUnread = true)
        emailDao.insertEmail(email)

        repository.updateReadStatus("1", false)

        val updatedEmail = emailDao.getEmailById("1")
        assertEquals(false, updatedEmail?.isUnread)
    }

    @Test
    fun database_orders_emails_by_date_descending() = runTest {
        val emails = listOf(
            createMockEmailItem("1", date = "2025-01-01T10:00:00Z"),
            createMockEmailItem("2", date = "2025-01-03T10:00:00Z"),
            createMockEmailItem("3", date = "2025-01-02T10:00:00Z")
        )

        emailDao.insertEmails(emails)

        val orderedEmails = emailDao.getAllEmails().first()

        assertEquals("2", orderedEmails[0].id)
        assertEquals("3", orderedEmails[1].id)
        assertEquals("1", orderedEmails[2].id)
    }

    private fun createMockEmailItem(id: String, date: String = "2025-01-01T00:00:00Z", isUnread: Boolean = true) =
        EmailItem(
            id = id,
            threadId = "thread_$id",
            subject = "Subject $id",
            fromEmail = "sender$id@example.com",
            snippet = "Snippet $id",
            date = date,
            dateRaw = "Wed, 1 Jan 2025 00:00:00 +0000",
            isUnread = isUnread,
            labelIds = listOf("INBOX"),
            cachedAt = System.currentTimeMillis()
        )
}
