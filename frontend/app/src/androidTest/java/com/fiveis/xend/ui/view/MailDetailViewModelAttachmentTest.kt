package com.fiveis.xend.ui.view

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.database.AppDatabase
import com.fiveis.xend.data.database.EmailDao
import com.fiveis.xend.data.model.Attachment
import com.fiveis.xend.data.model.AttachmentAnalysisResponse
import com.fiveis.xend.data.model.EmailItem
import com.fiveis.xend.data.repository.InboxRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
class MailDetailViewModelAttachmentTest {

    private lateinit var database: AppDatabase
    private lateinit var emailDao: EmailDao
    private lateinit var context: Context
    private lateinit var mockRepository: InboxRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        emailDao = database.emailDao()
        mockRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun downloadAttachment_success() = runBlocking {
        val testEmail = createEmailWithAttachment("msg1")
        emailDao.insertEmail(testEmail)
        val attachment = testEmail.attachments.first()

        val responseBody = "file content".toByteArray().toResponseBody()
        coEvery {
            mockRepository.downloadAttachment(any(), any(), any(), any())
        } returns Response.success(responseBody)

        val viewModel = MailDetailViewModel(context, emailDao, mockRepository, "msg1", Dispatchers.IO)
        delay(500) // Wait for initial mail load

        viewModel.downloadAttachment(attachment)

        // Wait until download completes with retries
        var attempts = 0
        while (viewModel.uiState.value.isDownloadingAttachment && attempts < 20) {
            delay(200)
            attempts++
        }

        val state = viewModel.uiState.first()
        assertFalse(state.isDownloadingAttachment)
    }

    @Test
    fun analyzeAttachment_success() = runBlocking {
        val testEmail = createEmailWithAttachment("msg7")
        emailDao.insertEmail(testEmail)
        val attachment = testEmail.attachments.first()

        val analysisResponse = AttachmentAnalysisResponse(
            summary = "Safe file",
            insights = "No threats detected",
            mailGuide = "Safe to proceed"
        )
        coEvery {
            mockRepository.analyzeAttachment(any(), any(), any(), any())
        } returns Response.success(analysisResponse)

        val viewModel = MailDetailViewModel(context, emailDao, mockRepository, "msg7", Dispatchers.IO)
        delay(100)

        viewModel.analyzeAttachment(attachment)
        delay(500)

        val state = viewModel.uiState.first()
        assertTrue(state.showAnalysisPopup)
        assertFalse(state.isAnalyzingAttachment)
        assertNotNull(state.analysisResult)
    }

    @Test
    fun clearDownloadResult_clears_messages() = runBlocking {
        val testEmail = createEmailWithAttachment("msg6")
        emailDao.insertEmail(testEmail)
        val viewModel = MailDetailViewModel(context, emailDao, mockRepository, "msg6", Dispatchers.IO)
        delay(100)

        viewModel.clearDownloadResult()
        delay(100)

        val state = viewModel.uiState.first()
        assertNull(state.downloadSuccessMessage)
        assertNull(state.downloadErrorMessage)
    }

    @Test
    fun dismissAnalysisPopup_clears_state() = runBlocking {
        val testEmail = createEmailWithAttachment("msg11")
        emailDao.insertEmail(testEmail)
        val viewModel = MailDetailViewModel(context, emailDao, mockRepository, "msg11", Dispatchers.IO)
        delay(100)

        viewModel.dismissAnalysisPopup()
        delay(100)

        val state = viewModel.uiState.first()
        assertFalse(state.showAnalysisPopup)
        assertFalse(state.isAnalyzingAttachment)
    }

    @Test
    fun openAttachmentExternally_success() = runBlocking {
        val testEmail = createEmailWithAttachment("msg12")
        emailDao.insertEmail(testEmail)
        val attachment = testEmail.attachments.first()

        val responseBody = "file content".toByteArray().toResponseBody()
        coEvery {
            mockRepository.downloadAttachment(any(), any(), any(), any())
        } returns Response.success(responseBody)

        val viewModel = MailDetailViewModel(context, emailDao, mockRepository, "msg12", Dispatchers.IO)
        delay(100)

        viewModel.openAttachmentExternally(attachment)
        delay(500)

        val state = viewModel.uiState.first()
        assertFalse(state.isExternalOpenLoading)
    }

    @Test
    fun consumeExternalOpenContent_clears_content() = runBlocking {
        val testEmail = createEmailWithAttachment("msg16")
        emailDao.insertEmail(testEmail)
        val viewModel = MailDetailViewModel(context, emailDao, mockRepository, "msg16", Dispatchers.IO)
        delay(100)

        viewModel.consumeExternalOpenContent()
        delay(100)

        val state = viewModel.uiState.first()
        assertNull(state.externalOpenContent)
    }

    @Test
    fun clearExternalOpenError_clears_error() = runBlocking {
        val testEmail = createEmailWithAttachment("msg17")
        emailDao.insertEmail(testEmail)
        val viewModel = MailDetailViewModel(context, emailDao, mockRepository, "msg17", Dispatchers.IO)
        delay(100)

        viewModel.clearExternalOpenError()
        delay(100)

        val state = viewModel.uiState.first()
        assertNull(state.externalOpenErrorMessage)
    }

    @Test
    fun previewAttachment_text_success() = runBlocking {
        val testEmail = createEmailWithAttachment("msg18", mimeType = "text/plain")
        emailDao.insertEmail(testEmail)
        val attachment = testEmail.attachments.first()

        val responseBody = "Text content here".toByteArray().toResponseBody()
        coEvery {
            mockRepository.downloadAttachment(any(), any(), any(), any())
        } returns Response.success(responseBody)

        val viewModel = MailDetailViewModel(context, emailDao, mockRepository, "msg18", Dispatchers.IO)
        delay(500) // Wait for mail load

        viewModel.previewAttachment(attachment)
        delay(500)

        val state = viewModel.uiState.value
        assertTrue(
            "Preview dialog not shown. Error: ${state.previewErrorMessage}, IsLoading: ${state.isPreviewLoading}",
            state.showPreviewDialog
        )
    }

    @Test
    fun dismissPreviewDialog_clears_state() = runBlocking {
        val testEmail = createEmailWithAttachment("msg24")
        emailDao.insertEmail(testEmail)
        val viewModel = MailDetailViewModel(context, emailDao, mockRepository, "msg24", Dispatchers.IO)
        delay(100)

        viewModel.dismissPreviewDialog()
        delay(100)

        val state = viewModel.uiState.first()
        assertFalse(state.showPreviewDialog)
    }

    private fun createEmailWithAttachment(
        id: String,
        filename: String = "test.txt",
        mimeType: String = "text/plain"
    ) = EmailItem(
        id = id,
        threadId = "thread_$id",
        subject = "Subject with attachment",
        fromEmail = "sender@example.com",
        snippet = "Has attachment",
        date = "2025-01-01T00:00:00Z",
        dateRaw = "Wed, 1 Jan 2025 00:00:00 +0000",
        isUnread = false,
        labelIds = listOf("INBOX"),
        cachedAt = System.currentTimeMillis(),
        attachments = listOf(
            Attachment(
                attachmentId = "att_$id",
                filename = filename,
                mimeType = mimeType,
                size = 1024
            )
        )
    )
}
