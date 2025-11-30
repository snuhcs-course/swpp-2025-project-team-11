package com.fiveis.xend.ui.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.EmailItem
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MailDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mailDetailScreen_displays_loading_state() {
        // Given
        val uiState = MailDetailUiState(isLoading = true)

        // When
        composeTestRule.setContent {
            MailDetailScreen(uiState = uiState, onBack = {}, onReply = {})
        }

        // Then - CircularProgressIndicator should be displayed
        Thread.sleep(200)
    }

    @Test
    fun mailDetailScreen_displays_error_state() {
        // Given
        val uiState = MailDetailUiState(error = "메일을 불러올 수 없습니다")

        // When
        composeTestRule.setContent {
            MailDetailScreen(uiState = uiState, onBack = {}, onReply = {})
        }

        // Then
        composeTestRule.onNodeWithText("오류: 메일을 불러올 수 없습니다").assertIsDisplayed()
    }

    @Test
    fun mailDetailScreen_displays_mail_content() {
        // Given
        val sampleMail = EmailItem(
            id = "1",
            threadId = "thread_1",
            subject = "Test Subject",
            fromEmail = "sender@test.com",
            snippet = "Test snippet",
            date = "2024.12.19",
            dateRaw = "2024-12-19T10:00:00Z",
            isUnread = false,
            labelIds = listOf("INBOX"),
            body = "Test email body"
        )
        val uiState = MailDetailUiState(mail = sampleMail)

        // When
        composeTestRule.setContent {
            MailDetailScreen(uiState = uiState, onBack = {}, onReply = {})
        }

        // Then
        composeTestRule.onNodeWithText("메일 상세").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Subject").assertIsDisplayed()
        composeTestRule.onNodeWithText("답장하기").assertIsDisplayed()
    }

    @Test
    fun mailDetailScreen_displays_sender_with_name_and_email() {
        // Given
        val sampleMail = EmailItem(
            id = "1",
            threadId = "thread_1",
            subject = "Test Subject",
            fromEmail = "김철수 <chulsoo@test.com>",
            snippet = "Test snippet",
            date = "2024.12.19",
            dateRaw = "2024-12-19T10:00:00Z",
            isUnread = false,
            labelIds = listOf("INBOX"),
            body = "Test body"
        )
        val uiState = MailDetailUiState(mail = sampleMail)

        // When
        composeTestRule.setContent {
            MailDetailScreen(uiState = uiState, onBack = {}, onReply = {})
        }

        // Then
        composeTestRule.onNodeWithText("김철수").assertIsDisplayed()
        composeTestRule.onNodeWithText("<chulsoo@test.com>").assertIsDisplayed()
    }

    @Test
    fun mailDetailScreen_backButton_triggers_callback() {
        // Given
        var backClicked = false
        val sampleMail = EmailItem(
            id = "1",
            threadId = "thread_1",
            subject = "Test",
            fromEmail = "test@test.com",
            snippet = "snippet",
            date = "2024.12.19",
            dateRaw = "2024-12-19T10:00:00Z",
            isUnread = false,
            labelIds = listOf("INBOX"),
            body = "body"
        )
        val uiState = MailDetailUiState(mail = sampleMail)

        // When
        composeTestRule.setContent {
            MailDetailScreen(
                uiState = uiState,
                onBack = { backClicked = true },
                onReply = {}
            )
        }

        // Then
        composeTestRule.onNodeWithContentDescription("뒤로가기").performClick()
        assertTrue("Back callback should be called", backClicked)
    }

    @Test
    fun mailDetailScreen_replyButton_triggers_callback() {
        // Given
        var replyClicked = false
        val sampleMail = EmailItem(
            id = "1",
            threadId = "thread_1",
            subject = "Test",
            fromEmail = "test@test.com",
            snippet = "snippet",
            date = "2024.12.19",
            dateRaw = "2024-12-19T10:00:00Z",
            isUnread = false,
            labelIds = listOf("INBOX"),
            body = "body"
        )
        val uiState = MailDetailUiState(mail = sampleMail)

        // When
        composeTestRule.setContent {
            MailDetailScreen(
                uiState = uiState,
                onBack = {},
                onReply = { replyClicked = true }
            )
        }

        // Then
        composeTestRule.onNodeWithText("답장하기").performClick()
        assertTrue("Reply callback should be called", replyClicked)
    }

    @Test
    fun mailDetailScreen_displays_empty_body_message() {
        // Given
        val sampleMail = EmailItem(
            id = "1",
            threadId = "thread_1",
            subject = "Test",
            fromEmail = "test@test.com",
            snippet = "snippet",
            date = "2024.12.19",
            dateRaw = "2024-12-19T10:00:00Z",
            isUnread = false,
            labelIds = listOf("INBOX"),
            body = ""
        )
        val uiState = MailDetailUiState(mail = sampleMail)

        // When
        composeTestRule.setContent {
            MailDetailScreen(uiState = uiState, onBack = {}, onReply = {})
        }

        // Then
        composeTestRule.onNodeWithText("메일 본문이 없습니다.").assertIsDisplayed()
    }

    @Test
    fun mailDetailScreen_renders_html_body() {
        // Given
        val sampleMail = EmailItem(
            id = "1",
            threadId = "thread_1",
            subject = "HTML Email",
            fromEmail = "test@test.com",
            snippet = "snippet",
            date = "2024.12.19",
            dateRaw = "2024-12-19T10:00:00Z",
            isUnread = false,
            labelIds = listOf("INBOX"),
            body = "<p>This is <strong>HTML</strong> content</p>"
        )
        val uiState = MailDetailUiState(mail = sampleMail)

        // When
        composeTestRule.setContent {
            MailDetailScreen(uiState = uiState, onBack = {}, onReply = {})
        }

        // Then - Should not crash
        Thread.sleep(500)
        composeTestRule.onNodeWithText("HTML Email").assertIsDisplayed()
    }

    @Test
    fun mailDetailScreen_displays_plain_email_without_name() {
        // Given
        val sampleMail = EmailItem(
            id = "1",
            threadId = "thread_1",
            subject = "Test",
            fromEmail = "simple@test.com",
            snippet = "snippet",
            date = "2024.12.19",
            dateRaw = "2024-12-19T10:00:00Z",
            isUnread = false,
            labelIds = listOf("INBOX"),
            body = "body"
        )
        val uiState = MailDetailUiState(mail = sampleMail)

        // When
        composeTestRule.setContent {
            MailDetailScreen(uiState = uiState, onBack = {}, onReply = {})
        }

        // Wait for UI to be ready
        composeTestRule.waitForIdle()
        Thread.sleep(200)

        // Then
        composeTestRule.onNodeWithText("simple@test.com").assertIsDisplayed()
    }

    @Test
    fun mailDetailScreen_displays_long_body() {
        // Given
        val longBody = "Line 1\n".repeat(100)
        val sampleMail = EmailItem(
            id = "1",
            threadId = "thread_1",
            subject = "Long Email",
            fromEmail = "test@test.com",
            snippet = "snippet",
            date = "2024.12.19",
            dateRaw = "2024-12-19T10:00:00Z",
            isUnread = false,
            labelIds = listOf("INBOX"),
            body = longBody
        )
        val uiState = MailDetailUiState(mail = sampleMail)

        // When
        composeTestRule.setContent {
            MailDetailScreen(uiState = uiState, onBack = {}, onReply = {})
        }

        // Then - Should scroll and display without crash
        Thread.sleep(500)
        composeTestRule.onNodeWithText("Long Email").assertIsDisplayed()
    }

    @Test
    fun mailDetailScreen_attachment_download_callback() {
        // Given
        var downloadClicked = false
        val attachment = com.fiveis.xend.data.model.Attachment(
            attachmentId = "att1",
            filename = "test.pdf",
            mimeType = "application/pdf",
            size = 1024
        )
        val sampleMail = EmailItem(
            id = "1",
            threadId = "thread_1",
            subject = "Attachment Mail",
            fromEmail = "test@test.com",
            snippet = "snippet",
            date = "2024.12.19",
            dateRaw = "2024-12-19T10:00:00Z",
            isUnread = false,
            labelIds = listOf("INBOX"),
            cachedAt = System.currentTimeMillis(),
            body = "body",
            attachments = listOf(attachment)
        )
        val uiState = MailDetailUiState(mail = sampleMail)

        // When
        composeTestRule.setContent {
            MailDetailScreen(
                uiState = uiState,
                onBack = {},
                onDownloadAttachment = { downloadClicked = true }
            )
        }

        // Then
        composeTestRule.onNodeWithText("test.pdf").performClick()
        composeTestRule.onNodeWithText("파일 저장").performClick()
        assertTrue("Download callback should be called", downloadClicked)
    }

    @Test
    fun mailDetailScreen_attachment_analyze_callback() {
        // Given
        var analyzeClicked = false
        val attachment = com.fiveis.xend.data.model.Attachment(
            attachmentId = "att1",
            filename = "test.pdf",
            mimeType = "application/pdf",
            size = 1024
        )
        val sampleMail = EmailItem(
            id = "1",
            threadId = "thread_1",
            subject = "Attachment Mail",
            fromEmail = "test@test.com",
            snippet = "snippet",
            date = "2024.12.19",
            dateRaw = "2024-12-19T10:00:00Z",
            isUnread = false,
            labelIds = listOf("INBOX"),
            cachedAt = System.currentTimeMillis(),
            body = "body",
            attachments = listOf(attachment)
        )
        val uiState = MailDetailUiState(mail = sampleMail)

        // When
        composeTestRule.setContent {
            MailDetailScreen(
                uiState = uiState,
                onBack = {},
                onAnalyzeAttachment = { analyzeClicked = true }
            )
        }

        // Then
        composeTestRule.onNodeWithText("AI 분석").performClick()
        assertTrue("Analyze callback should be called", analyzeClicked)
    }

    @Test
    fun mailDetailScreen_displays_analysis_popup() {
        // Given
        val attachment = com.fiveis.xend.data.model.Attachment(
            attachmentId = "att1",
            filename = "test.pdf",
            mimeType = "application/pdf",
            size = 1024
        )
        val uiState = MailDetailUiState(
            showAnalysisPopup = true,
            analysisTarget = attachment,
            isAnalyzingAttachment = true
        )

        // When
        composeTestRule.setContent {
            MailDetailScreen(uiState = uiState, onBack = {})
        }

        // Then
        composeTestRule.onNodeWithText("AI가 파일을 분석 중입니다...").assertIsDisplayed()
    }

    @Test
    fun mailDetailScreen_attachment_preview_callback() {
        // Given
        var previewClicked = false
        // Use a text file which supports internal preview (not image/png)
        val attachment = com.fiveis.xend.data.model.Attachment(
            attachmentId = "att1",
            filename = "test.txt",
            mimeType = "text/plain",
            size = 1024
        )
        val sampleMail = EmailItem(
            id = "1",
            threadId = "thread_1",
            subject = "Attachment Mail",
            fromEmail = "test@test.com",
            snippet = "snippet",
            date = "2024.12.19",
            dateRaw = "2024-12-19T10:00:00Z",
            isUnread = false,
            labelIds = listOf("INBOX"),
            cachedAt = System.currentTimeMillis(),
            body = "body",
            attachments = listOf(attachment)
        )
        val uiState = MailDetailUiState(mail = sampleMail)

        // When
        composeTestRule.setContent {
            MailDetailScreen(
                uiState = uiState,
                onBack = {},
                onPreviewAttachment = { previewClicked = true }
            )
        }

        // Wait for UI to render
        composeTestRule.waitForIdle()
        Thread.sleep(200)

        // Then - Click the preview button
        composeTestRule.onNodeWithText("보기").performClick()
        composeTestRule.waitForIdle()

        // Callback should be called
        assertTrue("Preview callback should be called", previewClicked)
    }

    @Test
    fun mailDetailScreen_attachment_open_external_callback() {
        // Given
        var openExternalClicked = false
        val attachment = com.fiveis.xend.data.model.Attachment(
            attachmentId = "att1",
            filename = "test.zip",
            mimeType = "application/zip",
            size = 1024
        )
        val sampleMail = EmailItem(
            id = "1",
            threadId = "thread_1",
            subject = "Attachment Mail",
            fromEmail = "test@test.com",
            snippet = "snippet",
            date = "2024.12.19",
            dateRaw = "2024-12-19T10:00:00Z",
            isUnread = false,
            labelIds = listOf("INBOX"),
            cachedAt = System.currentTimeMillis(),
            body = "body",
            attachments = listOf(attachment)
        )
        val uiState = MailDetailUiState(mail = sampleMail)

        // When
        composeTestRule.setContent {
            MailDetailScreen(
                uiState = uiState,
                onBack = {},
                onOpenAttachmentExternally = { openExternalClicked = true }
            )
        }

        // Then - Zip files don't support internal preview, so "보기" should trigger external open
        composeTestRule.onNodeWithText("보기").performClick()
        assertTrue("Open external callback should be called", openExternalClicked)
    }

    @Test
    fun mailDetailScreen_displays_preview_dialog() {
        // Given
        val attachment = com.fiveis.xend.data.model.Attachment(
            attachmentId = "att1",
            filename = "test.pdf",
            mimeType = "application/pdf",
            size = 1024
        )
        val uiState = MailDetailUiState(
            showPreviewDialog = true,
            previewTarget = attachment,
            isPreviewLoading = true
        )

        // When
        composeTestRule.setContent {
            MailDetailScreen(uiState = uiState, onBack = {})
        }

        // Then
        composeTestRule.onNodeWithText("파일을 불러오는 중입니다...").assertIsDisplayed()
    }

    @Test
    fun mailDetailScreen_displays_downloading_dialog() {
        // Given
        val uiState = MailDetailUiState(
            isDownloadingAttachment = true
        )

        // When
        composeTestRule.setContent {
            MailDetailScreen(uiState = uiState, onBack = {})
        }

        // Then
        composeTestRule.onNodeWithText("파일 저장 중입니다...").assertIsDisplayed()
    }

    @Test
    fun mailDetailScreen_displays_external_open_loading_dialog() {
        // Given
        val uiState = MailDetailUiState(
            isExternalOpenLoading = true
        )

        // When
        composeTestRule.setContent {
            MailDetailScreen(uiState = uiState, onBack = {})
        }

        // Then
        composeTestRule.onNodeWithText("파일을 준비하는 중입니다...").assertIsDisplayed()
    }

    @Test
    fun mailDetailScreen_displays_analysis_result() {
        // Given
        val attachment = com.fiveis.xend.data.model.Attachment(
            attachmentId = "att1",
            filename = "test.pdf",
            mimeType = "application/pdf",
            size = 1024
        )
        val analysisResponse = com.fiveis.xend.data.model.AttachmentAnalysisResponse(
            summary = "Summary content",
            insights = "Insights content",
            mailGuide = "Mail guide content"
        )
        val uiState = MailDetailUiState(
            showAnalysisPopup = true,
            analysisTarget = attachment,
            isAnalyzingAttachment = false,
            analysisResult = analysisResponse
        )

        // When
        composeTestRule.setContent {
            MailDetailScreen(uiState = uiState, onBack = {})
        }

        // Then
        composeTestRule.onNodeWithText("주요 내용 요약").assertIsDisplayed()
        composeTestRule.onNodeWithText("Summary content").assertIsDisplayed()
        composeTestRule.onNodeWithText("핵심 시사점").assertIsDisplayed()
        composeTestRule.onNodeWithText("Insights content").assertIsDisplayed()
        composeTestRule.onNodeWithText("답장 작성 가이드").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mail guide content").assertIsDisplayed()
    }

    @Test
    fun mailDetailScreen_displays_analysis_error() {
        // Given
        val attachment = com.fiveis.xend.data.model.Attachment(
            attachmentId = "att1",
            filename = "test.pdf",
            mimeType = "application/pdf",
            size = 1024
        )
        val uiState = MailDetailUiState(
            showAnalysisPopup = true,
            analysisTarget = attachment,
            isAnalyzingAttachment = false,
            analysisErrorMessage = "Analysis failed"
        )

        // When
        composeTestRule.setContent {
            MailDetailScreen(uiState = uiState, onBack = {})
        }

        // Then
        composeTestRule.onNodeWithText("Analysis failed").assertIsDisplayed()
    }

    @Test
    fun mailDetailScreen_displays_preview_text_content() {
        // Given
        val attachment = com.fiveis.xend.data.model.Attachment(
            attachmentId = "att1",
            filename = "test.txt",
            mimeType = "text/plain",
            size = 1024
        )
        val previewContent = AttachmentPreviewContent.Text("Preview text content")
        val uiState = MailDetailUiState(
            showPreviewDialog = true,
            previewTarget = attachment,
            isPreviewLoading = false,
            previewContent = previewContent
        )

        // When
        composeTestRule.setContent {
            MailDetailScreen(uiState = uiState, onBack = {})
        }

        // Then
        composeTestRule.onNodeWithText("Preview text content").assertIsDisplayed()
    }

    @Test
    fun mailDetailScreen_displays_preview_error() {
        // Given
        val attachment = com.fiveis.xend.data.model.Attachment(
            attachmentId = "att1",
            filename = "test.txt",
            mimeType = "text/plain",
            size = 1024
        )
        val uiState = MailDetailUiState(
            showPreviewDialog = true,
            previewTarget = attachment,
            isPreviewLoading = false,
            previewErrorMessage = "Preview failed"
        )

        // When
        composeTestRule.setContent {
            MailDetailScreen(uiState = uiState, onBack = {})
        }

        // Then
        composeTestRule.onNodeWithText("Preview failed").assertIsDisplayed()
        composeTestRule.onNodeWithText("다시 시도").assertIsDisplayed()
    }
}
