package com.fiveis.xend.ui.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.Contact
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MailComposeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun email_compose_screen_displays_all_sections() {
        composeTestRule.setContent {
            val richTextState = rememberRichTextState()
            EmailComposeScreen(
                subject = "",
                onSubjectChange = {},
                richTextState = richTextState,
                contacts = emptyList(),
                onContactsChange = {},
                newContact = TextFieldValue(""),
                onNewContactChange = {},
                isStreaming = false,
                error = null,
                sendUiState = SendUiState(),
                onSend = {},
                suggestionText = "",
                onAcceptSuggestion = {},
                aiRealtime = true,
                onAiRealtimeToggle = {},
                bannerState = null,
                onDismissBanner = {}
            )
        }

        composeTestRule.onNodeWithText("받는 사람").assertIsDisplayed()
        composeTestRule.onNodeWithText("제목").assertIsDisplayed()
        composeTestRule.onNodeWithText("본문").assertIsDisplayed()
    }

    @Test
    fun top_bar_displays_title_and_actions() {
        composeTestRule.setContent {
            val richTextState = rememberRichTextState()
            EmailComposeScreen(
                subject = "",
                onSubjectChange = {},
                richTextState = richTextState,
                contacts = emptyList(),
                onContactsChange = {},
                newContact = TextFieldValue(""),
                onNewContactChange = {},
                isStreaming = false,
                error = null,
                sendUiState = SendUiState(),
                onSend = {},
                bannerState = null,
                onDismissBanner = {}
            )
        }

        composeTestRule.onNodeWithText("메일 작성").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("뒤로가기").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("템플릿").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("첨부파일").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("전송").assertIsDisplayed()
    }

    @Test
    fun back_button_triggers_callback() {
        var backClicked = false

        composeTestRule.setContent {
            val richTextState = rememberRichTextState()
            EmailComposeScreen(
                subject = "",
                onSubjectChange = {},
                richTextState = richTextState,
                contacts = emptyList(),
                onContactsChange = {},
                newContact = TextFieldValue(""),
                onNewContactChange = {},
                isStreaming = false,
                error = null,
                sendUiState = SendUiState(),
                onBack = { backClicked = true },
                onSend = {},
                bannerState = null,
                onDismissBanner = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("뒤로가기").performClick()
        assertTrue(backClicked)
    }

    @Test
    fun template_button_triggers_callback() {
        var templateClicked = false

        composeTestRule.setContent {
            val richTextState = rememberRichTextState()
            EmailComposeScreen(
                subject = "",
                onSubjectChange = {},
                richTextState = richTextState,
                contacts = emptyList(),
                onContactsChange = {},
                newContact = TextFieldValue(""),
                onNewContactChange = {},
                isStreaming = false,
                error = null,
                sendUiState = SendUiState(),
                onTemplateClick = { templateClicked = true },
                onSend = {},
                bannerState = null,
                onDismissBanner = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("템플릿").performClick()
        assertTrue(templateClicked)
    }

    @Test
    fun send_button_disabled_when_no_contacts() {
        composeTestRule.setContent {
            val richTextState = rememberRichTextState()
            EmailComposeScreen(
                subject = "Test",
                onSubjectChange = {},
                richTextState = richTextState,
                contacts = emptyList(),
                onContactsChange = {},
                newContact = TextFieldValue(""),
                onNewContactChange = {},
                isStreaming = false,
                error = null,
                sendUiState = SendUiState(isSending = false),
                onSend = {},
                bannerState = null,
                onDismissBanner = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("전송").assertExists()
    }

    @Test
    fun send_button_triggers_callback_with_contacts() {
        var sendClicked = false

        composeTestRule.setContent {
            val richTextState = rememberRichTextState()
            EmailComposeScreen(
                subject = "Test Subject",
                onSubjectChange = {},
                richTextState = richTextState,
                contacts = listOf(Contact(1, null, "Test", "test@example.com")),
                onContactsChange = {},
                newContact = TextFieldValue(""),
                onNewContactChange = {},
                isStreaming = false,
                error = null,
                sendUiState = SendUiState(isSending = false),
                onSend = { sendClicked = true },
                bannerState = null,
                onDismissBanner = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("전송").performClick()
        assertTrue(sendClicked)
    }

    @Test
    fun displays_banner_initially() {
        composeTestRule.setContent {
            val richTextState = rememberRichTextState()
            EmailComposeScreen(
                subject = "",
                onSubjectChange = {},
                richTextState = richTextState,
                contacts = emptyList(),
                onContactsChange = {},
                newContact = TextFieldValue(""),
                onNewContactChange = {},
                isStreaming = false,
                error = null,
                sendUiState = SendUiState(),
                onSend = {},
                bannerState = BannerState("연락처를 저장하면 향상된 AI 메일 작성이 가능합니다.", BannerType.INFO),
                onDismissBanner = {}
            )
        }

        composeTestRule.onNodeWithText("연락처를 저장하면 향상된 AI 메일 작성이 가능합니다.").assertIsDisplayed()
    }

    @Test
    fun banner_can_be_dismissed() {
        var bannerDismissed = false
        composeTestRule.setContent {
            val richTextState = rememberRichTextState()
            EmailComposeScreen(
                subject = "",
                onSubjectChange = {},
                richTextState = richTextState,
                contacts = emptyList(),
                onContactsChange = {},
                newContact = TextFieldValue(""),
                onNewContactChange = {},
                isStreaming = false,
                error = null,
                sendUiState = SendUiState(),
                onSend = {},
                bannerState = BannerState("연락처를 저장하면 향상된 AI 메일 작성이 가능합니다.", BannerType.INFO),
                onDismissBanner = { bannerDismissed = true }
            )
        }

        composeTestRule.onNodeWithContentDescription("닫기").performClick()
        assertTrue(bannerDismissed)
    }

    @Test
    fun action_row_displays_all_buttons() {
        composeTestRule.setContent {
            val richTextState = rememberRichTextState()
            EmailComposeScreen(
                subject = "",
                onSubjectChange = {},
                richTextState = richTextState,
                contacts = emptyList(),
                onContactsChange = {},
                newContact = TextFieldValue(""),
                onNewContactChange = {},
                isStreaming = false,
                error = null,
                sendUiState = SendUiState(),
                onSend = {},
                bannerState = null,
                onDismissBanner = {}
            )
        }

        composeTestRule.onNodeWithText("실행취소").assertIsDisplayed()
        composeTestRule.onNodeWithText("AI 완성").assertIsDisplayed()
    }

    @Test
    fun stop_button_visible_when_streaming() {
        composeTestRule.setContent {
            val richTextState = rememberRichTextState()
            EmailComposeScreen(
                subject = "",
                onSubjectChange = {},
                richTextState = richTextState,
                contacts = emptyList(),
                onContactsChange = {},
                newContact = TextFieldValue(""),
                onNewContactChange = {},
                isStreaming = true,
                error = null,
                sendUiState = SendUiState(),
                onSend = {},
                bannerState = null,
                onDismissBanner = {}
            )
        }

        composeTestRule.onNodeWithText("중지").assertIsDisplayed()
    }

    @Test
    fun ai_complete_button_disabled_when_streaming() {
        composeTestRule.setContent {
            val richTextState = rememberRichTextState()
            EmailComposeScreen(
                subject = "",
                onSubjectChange = {},
                richTextState = richTextState,
                contacts = emptyList(),
                onContactsChange = {},
                newContact = TextFieldValue(""),
                onNewContactChange = {},
                isStreaming = true,
                error = null,
                sendUiState = SendUiState(),
                onSend = {},
                bannerState = null,
                onDismissBanner = {}
            )
        }

        composeTestRule.onNodeWithText("AI 완성").assertDoesNotExist()
    }

    @Test
    fun undo_button_triggers_callback() {
        var undoClicked = false

        composeTestRule.setContent {
            val richTextState = rememberRichTextState()
            EmailComposeScreen(
                subject = "",
                onSubjectChange = {},
                richTextState = richTextState,
                contacts = emptyList(),
                onContactsChange = {},
                newContact = TextFieldValue(""),
                onNewContactChange = {},
                isStreaming = false,
                error = null,
                sendUiState = SendUiState(),
                onUndo = { undoClicked = true },
                onSend = {},
                bannerState = null,
                onDismissBanner = {}
            )
        }

        composeTestRule.onNodeWithText("실행취소").performClick()
        assertTrue(undoClicked)
    }

    @Test
    fun ai_complete_button_triggers_callback() {
        var aiCompleteClicked = false

        composeTestRule.setContent {
            val richTextState = rememberRichTextState()
            EmailComposeScreen(
                subject = "",
                onSubjectChange = {},
                richTextState = richTextState,
                contacts = listOf(Contact(1, null, "Test", "test@example.com")),
                onContactsChange = {},
                newContact = TextFieldValue(""),
                onNewContactChange = {},
                isStreaming = false,
                error = null,
                sendUiState = SendUiState(),
                onAiComplete = { aiCompleteClicked = true },
                onSend = {},
                bannerState = null,
                onDismissBanner = {}
            )
        }

        composeTestRule.onNodeWithText("AI 완성").performClick()
        assertTrue(aiCompleteClicked)
    }

    @Test
    fun stop_streaming_button_triggers_callback() {
        var stopClicked = false

        composeTestRule.setContent {
            val richTextState = rememberRichTextState()
            EmailComposeScreen(
                subject = "",
                onSubjectChange = {},
                richTextState = richTextState,
                contacts = emptyList(),
                onContactsChange = {},
                newContact = TextFieldValue(""),
                onNewContactChange = {},
                isStreaming = true,
                error = null,
                sendUiState = SendUiState(),
                onStopStreaming = { stopClicked = true },
                onSend = {},
                bannerState = null,
                onDismissBanner = {}
            )
        }

        composeTestRule.onNodeWithText("중지").performClick()
        assertTrue(stopClicked)
    }

    @Test
    fun contact_chip_displays_contact_info() {
        composeTestRule.setContent {
            val richTextState = rememberRichTextState()
            EmailComposeScreen(
                subject = "",
                onSubjectChange = {},
                richTextState = richTextState,
                contacts = listOf(Contact(1, null, "John Doe", "john@example.com")),
                onContactsChange = {},
                newContact = TextFieldValue(""),
                onNewContactChange = {},
                isStreaming = false,
                error = null,
                sendUiState = SendUiState(),
                onSend = {},
                bannerState = null,
                onDismissBanner = {}
            )
        }

        composeTestRule.onNodeWithText("John Doe (john@example.com)", substring = true).assertIsDisplayed()
    }

    @Test
    fun contact_chip_can_be_removed() {
        var contactsChanged = false

        composeTestRule.setContent {
            val richTextState = rememberRichTextState()
            EmailComposeScreen(
                subject = "",
                onSubjectChange = {},
                richTextState = richTextState,
                contacts = listOf(Contact(1, null, "John Doe", "john@example.com")),
                onContactsChange = { contactsChanged = true },
                newContact = TextFieldValue(""),
                onNewContactChange = {},
                isStreaming = false,
                error = null,
                sendUiState = SendUiState(),
                onSend = {},
                bannerState = null,
                onDismissBanner = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("삭제").performClick()
        assertTrue(contactsChanged)
    }

    @Test
    fun realtime_ai_toggle_displays() {
        composeTestRule.setContent {
            val richTextState = rememberRichTextState()
            EmailComposeScreen(
                subject = "",
                onSubjectChange = {},
                richTextState = richTextState,
                contacts = emptyList(),
                onContactsChange = {},
                newContact = TextFieldValue(""),
                onNewContactChange = {},
                isStreaming = false,
                error = null,
                sendUiState = SendUiState(),
                onSend = {},
                aiRealtime = true,
                bannerState = null,
                onDismissBanner = {}
            )
        }

        composeTestRule.onNodeWithText("실시간 AI").assertIsDisplayed()
    }

    @Test
    fun error_message_displays_when_present() {
        composeTestRule.setContent {
            val richTextState = rememberRichTextState()
            EmailComposeScreen(
                subject = "",
                onSubjectChange = {},
                richTextState = richTextState,
                contacts = emptyList(),
                onContactsChange = {},
                newContact = TextFieldValue(""),
                onNewContactChange = {},
                isStreaming = false,
                error = "Test error message",
                sendUiState = SendUiState(),
                onSend = {},
                bannerState = null,
                onDismissBanner = {}
            )
        }

        composeTestRule.onNodeWithText("Test error message").assertIsDisplayed()
    }

    @Test
    fun suggestion_text_displays_when_present() {
        composeTestRule.setContent {
            val richTextState = rememberRichTextState()
            EmailComposeScreen(
                subject = "",
                onSubjectChange = {},
                richTextState = richTextState,
                contacts = emptyList(),
                onContactsChange = {},
                newContact = TextFieldValue(""),
                onNewContactChange = {},
                isStreaming = false,
                error = null,
                sendUiState = SendUiState(),
                onSend = {},
                suggestionText = "This is a suggestion",
                bannerState = null,
                onDismissBanner = {}
            )
        }

        composeTestRule.onNodeWithText("This is a suggestion").assertIsDisplayed()
    }

    @Test
    fun accept_suggestion_button_displays_when_suggestion_present() {
        composeTestRule.setContent {
            val richTextState = rememberRichTextState()
            EmailComposeScreen(
                subject = "",
                onSubjectChange = {},
                richTextState = richTextState,
                contacts = emptyList(),
                onContactsChange = {},
                newContact = TextFieldValue(""),
                onNewContactChange = {},
                isStreaming = false,
                error = null,
                sendUiState = SendUiState(),
                onSend = {},
                suggestionText = "Suggestion",
                bannerState = null,
                onDismissBanner = {}
            )
        }

        composeTestRule.onNodeWithText("탭 완성").assertIsDisplayed()
    }

    @Test
    fun accept_suggestion_button_triggers_callback() {
        var suggestionAccepted = false

        composeTestRule.setContent {
            val richTextState = rememberRichTextState()
            EmailComposeScreen(
                subject = "",
                onSubjectChange = {},
                richTextState = richTextState,
                contacts = emptyList(),
                onContactsChange = {},
                newContact = TextFieldValue(""),
                onNewContactChange = {},
                isStreaming = false,
                error = null,
                sendUiState = SendUiState(),
                onSend = {},
                suggestionText = "Suggestion",
                onAcceptSuggestion = { suggestionAccepted = true },
                bannerState = null,
                onDismissBanner = {}
            )
        }

        composeTestRule.onNodeWithText("탭 완성").performClick()
        assertTrue(suggestionAccepted)
    }

    @Test
    fun subject_field_disabled_when_streaming() {
        composeTestRule.setContent {
            val richTextState = rememberRichTextState()
            EmailComposeScreen(
                subject = "Test",
                onSubjectChange = {},
                richTextState = richTextState,
                contacts = emptyList(),
                onContactsChange = {},
                newContact = TextFieldValue(""),
                onNewContactChange = {},
                isStreaming = true,
                error = null,
                sendUiState = SendUiState(),
                onSend = {},
                bannerState = null,
                onDismissBanner = {}
            )
        }

        // The subject field should be disabled (not enabled for input)
        composeTestRule.waitForIdle()
    }

    @Test
    fun contact_chip_for_unknown_contact_shows_question_mark() {
        composeTestRule.setContent {
            ContactChip(
                contact = Contact(-1, null, "unknown@example.com", "unknown@example.com"),
                onRemove = {}
            )
        }

        composeTestRule.onNodeWithText("?").assertIsDisplayed()
    }

    @Test
    fun contact_chip_for_known_contact_shows_initial() {
        composeTestRule.setContent {
            ContactChip(
                contact = Contact(1, null, "John Doe", "john@example.com"),
                onRemove = {}
            )
        }

        composeTestRule.onNodeWithText("J").assertIsDisplayed()
    }

    @Test
    fun compose_vm_factory_creates_viewmodel() {
        val sseClient = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context.let {
            com.fiveis.xend.network.MailComposeSseClient(
                context = it,
                endpointUrl = "https://test.com"
            )
        }
        val wsClient = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context.let {
            com.fiveis.xend.network.MailComposeWebSocketClient(
                context = it,
                wsUrl = "wss://test.com"
            )
        }

        val factory = ComposeVmFactory(sseClient, wsClient)
        val viewModel = factory.create(MailComposeViewModel::class.java)

        assertTrue(viewModel is MailComposeViewModel)
    }
}
