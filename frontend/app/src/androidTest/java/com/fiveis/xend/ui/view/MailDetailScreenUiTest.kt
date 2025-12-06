package com.fiveis.xend.ui.view

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.Attachment
import com.fiveis.xend.data.model.AttachmentAnalysisResponse
import com.fiveis.xend.data.model.EmailItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MailDetailScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mailDetailScreen_loading_state_displays_progress_indicator() {
        composeTestRule.setContent {
            MailDetailScreen(
                uiState = MailDetailUiState(isLoading = true),
                onBack = {}
            )
        }

        composeTestRule.onNode(hasContentDescription("Loading") or hasTestTag("CircularProgressIndicator") or isLoading())
            .assertExists()
    }

    @Test
    fun mailDetailScreen_error_state_displays_error_message() {
        composeTestRule.setContent {
            MailDetailScreen(
                uiState = MailDetailUiState(error = "Network error"),
                onBack = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("오류: Network error").assertExists()
    }

    @Test
    fun mailDetailScreen_success_state_displays_mail_content() {
        val mail = EmailItem(
            id = "1", threadId = "t1", subject = "Test Subject", fromEmail = "sender@test.com",
            date = "2024.01.01", dateRaw = "2024-01-01", snippet = "snippet", isUnread = false,
            labelIds = listOf("INBOX"), body = "Test body content",
            cachedAt = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            MailDetailScreen(
                uiState = MailDetailUiState(mail = mail),
                onBack = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Test Subject").assertExists()
    }

    @Test
    fun mailDetailScreen_displays_top_bar_title() {
        val mail = EmailItem(
            id = "1", threadId = "t1", subject = "S", fromEmail = "f@e.com",
            date = "d", dateRaw = "d", snippet = "s", isUnread = false,
            labelIds = listOf("INBOX"), body = "b", cachedAt = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            MailDetailScreen(
                uiState = MailDetailUiState(mail = mail),
                onBack = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("메일 상세").assertExists()
    }

    @Test
    fun mailDetailScreen_back_button_triggers_callback() {
        var backClicked = false
        val mail = EmailItem(
            id = "1", threadId = "t1", subject = "S", fromEmail = "f@e.com",
            date = "d", dateRaw = "d", snippet = "s", isUnread = false,
            labelIds = listOf("INBOX"), body = "b", cachedAt = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            MailDetailScreen(
                uiState = MailDetailUiState(mail = mail),
                onBack = { backClicked = true }
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("뒤로가기").performClick()
        composeTestRule.waitForIdle()
        assert(backClicked)
    }

    @Test
    fun mailDetailScreen_reply_button_triggers_callback() {
        var replyClicked = false
        val mail = EmailItem(
            id = "1", threadId = "t1", subject = "S", fromEmail = "f@e.com",
            date = "d", dateRaw = "d", snippet = "s", isUnread = false,
            labelIds = listOf("INBOX"), body = "b", cachedAt = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            MailDetailScreen(
                uiState = MailDetailUiState(mail = mail),
                onBack = {},
                onReply = { replyClicked = true }
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("답장하기").assertExists()
        composeTestRule.onNodeWithText("답장하기").performClick()
        composeTestRule.waitForIdle()
        assert(replyClicked)
    }

    @Test
    fun mailDetailScreen_displays_sender_information() {
        val mail = EmailItem(
            id = "1", threadId = "t1", subject = "S", fromEmail = "John Doe <john@test.com>",
            date = "2024.12.19", dateRaw = "2024-12-19", snippet = "s", isUnread = false,
            labelIds = listOf("INBOX"), body = "b", cachedAt = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            MailDetailScreen(
                uiState = MailDetailUiState(mail = mail),
                onBack = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("John Doe").assertExists()
        composeTestRule.onNodeWithText("<john@test.com>").assertExists()
        composeTestRule.onNodeWithText("2024.12.19").assertExists()
    }

    @Test
    fun mailDetailScreen_displays_attachments() {
        val attachments = listOf(
            Attachment("1", "file1.pdf", "pdf", 1000),
            Attachment("2", "file2.txt", "text", 2000)
        )
        val mail = EmailItem(
            id = "1", threadId = "t1", subject = "S", fromEmail = "f@e.com",
            date = "d", dateRaw = "d", snippet = "s", isUnread = false,
            labelIds = listOf("INBOX"), body = "b", attachments = attachments,
            cachedAt = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            MailDetailScreen(
                uiState = MailDetailUiState(mail = mail),
                onBack = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("첨부파일 (2개)").assertExists()
        composeTestRule.onNodeWithText("file1.pdf").assertExists()
        composeTestRule.onNodeWithText("file2.txt").assertExists()
    }

    @Test
    fun mailDetailScreen_displays_analyzing_dialog() {
        val mail = EmailItem(
            id = "1", threadId = "t1", subject = "S", fromEmail = "f@e.com",
            date = "d", dateRaw = "d", snippet = "s", isUnread = false,
            labelIds = listOf("INBOX"), body = "b", cachedAt = System.currentTimeMillis()
        )
        val attachment = Attachment("1", "test.pdf", "pdf", 1000)

        composeTestRule.setContent {
            MailDetailScreen(
                uiState = MailDetailUiState(
                    mail = mail,
                    showAnalysisPopup = true,
                    isAnalyzingAttachment = true,
                    analysisTarget = attachment
                ),
                onBack = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("AI가 파일을 분석 중입니다...").assertExists()
    }

    @Test
    fun mailDetailScreen_displays_analysis_result() {
        val mail = EmailItem(
            id = "1", threadId = "t1", subject = "S", fromEmail = "f@e.com",
            date = "d", dateRaw = "d", snippet = "s", isUnread = false,
            labelIds = listOf("INBOX"), body = "b", cachedAt = System.currentTimeMillis()
        )
        val attachment = Attachment("1", "test.pdf", "pdf", 1000)
        val result = AttachmentAnalysisResponse(
            summary = "Test summary",
            insights = "Test insights",
            mailGuide = "Test guide"
        )

        composeTestRule.setContent {
            MailDetailScreen(
                uiState = MailDetailUiState(
                    mail = mail,
                    showAnalysisPopup = true,
                    isAnalyzingAttachment = false,
                    analysisTarget = attachment,
                    analysisResult = result
                ),
                onBack = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("파일 분석 결과").assertExists()
        composeTestRule.onNodeWithText("주요 내용 요약").assertExists()
        composeTestRule.onNodeWithText("핵심 시사점").assertExists()
        composeTestRule.onNodeWithText("답장 작성 가이드").assertExists()
    }

    @Test
    fun mailDetailScreen_displays_downloading_dialog() {
        val mail = EmailItem(
            id = "1", threadId = "t1", subject = "S", fromEmail = "f@e.com",
            date = "d", dateRaw = "d", snippet = "s", isUnread = false,
            labelIds = listOf("INBOX"), body = "b", cachedAt = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            MailDetailScreen(
                uiState = MailDetailUiState(mail = mail, isDownloadingAttachment = true),
                onBack = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("파일 저장 중입니다...").assertExists()
    }

    @Test
    fun mailDetailScreen_displays_external_open_loading() {
        val mail = EmailItem(
            id = "1", threadId = "t1", subject = "S", fromEmail = "f@e.com",
            date = "d", dateRaw = "d", snippet = "s", isUnread = false,
            labelIds = listOf("INBOX"), body = "b", cachedAt = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            MailDetailScreen(
                uiState = MailDetailUiState(mail = mail, isExternalOpenLoading = true),
                onBack = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("파일을 준비하는 중입니다...").assertExists()
    }

    @Test
    fun mailDetailScreen_attachment_click_shows_download_dialog() {
        val attachment = Attachment("1", "document.pdf", "pdf", 5000)
        val mail = EmailItem(
            id = "1", threadId = "t1", subject = "S", fromEmail = "f@e.com",
            date = "d", dateRaw = "d", snippet = "s", isUnread = false,
            labelIds = listOf("INBOX"), body = "b", attachments = listOf(attachment),
            cachedAt = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            MailDetailScreen(
                uiState = MailDetailUiState(mail = mail),
                onBack = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("document.pdf").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("첨부파일 다운로드").assertExists()
        composeTestRule.onNodeWithText("'document.pdf' 파일을 저장할까요?").assertExists()
    }

    @Test
    fun mailDetailScreen_displays_preview_loading() {
        val mail = EmailItem(
            id = "1", threadId = "t1", subject = "S", fromEmail = "f@e.com",
            date = "d", dateRaw = "d", snippet = "s", isUnread = false,
            labelIds = listOf("INBOX"), body = "b", cachedAt = System.currentTimeMillis()
        )
        val attachment = Attachment("1", "test.txt", "text", 100)

        composeTestRule.setContent {
            MailDetailScreen(
                uiState = MailDetailUiState(
                    mail = mail,
                    showPreviewDialog = true,
                    isPreviewLoading = true,
                    previewTarget = attachment
                ),
                onBack = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("파일을 불러오는 중입니다...").assertExists()
    }

    @Test
    fun mailDetailScreen_displays_preview_error() {
        val mail = EmailItem(
            id = "1", threadId = "t1", subject = "S", fromEmail = "f@e.com",
            date = "d", dateRaw = "d", snippet = "s", isUnread = false,
            labelIds = listOf("INBOX"), body = "b", cachedAt = System.currentTimeMillis()
        )
        val attachment = Attachment("1", "test.txt", "text", 100)

        composeTestRule.setContent {
            MailDetailScreen(
                uiState = MailDetailUiState(
                    mail = mail,
                    showPreviewDialog = true,
                    isPreviewLoading = false,
                    previewTarget = attachment,
                    previewErrorMessage = "Failed to load"
                ),
                onBack = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Failed to load").assertExists()
        composeTestRule.onNodeWithText("다시 시도").assertExists()
    }

    @Test
    fun mailDetailScreen_sent_mail_displays_recipient_info() {
        val mail = EmailItem(
            id = "1", threadId = "t1", subject = "S",
            fromEmail = "me@test.com",
            toEmail = "John Doe <john@test.com>",
            date = "2024.12.19", dateRaw = "2024-12-19", snippet = "s",
            isUnread = false, labelIds = listOf("SENT"), body = "b",
            cachedAt = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            MailDetailScreen(
                uiState = MailDetailUiState(mail = mail),
                onBack = {}
            )
        }

        composeTestRule.waitForIdle()
        // Sent mail should display "To. name" format
        composeTestRule.onNodeWithText("To. John Doe", substring = true).assertExists()
    }

    @Test
    fun mailDetailScreen_ai_analysis_badge_visible() {
        val attachment = Attachment("1", "report.pdf", "pdf", 10000)
        val mail = EmailItem(
            id = "1", threadId = "t1", subject = "S", fromEmail = "f@e.com",
            date = "d", dateRaw = "d", snippet = "s", isUnread = false,
            labelIds = listOf("INBOX"), body = "b", attachments = listOf(attachment),
            cachedAt = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            MailDetailScreen(
                uiState = MailDetailUiState(mail = mail),
                onBack = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("AI 분석").assertExists()
    }

    @Test
    fun mailDetailScreen_preview_button_visible() {
        val attachment = Attachment("1", "image.png", "image", 5000)
        val mail = EmailItem(
            id = "1", threadId = "t1", subject = "S", fromEmail = "f@e.com",
            date = "d", dateRaw = "d", snippet = "s", isUnread = false,
            labelIds = listOf("INBOX"), body = "b", attachments = listOf(attachment),
            cachedAt = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            MailDetailScreen(
                uiState = MailDetailUiState(mail = mail),
                onBack = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("보기").assertExists()
    }
}

private fun isLoading() = SemanticsMatcher("is loading") {
    androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo in it.config
}
