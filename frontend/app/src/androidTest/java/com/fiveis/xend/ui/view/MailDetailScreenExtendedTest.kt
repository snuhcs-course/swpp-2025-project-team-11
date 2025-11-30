package com.fiveis.xend.ui.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.Attachment
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.EmailItem
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MailDetailScreenExtendedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val baseMail = EmailItem(
        id = "1",
        threadId = "thread_1",
        subject = "Test Subject",
        fromEmail = "Sender <sender@test.com>",
        toEmail = "Recipient <recipient@test.com>",
        snippet = "Snippet",
        date = "2024.12.25",
        dateRaw = "2024-12-25T10:00:00Z",
        isUnread = false,
        labelIds = listOf("INBOX"),
        body = "Body content"
    )

    @Test
    fun senderSection_displays_correctly_for_received_mail() {
        val uiState = MailDetailUiState(mail = baseMail.copy(labelIds = listOf("INBOX")))
        composeTestRule.setContent {
            MailDetailScreen(uiState = uiState, onBack = {})
        }
        // parseSenderEmail returns "Sender" as display name (not with email in parenthesis)
        composeTestRule.onNodeWithText("Sender").assertIsDisplayed()
        composeTestRule.onNodeWithText("<sender@test.com>").assertIsDisplayed()
    }

    @Test
    fun senderSection_displays_correctly_for_sent_mail() {
        val uiState = MailDetailUiState(mail = baseMail.copy(labelIds = listOf("SENT")))
        composeTestRule.setContent {
            MailDetailScreen(uiState = uiState, onBack = {})
        }
        // parseSenderEmail returns "Recipient" as display name, with "To. " prefix
        composeTestRule.onNodeWithText("To. Recipient").assertIsDisplayed()
        composeTestRule.onNodeWithText("<recipient@test.com>").assertIsDisplayed()
    }

    @Test
    fun senderSection_resolves_contact_name() {
        val contact = Contact(id = 1, name = "Saved Name", email = "sender@test.com", group = null)
        val contacts = mapOf("sender@test.com" to contact)
        val uiState = MailDetailUiState(mail = baseMail)
        
        composeTestRule.setContent {
            MailDetailScreen(uiState = uiState, knownContactsByEmail = contacts, onBack = {})
        }
        
        composeTestRule.onNodeWithText("Saved Name").assertIsDisplayed()
        // Should still show email
        composeTestRule.onNodeWithText("<sender@test.com>").assertIsDisplayed()
    }

    @Test
    fun senderSection_handles_email_only_format() {
        val mail = baseMail.copy(fromEmail = "only@test.com")
        val uiState = MailDetailUiState(mail = mail)
        
        composeTestRule.setContent {
            MailDetailScreen(uiState = uiState, onBack = {})
        }
        
        composeTestRule.onNodeWithText("only@test.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("<only@test.com>").assertIsDisplayed()
    }

    @Test
    fun attachmentSection_header_shows_count() {
        val attachments = listOf(
            Attachment("1", "file1.txt", "text/plain", 100),
            Attachment("2", "file2.txt", "text/plain", 100)
        )
        val mail = baseMail.copy(attachments = attachments)
        val uiState = MailDetailUiState(mail = mail)

        composeTestRule.setContent {
            MailDetailScreen(uiState = uiState, onBack = {})
        }

        composeTestRule.onNodeWithText("첨부파일 (2개)").assertIsDisplayed()
    }

    @Test
    fun attachmentSection_not_shown_when_empty() {
        val mail = baseMail.copy(attachments = emptyList())
        val uiState = MailDetailUiState(mail = mail)

        composeTestRule.setContent {
            MailDetailScreen(uiState = uiState, onBack = {})
        }

        composeTestRule.onNodeWithText("첨부파일").assertDoesNotExist()
    }

    @Test
    fun bodySection_markers_case1() {
        val body = "Reply\n\n-----Original Message-----\nFrom:..."
        val mail = baseMail.copy(body = body)
        val uiState = MailDetailUiState(mail = mail)

        composeTestRule.setContent {
            MailDetailScreen(uiState = uiState, onBack = {})
        }
        
        // Should use collapsible view, so "원본 메시지" header should appear
        composeTestRule.onNodeWithText("원본 메시지").assertIsDisplayed()
    }

    @Test
    fun bodySection_markers_case2() {
        val body = "Reply\n\n<br><br>From:..."
        val mail = baseMail.copy(body = body)
        val uiState = MailDetailUiState(mail = mail)

        composeTestRule.setContent {
            MailDetailScreen(uiState = uiState, onBack = {})
        }
        
        composeTestRule.onNodeWithText("원본 메시지").assertIsDisplayed()
    }

    @Test
    fun analysisPopup_shows_auto_reflection_message() {
        val attachment = Attachment("1", "f.pdf", "pdf", 100)
        val result = com.fiveis.xend.data.model.AttachmentAnalysisResponse(
            summary = "s", insights = "i", mailGuide = "Guide Text"
        )
        val uiState = MailDetailUiState(
            showAnalysisPopup = true,
            analysisTarget = attachment,
            analysisResult = result
        )

        composeTestRule.setContent {
            MailDetailScreen(uiState = uiState, onBack = {})
        }

        // The popup now shows an info message instead of a copy button
        composeTestRule.onNodeWithText("답장 작성 가이드는 답장 생성 시 자동으로 반영돼요.").assertIsDisplayed()
    }

    @Test
    fun analysisPopup_shows_info_message_even_when_guide_empty() {
        val attachment = Attachment("1", "f.pdf", "pdf", 100)
        val result = com.fiveis.xend.data.model.AttachmentAnalysisResponse(
            summary = "s", insights = "i", mailGuide = ""
        )
        val uiState = MailDetailUiState(
            showAnalysisPopup = true,
            analysisTarget = attachment,
            analysisResult = result
        )

        composeTestRule.setContent {
            MailDetailScreen(uiState = uiState, onBack = {})
        }

        // The info message is shown regardless of whether mailGuide is empty
        composeTestRule.onNodeWithText("답장 작성 가이드는 답장 생성 시 자동으로 반영돼요.").assertIsDisplayed()
    }
    
    @Test
    fun previewDialog_retry_callback() {
        var retryCalled = false
        val attachment = Attachment("1", "f.pdf", "pdf", 100)
        val uiState = MailDetailUiState(
            showPreviewDialog = true,
            previewTarget = attachment,
            previewErrorMessage = "Error"
        )

        composeTestRule.setContent {
            MailDetailScreen(
                uiState = uiState, 
                onBack = {},
                onPreviewAttachment = { retryCalled = true }
            )
        }

        composeTestRule.onNodeWithText("다시 시도").performClick()
        assertTrue("Retry callback should be called", retryCalled)
    }

    @Test
    fun previewDialog_dismiss_callback() {
        var dismissCalled = false
        val attachment = Attachment("1", "f.pdf", "pdf", 100)
        val uiState = MailDetailUiState(
            showPreviewDialog = true,
            previewTarget = attachment,
            isPreviewLoading = true
        )

        composeTestRule.setContent {
            MailDetailScreen(
                uiState = uiState, 
                onBack = {},
                onDismissPreview = { dismissCalled = true }
            )
        }

        composeTestRule.onNodeWithContentDescription("닫기").performClick()
        assertTrue("Dismiss callback should be called", dismissCalled)
    }
}
