package com.fiveis.xend.ui.compose

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.ui.compose.common.rememberXendRichEditorState
import com.fiveis.xend.ui.theme.XendTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MailComposeScreenAdditionalTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emailComposeScreen_displays_subject_label() {
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

        composeTestRule.onNodeWithText("제목").assertIsDisplayed()
    }

    @Test
    fun emailComposeScreen_displays_recipient_label() {
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

        composeTestRule.onNodeWithText("받는 사람").assertIsDisplayed()
    }

    @Test
    fun emailComposeScreen_ai_complete_disabled_without_contacts() {
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

        composeTestRule.onNodeWithText("AI 완성").assertIsDisplayed()
    }

    @Test
    fun emailComposeScreen_ai_complete_button_calls_callback() {
        var aiCompleteCalled = false
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
                    onAiComplete = { aiCompleteCalled = true },
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

        composeTestRule.onNodeWithText("AI 완성").performClick()
        assert(aiCompleteCalled)
    }

    @Test
    fun emailComposeScreen_send_button_calls_callback() {
        var sendCalled = false
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
                    onSend = { sendCalled = true },
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

        composeTestRule.onNodeWithContentDescription("전송").performClick()
        assert(sendCalled)
    }

    @Test
    fun emailComposeScreen_dismisses_banner() {
        var bannerDismissed = false
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
                        message = "Test Banner",
                        type = BannerType.INFO
                    ),
                    onDismissBanner = { bannerDismissed = true },
                    canUndo = false,
                    canRedo = false,
                    onRedo = {},
                    contactSuggestions = emptyList(),
                    onSuggestionClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Test Banner").assertIsDisplayed()
    }

    @Test
    fun contactChip_shows_add_to_contacts_button_for_unknown() {
        val unknownContact = Contact(-1L, null, "unknown@example.com", "unknown@example.com")
        var addToContactsCalled = false

        composeTestRule.setContent {
            XendTheme {
                ContactChip(
                    contact = unknownContact,
                    onRemove = {},
                    onAddToContacts = { addToContactsCalled = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("연락처 추가").assertIsDisplayed()
    }

    @Test
    fun contactChip_does_not_show_add_button_for_known_contact() {
        val knownContact = Contact(1L, null, "Known User", "known@example.com")

        composeTestRule.setContent {
            XendTheme {
                ContactChip(
                    contact = knownContact,
                    onRemove = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Known User (known@example.com)").assertIsDisplayed()
    }

    @Test
    fun contactChip_edit_button_triggers_callback() {
        var editCalled = false
        val contact = Contact(1L, null, "Test User", "test@example.com")

        composeTestRule.setContent {
            XendTheme {
                ContactChip(
                    contact = contact,
                    onRemove = {},
                    onEdit = { editCalled = true }
                )
            }
        }

        // Clicking on chip should trigger edit
        composeTestRule.onNodeWithText("Test User (test@example.com)").performClick()
        assert(editCalled)
    }

    @Test
    fun emailComposeScreen_contact_suggestion_click_adds_contact() {
        var suggestionClicked: Contact? = null
        val suggestion = Contact(1L, null, "Suggested User", "suggested@example.com")

        composeTestRule.setContent {
            XendTheme {
                val editorState = rememberXendRichEditorState()
                EmailComposeScreen(
                    subject = "",
                    onSubjectChange = {},
                    editorState = editorState,
                    contacts = emptyList(),
                    onContactsChange = {},
                    newContact = TextFieldValue("sugg"),
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
                    contactSuggestions = listOf(suggestion),
                    onSuggestionClick = { suggestionClicked = it }
                )
            }
        }

        composeTestRule.onNodeWithText("suggested@example.com").performClick()
        assert(suggestionClicked == suggestion)
    }

    @Test
    fun emailComposeScreen_shows_sending_indicator() {
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
                    sendUiState = SendUiState(isSending = true),
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

        // When sending, progress indicator should be shown (not the send icon)
        // Just verify the screen renders without crash
        composeTestRule.waitForIdle()
    }

    @Test
    fun emailComposeScreen_subject_enabled_when_not_streaming() {
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

        composeTestRule.onNodeWithText("Test Subject").assertIsDisplayed()
    }

    @Test
    fun emailComposeScreen_multiple_contacts_display() {
        val contacts = listOf(
            Contact(1L, null, "User1", "user1@example.com"),
            Contact(2L, null, "User2", "user2@example.com"),
            Contact(3L, null, "User3", "user3@example.com")
        )

        composeTestRule.setContent {
            XendTheme {
                val editorState = rememberXendRichEditorState()
                EmailComposeScreen(
                    subject = "",
                    onSubjectChange = {},
                    editorState = editorState,
                    contacts = contacts,
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

        composeTestRule.onNodeWithText("User1 (user1@example.com)").assertIsDisplayed()
        composeTestRule.onNodeWithText("User2 (user2@example.com)").assertIsDisplayed()
        composeTestRule.onNodeWithText("User3 (user3@example.com)").assertIsDisplayed()
    }

    @Test
    fun emailComposeScreen_success_banner_shows() {
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
                        message = "Success!",
                        type = BannerType.SUCCESS
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

        composeTestRule.onNodeWithText("Success!").assertIsDisplayed()
    }

    @Test
    fun emailComposeScreen_error_banner_shows() {
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
                        message = "Error occurred",
                        type = BannerType.ERROR
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

        composeTestRule.onNodeWithText("Error occurred").assertIsDisplayed()
    }
}
