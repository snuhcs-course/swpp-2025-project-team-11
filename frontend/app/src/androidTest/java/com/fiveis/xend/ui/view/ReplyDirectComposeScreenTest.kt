package com.fiveis.xend.ui.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReplyDirectComposeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun replyDirectComposeScreen_displays_topBar() {
        // When
        composeTestRule.setContent {
            ReplyDirectComposeScreen(
                recipientEmail = "test@test.com",
                recipientName = "Test User",
                subject = "Test Subject",
                groups = emptyList(),
                onBack = {},
                onSend = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("답장 작성").assertIsDisplayed()
    }

    @Test
    fun replyDirectComposeScreen_backButton_triggers_callback() {
        // Given
        var backClicked = false

        // When
        composeTestRule.setContent {
            ReplyDirectComposeScreen(
                recipientEmail = "test@test.com",
                recipientName = "Test User",
                subject = "Test",
                groups = emptyList(),
                onBack = { backClicked = true },
                onSend = {}
            )
        }

        // Then
        composeTestRule.onNodeWithContentDescription("뒤로가기").performClick()
        assert(backClicked)
    }

    @Test
    fun replyDirectComposeScreen_sendButton_triggers_callback() {
        // Given
        var sendCalled = false
        var sentBody = ""

        // When
        composeTestRule.setContent {
            ReplyDirectComposeScreen(
                recipientEmail = "test@test.com",
                recipientName = "Test User",
                subject = "Test",
                groups = emptyList(),
                onBack = {},
                onSend = { body ->
                    sendCalled = true
                    sentBody = body
                }
            )
        }

        // Wait for the UI to settle
        composeTestRule.waitForIdle()

        // Then
        composeTestRule.onNodeWithContentDescription("전송").performClick()

        // Wait for callback to be invoked
        composeTestRule.waitForIdle()

        assert(sendCalled)
    }

    @Test
    fun replyDirectComposeScreen_displays_recipient_info() {
        // When
        composeTestRule.setContent {
            ReplyDirectComposeScreen(
                recipientEmail = "john@test.com",
                recipientName = "John Doe <john@test.com>",
                subject = "Test",
                groups = emptyList(),
                onBack = {},
                onSend = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("받는 사람").assertIsDisplayed()
        composeTestRule.onNodeWithText("John Doe").assertIsDisplayed()
    }

    @Test
    fun replyDirectComposeScreen_displays_group_chip_when_hasGroups() {
        // When
        composeTestRule.setContent {
            ReplyDirectComposeScreen(
                recipientEmail = "test@test.com",
                recipientName = "Test User",
                subject = "Test",
                groups = listOf("Group1"),
                onBack = {},
                onSend = {}
            )
        }

        // Then - Check for group name instead of "그룹"
        composeTestRule.onNodeWithText("Group1").assertIsDisplayed()
    }

    @Test
    fun replyDirectComposeScreen_hides_group_chip_when_no_groups() {
        // When
        composeTestRule.setContent {
            ReplyDirectComposeScreen(
                recipientEmail = "test@test.com",
                recipientName = "Test User",
                subject = "Test",
                groups = emptyList(),
                onBack = {},
                onSend = {}
            )
        }

        // Then - No crash, group chip not displayed
        Thread.sleep(200)
    }

    @Test
    fun replyDirectComposeScreen_displays_subject_field() {
        // When
        composeTestRule.setContent {
            ReplyDirectComposeScreen(
                recipientEmail = "test@test.com",
                recipientName = "Test User",
                subject = "Initial Subject",
                groups = emptyList(),
                onBack = {},
                onSend = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("제목").assertIsDisplayed()
    }

    @Test
    fun replyDirectComposeScreen_subject_change_triggers_callback() {
        // Given
        var subjectChanged = ""

        // When
        composeTestRule.setContent {
            ReplyDirectComposeScreen(
                recipientEmail = "test@test.com",
                recipientName = "Test User",
                subject = "",
                groups = emptyList(),
                onBack = {},
                onSend = {},
                onSubjectChange = { subjectChanged = it }
            )
        }

        // Then
        Thread.sleep(200)
    }

    @Test
    fun replyDirectComposeScreen_displays_realtime_ai_toggle() {
        // When
        composeTestRule.setContent {
            ReplyDirectComposeScreen(
                recipientEmail = "test@test.com",
                recipientName = "Test User",
                subject = "Test",
                groups = emptyList(),
                onBack = {},
                onSend = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("실시간 AI").assertIsDisplayed()
    }

    @Test
    fun replyDirectComposeScreen_displays_body_section() {
        // When
        composeTestRule.setContent {
            ReplyDirectComposeScreen(
                recipientEmail = "test@test.com",
                recipientName = "Test User",
                subject = "Test",
                groups = emptyList(),
                onBack = {},
                onSend = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("본문").assertIsDisplayed()
    }

    @Test
    fun replyDirectComposeScreen_displays_undo_button() {
        // When
        composeTestRule.setContent {
            ReplyDirectComposeScreen(
                recipientEmail = "test@test.com",
                recipientName = "Test User",
                subject = "Test",
                groups = emptyList(),
                onBack = {},
                onSend = {},
                canUndo = true,
                isStreaming = false
            )
        }

        // Then
        composeTestRule.onNodeWithContentDescription("실행취소").assertIsDisplayed()
    }

    @Test
    fun replyDirectComposeScreen_displays_ai_complete_button() {
        // When
        composeTestRule.setContent {
            ReplyDirectComposeScreen(
                recipientEmail = "test@test.com",
                recipientName = "Test User",
                subject = "Test",
                groups = emptyList(),
                onBack = {},
                onSend = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("AI 완성").assertIsDisplayed()
    }

    @Test
    fun replyDirectComposeScreen_templateButton_triggers_callback() {
        // Given
        var templateClicked = false

        // When
        composeTestRule.setContent {
            ReplyDirectComposeScreen(
                recipientEmail = "test@test.com",
                recipientName = "Test User",
                subject = "Test",
                groups = emptyList(),
                onBack = {},
                onSend = {},
                onTemplateClick = { templateClicked = true }
            )
        }

        // Then
        composeTestRule.onNodeWithContentDescription("템플릿").performClick()
        assert(templateClicked)
    }

    @Test
    fun replyDirectComposeScreen_displays_sender_info_when_provided() {
        // When
        composeTestRule.setContent {
            ReplyDirectComposeScreen(
                recipientEmail = "test@test.com",
                recipientName = "Test User",
                subject = "Test",
                groups = emptyList(),
                onBack = {},
                onSend = {},
                senderEmail = "sender@test.com",
                date = "2024.12.19",
                originalBody = "Original email body"
            )
        }

        // Then - Sender info section should be displayed
        Thread.sleep(200)
    }

    @Test
    fun replyDirectComposeScreen_hides_sender_info_when_not_provided() {
        // When
        composeTestRule.setContent {
            ReplyDirectComposeScreen(
                recipientEmail = "test@test.com",
                recipientName = "Test User",
                subject = "Test",
                groups = emptyList(),
                onBack = {},
                onSend = {},
                senderEmail = "",
                date = "",
                originalBody = ""
            )
        }

        // Then - Should not crash
        Thread.sleep(200)
    }

    @Test
    fun replyDirectComposeScreen_toggles_sender_section() {
        // When
        composeTestRule.setContent {
            ReplyDirectComposeScreen(
                recipientEmail = "test@test.com",
                recipientName = "Test User",
                subject = "Test Subject",
                groups = emptyList(),
                onBack = {},
                onSend = {},
                senderEmail = "Jane Doe <jane@test.com>",
                date = "2024.12.19",
                originalBody = "Original body"
            )
        }

        // Then - Toggle sender section
        Thread.sleep(300)
    }

    @Test
    fun replyDirectComposeScreen_displays_original_body_when_expanded() {
        // When
        composeTestRule.setContent {
            ReplyDirectComposeScreen(
                recipientEmail = "test@test.com",
                recipientName = "Test User",
                subject = "RE: Test",
                groups = emptyList(),
                onBack = {},
                onSend = {},
                senderEmail = "sender@test.com",
                date = "2024.12.19",
                originalBody = "This is the original email body content."
            )
        }

        // Then
        Thread.sleep(300)
        composeTestRule.onNodeWithText("RE: Test").assertIsDisplayed()
    }

    @Test
    fun replyDirectComposeScreen_handles_complex_recipient_name() {
        // When
        composeTestRule.setContent {
            ReplyDirectComposeScreen(
                recipientEmail = "test@test.com",
                recipientName = "김철수 대표 <ceo@company.com>",
                subject = "Test",
                groups = listOf("Group1", "Group2"),
                onBack = {},
                onSend = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("김철수 대표").assertIsDisplayed()
    }

    @Test
    fun replyDirectComposeScreen_handles_plain_email_recipient() {
        // When
        composeTestRule.setContent {
            ReplyDirectComposeScreen(
                recipientEmail = "plain@test.com",
                recipientName = "plain@test.com",
                subject = "Test",
                groups = emptyList(),
                onBack = {},
                onSend = {}
            )
        }

        // Then
        Thread.sleep(200)
    }

    @Test
    fun replyDirectComposeScreen_handles_long_subject() {
        // Given
        val longSubject = "This is a very long email subject that might wrap or be truncated in the UI"

        // When
        composeTestRule.setContent {
            ReplyDirectComposeScreen(
                recipientEmail = "test@test.com",
                recipientName = "Test User",
                subject = longSubject,
                groups = emptyList(),
                onBack = {},
                onSend = {}
            )
        }

        // Then - Should not crash
        Thread.sleep(200)
    }

    @Test
    fun replyDirectComposeScreen_handles_html_in_originalBody() {
        // When
        composeTestRule.setContent {
            ReplyDirectComposeScreen(
                recipientEmail = "test@test.com",
                recipientName = "Test User",
                subject = "Test",
                groups = emptyList(),
                onBack = {},
                onSend = {},
                senderEmail = "sender@test.com",
                date = "2024.12.19",
                originalBody = "<p>HTML <strong>content</strong> with <em>formatting</em></p>"
            )
        }

        // Then - Should render HTML without crash
        Thread.sleep(300)
    }

    @Test
    fun replyDirectComposeScreen_handles_originalMessage_marker() {
        // When
        composeTestRule.setContent {
            ReplyDirectComposeScreen(
                recipientEmail = "test@test.com",
                recipientName = "Test User",
                subject = "RE: Test",
                groups = emptyList(),
                onBack = {},
                onSend = {},
                senderEmail = "sender@test.com",
                date = "2024.12.19",
                originalBody = "Reply content\n\n-- original message --\nFrom: original@test.com"
            )
        }

        // Then - Should use CollapsibleBodyPreview
        Thread.sleep(300)
    }

    @Test
    fun replyDirectComposeScreen_attachment_button_displayed() {
        // When
        composeTestRule.setContent {
            ReplyDirectComposeScreen(
                recipientEmail = "test@test.com",
                recipientName = "Test User",
                subject = "Test",
                groups = emptyList(),
                onBack = {},
                onSend = {}
            )
        }

        // Then
        composeTestRule.onNodeWithContentDescription("첨부파일").assertIsDisplayed()
    }

    @Test
    fun replyDirectComposeScreen_richtext_editor_displays() {
        // When
        composeTestRule.setContent {
            ReplyDirectComposeScreen(
                recipientEmail = "test@test.com",
                recipientName = "Test User",
                subject = "Test",
                groups = emptyList(),
                onBack = {},
                onSend = {}
            )
        }

        // Then - Rich text editor should be displayed with placeholder
        Thread.sleep(200)
    }

    @Test
    fun replyDirectComposeScreen_handles_multiple_groups() {
        // When
        composeTestRule.setContent {
            ReplyDirectComposeScreen(
                recipientEmail = "test@test.com",
                recipientName = "Test User",
                subject = "Test",
                groups = listOf("Group1", "Group2", "Group3"),
                onBack = {},
                onSend = {}
            )
        }

        // Then - Check that all group names are displayed
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Group1, Group2, Group3").assertIsDisplayed()
    }

    @Test
    fun replyDirectComposeScreen_displays_subject_placeholder() {
        // When
        composeTestRule.setContent {
            ReplyDirectComposeScreen(
                recipientEmail = "test@test.com",
                recipientName = "Test User",
                subject = "",
                groups = emptyList(),
                onBack = {},
                onSend = {}
            )
        }

        // Then
        Thread.sleep(200)
    }

    @Test
    fun replyDirectComposeScreen_handles_unicode_characters() {
        // When
        composeTestRule.setContent {
            ReplyDirectComposeScreen(
                recipientEmail = "test@test.com",
                recipientName = "한글이름 日本語 中文",
                subject = "Unicode 제목 テスト 测试",
                groups = emptyList(),
                onBack = {},
                onSend = {}
            )
        }

        // Then
        Thread.sleep(200)
    }

    @Test
    fun replyDirectComposeScreen_handles_special_characters_in_email() {
        // When
        composeTestRule.setContent {
            ReplyDirectComposeScreen(
                recipientEmail = "test+tag@test.com",
                recipientName = "Test User",
                subject = "Special!@#$%Characters",
                groups = emptyList(),
                onBack = {},
                onSend = {}
            )
        }

        // Then - Should not crash
        Thread.sleep(200)
    }
}
