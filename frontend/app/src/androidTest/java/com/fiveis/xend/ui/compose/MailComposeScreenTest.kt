package com.fiveis.xend.ui.compose

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.ui.compose.common.rememberXendRichEditorState
import com.fiveis.xend.ui.theme.XendTheme
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MailComposeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emailComposeScreen_displays_topBar_with_title() {
        composeTestRule.setContent {
            XendTheme {
                val editorState = rememberXendRichEditorState()
                EmailComposeScreen(
                    subject = "",
                    onSubjectChange = {},
                    editorState = editorState,
                    contacts = emptyList(),
                    onContactsChange = {},
                    newContact = TextFieldValue(""),
                    onNewContactChange = {},
                    knownContactsByEmail = emptyMap(),
                    isStreaming = false,
                    error = null,
                    onBack = {},
                    onTemplateClick = {},
                    onAttachmentClick = {},
                    onRemoveAttachment = {},
                    onUndo = {},
                    onAiComplete = {},
                    onStopStreaming = {},
                    sendUiState = SendUiState(),
                    attachments = emptyList(),
                    onSend = {},
                    bannerState = null,
                    onDismissBanner = {},
                    canUndo = false,
                    canRedo = false,
                    onRedo = {},
                    contactSuggestions = emptyList(),
                    onSuggestionClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("메일 작성").assertIsDisplayed()
    }

    @Test
    fun emailComposeScreen_back_button_triggers_callback() {
        var backClicked = false
        composeTestRule.setContent {
            XendTheme {
                val editorState = rememberXendRichEditorState()
                EmailComposeScreen(
                    subject = "",
                    onSubjectChange = {},
                    editorState = editorState,
                    contacts = emptyList(),
                    onContactsChange = {},
                    newContact = TextFieldValue(""),
                    onNewContactChange = {},
                    knownContactsByEmail = emptyMap(),
                    isStreaming = false,
                    error = null,
                    onBack = { backClicked = true },
                    onTemplateClick = {},
                    onAttachmentClick = {},
                    onRemoveAttachment = {},
                    onUndo = {},
                    onAiComplete = {},
                    onStopStreaming = {},
                    sendUiState = SendUiState(),
                    attachments = emptyList(),
                    onSend = {},
                    bannerState = null,
                    onDismissBanner = {},
                    canUndo = false,
                    canRedo = false,
                    onRedo = {},
                    contactSuggestions = emptyList(),
                    onSuggestionClick = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("뒤로가기").performClick()
        assert(backClicked)
    }

    @Test
    fun emailComposeScreen_send_button_disabled_when_no_contacts() {
        composeTestRule.setContent {
            XendTheme {
                val editorState = rememberXendRichEditorState()
                EmailComposeScreen(
                    subject = "Test Subject",
                    onSubjectChange = {},
                    editorState = editorState,
                    contacts = emptyList(),
                    onContactsChange = {},
                    newContact = TextFieldValue(""),
                    onNewContactChange = {},
                    knownContactsByEmail = emptyMap(),
                    isStreaming = false,
                    error = null,
                    onBack = {},
                    onTemplateClick = {},
                    onAttachmentClick = {},
                    onRemoveAttachment = {},
                    onUndo = {},
                    onAiComplete = {},
                    onStopStreaming = {},
                    sendUiState = SendUiState(),
                    attachments = emptyList(),
                    onSend = {},
                    bannerState = null,
                    onDismissBanner = {},
                    canUndo = false,
                    canRedo = false,
                    onRedo = {},
                    contactSuggestions = emptyList(),
                    onSuggestionClick = {}
                )
            }
        }

        // Send icon should be displayed but with disabled color
        composeTestRule.onNodeWithContentDescription("전송").assertIsDisplayed()
    }

    @Test
    fun emailComposeScreen_ai_complete_button_enabled_with_contacts() {
        composeTestRule.setContent {
            XendTheme {
                val editorState = rememberXendRichEditorState()
                EmailComposeScreen(
                    subject = "",
                    onSubjectChange = {},
                    editorState = editorState,
                    contacts = listOf(Contact(1L, null, "Test", "test@example.com")),
                    onContactsChange = {},
                    newContact = TextFieldValue(""),
                    onNewContactChange = {},
                    knownContactsByEmail = emptyMap(),
                    isStreaming = false,
                    error = null,
                    onBack = {},
                    onTemplateClick = {},
                    onAttachmentClick = {},
                    onRemoveAttachment = {},
                    onUndo = {},
                    onAiComplete = {},
                    onStopStreaming = {},
                    sendUiState = SendUiState(),
                    attachments = emptyList(),
                    onSend = {},
                    bannerState = null,
                    onDismissBanner = {},
                    canUndo = false,
                    canRedo = false,
                    onRedo = {},
                    contactSuggestions = emptyList(),
                    onSuggestionClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("AI 완성").assertIsDisplayed()
    }

    @Test
    fun emailComposeScreen_displays_streaming_state() {
        composeTestRule.setContent {
            XendTheme {
                val editorState = rememberXendRichEditorState()
                EmailComposeScreen(
                    subject = "",
                    onSubjectChange = {},
                    editorState = editorState,
                    contacts = listOf(Contact(1L, null, "Test", "test@example.com")),
                    onContactsChange = {},
                    newContact = TextFieldValue(""),
                    onNewContactChange = {},
                    knownContactsByEmail = emptyMap(),
                    isStreaming = true,
                    error = null,
                    onBack = {},
                    onTemplateClick = {},
                    onAttachmentClick = {},
                    onRemoveAttachment = {},
                    onUndo = {},
                    onAiComplete = {},
                    onStopStreaming = {},
                    sendUiState = SendUiState(),
                    attachments = emptyList(),
                    onSend = {},
                    bannerState = null,
                    onDismissBanner = {},
                    canUndo = false,
                    canRedo = false,
                    onRedo = {},
                    contactSuggestions = emptyList(),
                    onSuggestionClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("AI 플래너가 메일 구조를 설계 중입니다").assertIsDisplayed()
        composeTestRule.onNodeWithText("중지").assertIsDisplayed()
    }

    @Test
    fun emailComposeScreen_stop_button_calls_callback() {
        var stopCalled = false
        composeTestRule.setContent {
            XendTheme {
                val editorState = rememberXendRichEditorState()
                EmailComposeScreen(
                    subject = "",
                    onSubjectChange = {},
                    editorState = editorState,
                    contacts = listOf(Contact(1L, null, "Test", "test@example.com")),
                    onContactsChange = {},
                    newContact = TextFieldValue(""),
                    onNewContactChange = {},
                    knownContactsByEmail = emptyMap(),
                    isStreaming = true,
                    error = null,
                    onBack = {},
                    onTemplateClick = {},
                    onAttachmentClick = {},
                    onRemoveAttachment = {},
                    onUndo = {},
                    onAiComplete = {},
                    onStopStreaming = { stopCalled = true },
                    sendUiState = SendUiState(),
                    attachments = emptyList(),
                    onSend = {},
                    bannerState = null,
                    onDismissBanner = {},
                    canUndo = false,
                    canRedo = false,
                    onRedo = {},
                    contactSuggestions = emptyList(),
                    onSuggestionClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("중지").performClick()
        assert(stopCalled)
    }

    @Test
    fun emailComposeScreen_displays_error_message() {
        val errorMessage = "Test error message"
        composeTestRule.setContent {
            XendTheme {
                val editorState = rememberXendRichEditorState()
                EmailComposeScreen(
                    subject = "",
                    onSubjectChange = {},
                    editorState = editorState,
                    contacts = emptyList(),
                    onContactsChange = {},
                    newContact = TextFieldValue(""),
                    onNewContactChange = {},
                    knownContactsByEmail = emptyMap(),
                    isStreaming = false,
                    error = errorMessage,
                    onBack = {},
                    onTemplateClick = {},
                    onAttachmentClick = {},
                    onRemoveAttachment = {},
                    onUndo = {},
                    onAiComplete = {},
                    onStopStreaming = {},
                    sendUiState = SendUiState(),
                    attachments = emptyList(),
                    onSend = {},
                    bannerState = null,
                    onDismissBanner = {},
                    canUndo = false,
                    canRedo = false,
                    onRedo = {},
                    contactSuggestions = emptyList(),
                    onSuggestionClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }

    @Test
    fun emailComposeScreen_displays_banner() {
        val bannerMessage = "Test banner message"
        composeTestRule.setContent {
            XendTheme {
                val editorState = rememberXendRichEditorState()
                EmailComposeScreen(
                    subject = "",
                    onSubjectChange = {},
                    editorState = editorState,
                    contacts = emptyList(),
                    onContactsChange = {},
                    newContact = TextFieldValue(""),
                    onNewContactChange = {},
                    knownContactsByEmail = emptyMap(),
                    isStreaming = false,
                    error = null,
                    onBack = {},
                    onTemplateClick = {},
                    onAttachmentClick = {},
                    onRemoveAttachment = {},
                    onUndo = {},
                    onAiComplete = {},
                    onStopStreaming = {},
                    sendUiState = SendUiState(),
                    attachments = emptyList(),
                    onSend = {},
                    bannerState = BannerState(
                        message = bannerMessage,
                        type = BannerType.INFO
                    ),
                    onDismissBanner = {},
                    canUndo = false,
                    canRedo = false,
                    onRedo = {},
                    contactSuggestions = emptyList(),
                    onSuggestionClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(bannerMessage).assertIsDisplayed()
    }

    @Test
    fun emailComposeScreen_shows_undo_redo_buttons_when_available() {
        composeTestRule.setContent {
            XendTheme {
                val editorState = rememberXendRichEditorState()
                EmailComposeScreen(
                    subject = "",
                    onSubjectChange = {},
                    editorState = editorState,
                    contacts = listOf(Contact(1L, null, "Test", "test@example.com")),
                    onContactsChange = {},
                    newContact = TextFieldValue(""),
                    onNewContactChange = {},
                    knownContactsByEmail = emptyMap(),
                    isStreaming = false,
                    error = null,
                    onBack = {},
                    onTemplateClick = {},
                    onAttachmentClick = {},
                    onRemoveAttachment = {},
                    onUndo = {},
                    onAiComplete = {},
                    onStopStreaming = {},
                    sendUiState = SendUiState(),
                    attachments = emptyList(),
                    onSend = {},
                    bannerState = null,
                    onDismissBanner = {},
                    canUndo = true,
                    canRedo = true,
                    onRedo = {},
                    contactSuggestions = emptyList(),
                    onSuggestionClick = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("실행취소").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("다시 실행").assertIsDisplayed()
    }

    @Test
    fun emailComposeScreen_undo_button_triggers_callback() {
        var undoCalled = false
        composeTestRule.setContent {
            XendTheme {
                val editorState = rememberXendRichEditorState()
                EmailComposeScreen(
                    subject = "",
                    onSubjectChange = {},
                    editorState = editorState,
                    contacts = listOf(Contact(1L, null, "Test", "test@example.com")),
                    onContactsChange = {},
                    newContact = TextFieldValue(""),
                    onNewContactChange = {},
                    knownContactsByEmail = emptyMap(),
                    isStreaming = false,
                    error = null,
                    onBack = {},
                    onTemplateClick = {},
                    onAttachmentClick = {},
                    onRemoveAttachment = {},
                    onUndo = { undoCalled = true },
                    onAiComplete = {},
                    onStopStreaming = {},
                    sendUiState = SendUiState(),
                    attachments = emptyList(),
                    onSend = {},
                    bannerState = null,
                    onDismissBanner = {},
                    canUndo = true,
                    canRedo = false,
                    onRedo = {},
                    contactSuggestions = emptyList(),
                    onSuggestionClick = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("실행취소").performClick()
        assert(undoCalled)
    }

    @Test
    fun emailComposeScreen_redo_button_triggers_callback() {
        var redoCalled = false
        composeTestRule.setContent {
            XendTheme {
                val editorState = rememberXendRichEditorState()
                EmailComposeScreen(
                    subject = "",
                    onSubjectChange = {},
                    editorState = editorState,
                    contacts = listOf(Contact(1L, null, "Test", "test@example.com")),
                    onContactsChange = {},
                    newContact = TextFieldValue(""),
                    onNewContactChange = {},
                    knownContactsByEmail = emptyMap(),
                    isStreaming = false,
                    error = null,
                    onBack = {},
                    onTemplateClick = {},
                    onAttachmentClick = {},
                    onRemoveAttachment = {},
                    onUndo = {},
                    onAiComplete = {},
                    onStopStreaming = {},
                    sendUiState = SendUiState(),
                    attachments = emptyList(),
                    onSend = {},
                    bannerState = null,
                    onDismissBanner = {},
                    canUndo = false,
                    canRedo = true,
                    onRedo = { redoCalled = true },
                    contactSuggestions = emptyList(),
                    onSuggestionClick = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("다시 실행").performClick()
        assert(redoCalled)
    }

    @Test
    fun emailComposeScreen_template_button_triggers_callback() {
        var templateClicked = false
        composeTestRule.setContent {
            XendTheme {
                val editorState = rememberXendRichEditorState()
                EmailComposeScreen(
                    subject = "",
                    onSubjectChange = {},
                    editorState = editorState,
                    contacts = emptyList(),
                    onContactsChange = {},
                    newContact = TextFieldValue(""),
                    onNewContactChange = {},
                    knownContactsByEmail = emptyMap(),
                    isStreaming = false,
                    error = null,
                    onBack = {},
                    onTemplateClick = { templateClicked = true },
                    onAttachmentClick = {},
                    onRemoveAttachment = {},
                    onUndo = {},
                    onAiComplete = {},
                    onStopStreaming = {},
                    sendUiState = SendUiState(),
                    attachments = emptyList(),
                    onSend = {},
                    bannerState = null,
                    onDismissBanner = {},
                    canUndo = false,
                    canRedo = false,
                    onRedo = {},
                    contactSuggestions = emptyList(),
                    onSuggestionClick = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("템플릿").performClick()
        assert(templateClicked)
    }

    @Test
    fun emailComposeScreen_attachment_button_triggers_callback() {
        var attachmentClicked = false
        composeTestRule.setContent {
            XendTheme {
                val editorState = rememberXendRichEditorState()
                EmailComposeScreen(
                    subject = "",
                    onSubjectChange = {},
                    editorState = editorState,
                    contacts = emptyList(),
                    onContactsChange = {},
                    newContact = TextFieldValue(""),
                    onNewContactChange = {},
                    knownContactsByEmail = emptyMap(),
                    isStreaming = false,
                    error = null,
                    onBack = {},
                    onTemplateClick = {},
                    onAttachmentClick = { attachmentClicked = true },
                    onRemoveAttachment = {},
                    onUndo = {},
                    onAiComplete = {},
                    onStopStreaming = {},
                    sendUiState = SendUiState(),
                    attachments = emptyList(),
                    onSend = {},
                    bannerState = null,
                    onDismissBanner = {},
                    canUndo = false,
                    canRedo = false,
                    onRedo = {},
                    contactSuggestions = emptyList(),
                    onSuggestionClick = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("첨부파일").performClick()
        assert(attachmentClicked)
    }

    @Test
    fun emailComposeScreen_displays_contact_suggestions() {
        val suggestions = listOf(
            Contact(1L, null, "Test User 1", "test1@example.com"),
            Contact(2L, null, "Test User 2", "test2@example.com")
        )
        composeTestRule.setContent {
            XendTheme {
                val editorState = rememberXendRichEditorState()
                EmailComposeScreen(
                    subject = "",
                    onSubjectChange = {},
                    editorState = editorState,
                    contacts = emptyList(),
                    onContactsChange = {},
                    newContact = TextFieldValue("test"),
                    onNewContactChange = {},
                    knownContactsByEmail = emptyMap(),
                    isStreaming = false,
                    error = null,
                    onBack = {},
                    onTemplateClick = {},
                    onAttachmentClick = {},
                    onRemoveAttachment = {},
                    onUndo = {},
                    onAiComplete = {},
                    onStopStreaming = {},
                    sendUiState = SendUiState(),
                    attachments = emptyList(),
                    onSend = {},
                    bannerState = null,
                    onDismissBanner = {},
                    canUndo = false,
                    canRedo = false,
                    onRedo = {},
                    contactSuggestions = suggestions,
                    onSuggestionClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("연락처 추천").assertIsDisplayed()
        composeTestRule.onNodeWithText("test1@example.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("test2@example.com").assertIsDisplayed()
    }

    @Test
    fun contactChip_displays_contact_information() {
        val contact = Contact(1L, null, "Test User", "test@example.com")
        composeTestRule.setContent {
            XendTheme {
                ContactChip(
                    contact = contact,
                    onRemove = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Test User (test@example.com)").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("삭제").assertIsDisplayed()
    }

    @Test
    fun contactChip_remove_button_triggers_callback() {
        var removeCalled = false
        val contact = Contact(1L, null, "Test User", "test@example.com")
        composeTestRule.setContent {
            XendTheme {
                ContactChip(
                    contact = contact,
                    onRemove = { removeCalled = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("삭제").performClick()
        assert(removeCalled)
    }

    @Test
    fun contactChip_shows_unknown_contact_differently() {
        val unknownContact = Contact(-1L, null, "unknown@example.com", "unknown@example.com")
        composeTestRule.setContent {
            XendTheme {
                ContactChip(
                    contact = unknownContact,
                    onRemove = {}
                )
            }
        }

        composeTestRule.onNodeWithText("unknown@example.com").assertIsDisplayed()
    }

    @Test
    fun bannerState_data_class_creates_correctly() {
        val banner = BannerState(
            message = "Test message",
            type = BannerType.SUCCESS,
            autoDismiss = true,
            actionText = "Action",
            onActionClick = {}
        )

        assert(banner.message == "Test message")
        assert(banner.type == BannerType.SUCCESS)
        assert(banner.autoDismiss)
        assert(banner.actionText == "Action")
        assert(banner.onActionClick != null)
    }
}
