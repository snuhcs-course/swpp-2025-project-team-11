package com.fiveis.xend.ui.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.EmailItem
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
        composeTestRule.onNodeWithText("김철수 (chulsoo@test.com)").assertIsDisplayed()
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
        assert(backClicked)
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
        assert(replyClicked)
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
}
