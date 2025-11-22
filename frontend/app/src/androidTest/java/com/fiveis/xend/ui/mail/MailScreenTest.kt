package com.fiveis.xend.ui.mail

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.EmailItem
import com.fiveis.xend.ui.inbox.InboxUiState
import com.fiveis.xend.ui.sent.SentUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun test_mailScreen_shows_inbox_tab_by_default() {
        composeTestRule.setContent {
            MailScreen(
                inboxUiState = InboxUiState(),
                sentUiState = SentUiState(),
                onEmailClick = {},
                onAddContactClick = {},
                showDraftSavedBanner = false,
                onDismissDraftSavedBanner = {}
            )
        }

        composeTestRule.onNodeWithText("수신").assertIsDisplayed()
    }

    @Test
    fun test_mailScreen_shows_sent_tab() {
        composeTestRule.setContent {
            MailScreen(
                inboxUiState = InboxUiState(),
                sentUiState = SentUiState(),
                onEmailClick = {},
                onAddContactClick = {},
                showDraftSavedBanner = false,
                onDismissDraftSavedBanner = {}
            )
        }

        composeTestRule.onNodeWithText("발신").assertIsDisplayed()
    }

    @Test
    fun test_mailScreen_tab_switch() {
        composeTestRule.setContent {
            MailScreen(
                inboxUiState = InboxUiState(),
                sentUiState = SentUiState(),
                onEmailClick = {},
                onAddContactClick = {},
                showDraftSavedBanner = false,
                onDismissDraftSavedBanner = {}
            )
        }

        composeTestRule.onNodeWithText("발신").performClick()
        composeTestRule.onNodeWithText("수신").performClick()
    }

    @Test
    fun test_mailScreen_shows_search_button() {
        composeTestRule.setContent {
            MailScreen(
                inboxUiState = InboxUiState(),
                sentUiState = SentUiState(),
                onEmailClick = {},
                onAddContactClick = {},
                showDraftSavedBanner = false,
                onDismissDraftSavedBanner = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Search").assertIsDisplayed()
    }

    @Test
    fun test_mailScreen_search_button_click() {
        var searchClicked = false

        composeTestRule.setContent {
            MailScreen(
                inboxUiState = InboxUiState(),
                sentUiState = SentUiState(),
                onEmailClick = {},
                onAddContactClick = {},
                onOpenSearch = { searchClicked = true },
                showDraftSavedBanner = false,
                onDismissDraftSavedBanner = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Search").performClick()
        assert(searchClicked)
    }

    @Test
    fun test_mailScreen_shows_profile_button() {
        composeTestRule.setContent {
            MailScreen(
                inboxUiState = InboxUiState(),
                sentUiState = SentUiState(),
                onEmailClick = {},
                onAddContactClick = {},
                showDraftSavedBanner = false,
                onDismissDraftSavedBanner = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Profile").assertIsDisplayed()
    }

    @Test
    fun test_mailScreen_profile_button_click() {
        var profileClicked = false

        composeTestRule.setContent {
            MailScreen(
                inboxUiState = InboxUiState(),
                sentUiState = SentUiState(),
                onEmailClick = {},
                onAddContactClick = {},
                onOpenProfile = { profileClicked = true },
                showDraftSavedBanner = false,
                onDismissDraftSavedBanner = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Profile").performClick()
        assert(profileClicked)
    }

    @Test
    fun test_mailScreen_shows_fab() {
        composeTestRule.setContent {
            MailScreen(
                inboxUiState = InboxUiState(),
                sentUiState = SentUiState(),
                onEmailClick = {},
                onAddContactClick = {},
                showDraftSavedBanner = false,
                onDismissDraftSavedBanner = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("새 메일 작성").assertIsDisplayed()
    }

    @Test
    fun test_mailScreen_fab_click() {
        var fabClicked = false

        composeTestRule.setContent {
            MailScreen(
                inboxUiState = InboxUiState(),
                sentUiState = SentUiState(),
                onEmailClick = {},
                onAddContactClick = {},
                onFabClick = { fabClicked = true },
                showDraftSavedBanner = false,
                onDismissDraftSavedBanner = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("새 메일 작성").performClick()
        assert(fabClicked)
    }

    @Test
    fun test_mailScreen_shows_bottom_nav() {
        composeTestRule.setContent {
            MailScreen(
                inboxUiState = InboxUiState(),
                sentUiState = SentUiState(),
                onEmailClick = {},
                onAddContactClick = {},
                showDraftSavedBanner = false,
                onDismissDraftSavedBanner = {}
            )
        }

        composeTestRule.onNodeWithText("메일함").assertIsDisplayed()
        composeTestRule.onNodeWithText("연락처").assertIsDisplayed()
    }

    @Test
    fun test_mailScreen_bottom_nav_contacts_click() {
        var selectedNav = ""

        composeTestRule.setContent {
            MailScreen(
                inboxUiState = InboxUiState(),
                sentUiState = SentUiState(),
                onEmailClick = {},
                onAddContactClick = {},
                onBottomNavChange = { selectedNav = it },
                showDraftSavedBanner = false,
                onDismissDraftSavedBanner = {}
            )
        }

        composeTestRule.onNodeWithText("연락처").performClick()
        assert(selectedNav == "contacts")
    }

    @Test
    fun test_mailScreen_shows_success_banner() {
        composeTestRule.setContent {
            MailScreen(
                inboxUiState = InboxUiState(addContactSuccess = true),
                sentUiState = SentUiState(),
                onEmailClick = {},
                onAddContactClick = {},
                showDraftSavedBanner = false,
                onDismissDraftSavedBanner = {}
            )
        }

        composeTestRule.onNodeWithText("연락처가 추가되었습니다").assertIsDisplayed()
    }

    @Test
    fun test_mailScreen_shows_draft_saved_banner() {
        composeTestRule.setContent {
            MailScreen(
                inboxUiState = InboxUiState(),
                sentUiState = SentUiState(),
                onEmailClick = {},
                onAddContactClick = {},
                showDraftSavedBanner = true,
                onDismissDraftSavedBanner = {}
            )
        }

        composeTestRule.onNodeWithText("임시 저장되었습니다.").assertIsDisplayed()
    }

    @Test
    fun test_mailScreen_displays_inbox_emails() {
        val emails = listOf(
            EmailItem(
                id = "1",
                threadId = "thread1",
                subject = "Test Email 1",
                fromEmail = "sender@example.com",
                toEmail = "me@example.com",
                snippet = "Test snippet",
                date = "2024-01-01",
                dateRaw = "1704067200000",
                isUnread = true,
                labelIds = listOf("INBOX")
            ),
            EmailItem(
                id = "2",
                threadId = "thread2",
                subject = "Test Email 2",
                fromEmail = "sender2@example.com",
                toEmail = "me@example.com",
                snippet = "Test snippet 2",
                date = "2024-01-02",
                dateRaw = "1704153600000",
                isUnread = true,
                labelIds = listOf("INBOX")
            )
        )

        composeTestRule.setContent {
            MailScreen(
                inboxUiState = InboxUiState(emails = emails),
                sentUiState = SentUiState(),
                onEmailClick = {},
                onAddContactClick = {},
                showDraftSavedBanner = false,
                onDismissDraftSavedBanner = {}
            )
        }

        composeTestRule.onNodeWithText("Test Email 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Email 2").assertIsDisplayed()
    }

    @Test
    fun test_mailScreen_displays_sent_emails() {
        val emails = listOf(
            EmailItem(
                id = "1",
                threadId = "thread1",
                subject = "Sent Email 1",
                fromEmail = "me@example.com",
                toEmail = "recipient@example.com",
                snippet = "Sent snippet",
                date = "2024-01-01",
                dateRaw = "1704067200000",
                isUnread = false,
                labelIds = listOf("SENT")
            )
        )

        composeTestRule.setContent {
            MailScreen(
                inboxUiState = InboxUiState(),
                sentUiState = SentUiState(emails = emails),
                onEmailClick = {},
                onAddContactClick = {},
                showDraftSavedBanner = false,
                onDismissDraftSavedBanner = {}
            )
        }

        composeTestRule.onNodeWithText("발신").performClick()
        composeTestRule.onNodeWithText("Sent Email 1").assertIsDisplayed()
    }

    @Test
    fun test_mailScreen_inbox_email_click() {
        var clickedEmail: EmailItem? = null
        val email = EmailItem(
            id = "1",
            threadId = "thread1",
            subject = "Clickable Email",
            fromEmail = "sender@example.com",
            toEmail = "me@example.com",
            snippet = "Click me",
            date = "2024-01-01",
            dateRaw = "1704067200000",
            isUnread = true,
            labelIds = listOf("INBOX")
        )

        composeTestRule.setContent {
            MailScreen(
                inboxUiState = InboxUiState(
                    emails = listOf(email),
                    contactEmails = emptySet(),
                    contactsByEmail = emptyMap()
                ),
                sentUiState = SentUiState(),
                onEmailClick = { clickedEmail = it },
                onAddContactClick = {},
                showDraftSavedBanner = false,
                onDismissDraftSavedBanner = {}
            )
        }

        composeTestRule.waitForIdle()
        Thread.sleep(500)

        composeTestRule.onNodeWithText("Clickable Email", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        assert(clickedEmail != null) { "Email was not clicked" }
        assert(clickedEmail?.id == "1") { "Wrong email clicked: ${clickedEmail?.id}" }
    }

    @Test
    fun test_mailScreen_shows_empty_inbox() {
        composeTestRule.setContent {
            MailScreen(
                inboxUiState = InboxUiState(emails = emptyList()),
                sentUiState = SentUiState(),
                onEmailClick = {},
                onAddContactClick = {},
                showDraftSavedBanner = false,
                onDismissDraftSavedBanner = {}
            )
        }

        composeTestRule.onNodeWithText("수신").assertIsDisplayed()
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun test_mailScreen_shows_empty_sent() {
        composeTestRule.setContent {
            MailScreen(
                inboxUiState = InboxUiState(),
                sentUiState = SentUiState(emails = emptyList()),
                onEmailClick = {},
                onAddContactClick = {},
                showDraftSavedBanner = false,
                onDismissDraftSavedBanner = {}
            )
        }

        composeTestRule.onNodeWithText("발신").performClick()
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun test_mailScreen_inbox_loading_state() {
        composeTestRule.setContent {
            MailScreen(
                inboxUiState = InboxUiState(isLoading = true),
                sentUiState = SentUiState(),
                onEmailClick = {},
                onAddContactClick = {},
                showDraftSavedBanner = false,
                onDismissDraftSavedBanner = {}
            )
        }

        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun test_mailScreen_sent_loading_state() {
        composeTestRule.setContent {
            MailScreen(
                inboxUiState = InboxUiState(),
                sentUiState = SentUiState(isLoading = true),
                onEmailClick = {},
                onAddContactClick = {},
                showDraftSavedBanner = false,
                onDismissDraftSavedBanner = {}
            )
        }

        composeTestRule.onNodeWithText("발신").performClick()
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun test_mailScreen_inbox_error_state() {
        composeTestRule.setContent {
            MailScreen(
                inboxUiState = InboxUiState(error = "Network error"),
                sentUiState = SentUiState(),
                onEmailClick = {},
                onAddContactClick = {},
                showDraftSavedBanner = false,
                onDismissDraftSavedBanner = {}
            )
        }

        composeTestRule.onNodeWithText("Network error", substring = true).assertIsDisplayed()
    }

    @Test
    fun test_mailScreen_sent_error_state() {
        composeTestRule.setContent {
            MailScreen(
                inboxUiState = InboxUiState(),
                sentUiState = SentUiState(error = "Failed to load sent emails"),
                onEmailClick = {},
                onAddContactClick = {},
                showDraftSavedBanner = false,
                onDismissDraftSavedBanner = {}
            )
        }

        composeTestRule.onNodeWithText("발신").performClick()
        composeTestRule.onNodeWithText("Failed to load sent emails", substring = true).assertIsDisplayed()
    }
}
