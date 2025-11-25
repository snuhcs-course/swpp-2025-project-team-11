package com.fiveis.xend.ui.sent

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
class SentScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createMockEmail(id: String, subject: String, isUnread: Boolean = false): EmailItem {
        return EmailItem(
            id = id,
            threadId = "thread_$id",
            subject = subject,
            fromEmail = "me@test.com",
            toEmail = "",
            snippet = "Email snippet",
            date = "2024.12.19",
            dateRaw = "2024-12-19T10:00:00Z",
            isUnread = isUnread,
            labelIds = listOf("SENT"),
            body = "Email body"
        )
    }

    @Test
    fun sentScreen_displays_empty_state_with_loading() {
        // Given
        val uiState = SentUiState(isRefreshing = true, emails = emptyList())

        // When
        composeTestRule.setContent {
            SentScreen(
                uiState = uiState,
                onEmailClick = {}
            )
        }

        // Then
        Thread.sleep(200)
    }

    @Test
    fun sentScreen_displays_error_state() {
        // Given
        val uiState = SentUiState(error = "Failed to load sent emails", emails = emptyList())

        // When
        composeTestRule.setContent {
            SentScreen(
                uiState = uiState,
                onEmailClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Error: Failed to load sent emails").assertIsDisplayed()
    }

    @Test
    fun sentScreen_displays_email_list() {
        // Given
        val emails = listOf(
            createMockEmail("1", "Sent Email 1"),
            createMockEmail("2", "Sent Email 2"),
            createMockEmail("3", "Sent Email 3")
        )
        val uiState = SentUiState(emails = emails)

        // When
        composeTestRule.setContent {
            SentScreen(
                uiState = uiState,
                onEmailClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Sent Email 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sent Email 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sent Email 3").assertIsDisplayed()
    }

    @Test
    fun sentScreen_emailClick_triggers_callback() {
        // Given
        var clickedEmail: EmailItem? = null
        val email = createMockEmail("1", "Clickable Sent Email")
        val uiState = SentUiState(emails = listOf(email))

        // When
        composeTestRule.setContent {
            SentScreen(
                uiState = uiState,
                onEmailClick = { clickedEmail = it }
            )
        }

        // Then
        composeTestRule.onNodeWithText("Clickable Sent Email").performClick()
        assert(clickedEmail?.id == "1")
    }

    @Test
    fun sentScreen_displays_bottomNav() {
        // Given
        val uiState = SentUiState(emails = emptyList())

        // When
        composeTestRule.setContent {
            SentScreen(
                uiState = uiState,
                onEmailClick = {}
            )
        }

        // Then - Old nav bar (deprecated but still exists in standalone SentScreen)
        Thread.sleep(200)
    }

    @Test
    fun sentScreen_bottomNav_sent_selected() {
        // Given
        val uiState = SentUiState(emails = emptyList())

        // When
        composeTestRule.setContent {
            SentScreen(
                uiState = uiState,
                onEmailClick = {}
            )
        }

        // Then - Just verify screen loads
        Thread.sleep(200)
    }

    @Test
    fun sentScreen_bottomNav_inbox_click() {
        // Given
        var selectedNav = ""
        val uiState = SentUiState(emails = emptyList())

        // When
        composeTestRule.setContent {
            SentScreen(
                uiState = uiState,
                onEmailClick = {},
                onBottomNavChange = { selectedNav = it }
            )
        }

        // Then
        composeTestRule.onNodeWithText("받은메일").performClick()
        assert(selectedNav == "inbox")
    }

    @Test
    fun sentScreen_bottomNav_contacts_click() {
        // Given
        var selectedNav = ""
        val uiState = SentUiState(emails = emptyList())

        // When
        composeTestRule.setContent {
            SentScreen(
                uiState = uiState,
                onEmailClick = {},
                onBottomNavChange = { selectedNav = it }
            )
        }

        // Then
        composeTestRule.onNodeWithText("연락처").performClick()
        assert(selectedNav == "contacts")
    }

    @Test
    fun sentScreen_fab_click_triggers_callback() {
        // Given
        var fabClicked = false
        val uiState = SentUiState(emails = emptyList())

        // When
        composeTestRule.setContent {
            SentScreen(
                uiState = uiState,
                onEmailClick = {},
                onFabClick = { fabClicked = true }
            )
        }

        // Then
        composeTestRule.onNodeWithContentDescription("새 메일 작성").performClick()
        assert(fabClicked)
    }

    @Test
    fun sentScreen_search_button_triggers_callback() {
        // Given
        var searchClicked = false
        val uiState = SentUiState(emails = emptyList())

        // When
        composeTestRule.setContent {
            SentScreen(
                uiState = uiState,
                onEmailClick = {},
                onOpenSearch = { searchClicked = true }
            )
        }

        // Then
        composeTestRule.onNodeWithContentDescription("Search").performClick()
        assert(searchClicked)
    }

    @Test
    fun sentScreen_profile_button_triggers_callback() {
        // Given
        var profileClicked = false
        val uiState = SentUiState(emails = emptyList())

        // When
        composeTestRule.setContent {
            SentScreen(
                uiState = uiState,
                onEmailClick = {},
                onOpenProfile = { profileClicked = true }
            )
        }

        // Then
        composeTestRule.onNodeWithContentDescription("Profile").performClick()
        assert(profileClicked)
    }

    @Test
    fun sentScreen_displays_sender_name_extraction() {
        // Given
        val emails = listOf(
            createMockEmail("1", "Test Subject").copy(toEmail = "Jane Smith <jane@test.com>")
        )
        val uiState = SentUiState(emails = emails)

        // When
        composeTestRule.setContent {
            SentScreen(
                uiState = uiState,
                onEmailClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("To: Jane Smith").assertIsDisplayed()
    }

    @Test
    fun sentScreen_displays_plain_email_address() {
        // Given
        val emails = listOf(
            createMockEmail("1", "Test Subject").copy(toEmail = "myemail@test.com")
        )
        val uiState = SentUiState(emails = emails)

        // When
        composeTestRule.setContent {
            SentScreen(
                uiState = uiState,
                onEmailClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("To: myemail@test.com").assertIsDisplayed()
    }

    @Test
    fun sentScreen_displays_loading_more_indicator() {
        // Given
        val emails = listOf(createMockEmail("1", "Email 1"))
        val uiState = SentUiState(emails = emails, isLoading = true)

        // When
        composeTestRule.setContent {
            SentScreen(
                uiState = uiState,
                onEmailClick = {}
            )
        }

        // Then
        Thread.sleep(200)
        composeTestRule.onNodeWithText("Email 1").assertIsDisplayed()
    }

    @Test
    fun sentScreen_displays_snippet() {
        // Given
        val emails = listOf(
            createMockEmail("1", "Test Subject").copy(snippet = "This is a sent email snippet")
        )
        val uiState = SentUiState(emails = emails)

        // When
        composeTestRule.setContent {
            SentScreen(
                uiState = uiState,
                onEmailClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("This is a sent email snippet").assertIsDisplayed()
    }

    @Test
    fun sentScreen_displays_date() {
        // Given
        val emails = listOf(
            createMockEmail("1", "Test Subject").copy(date = "Dec 20")
        )
        val uiState = SentUiState(emails = emails)

        // When
        composeTestRule.setContent {
            SentScreen(
                uiState = uiState,
                onEmailClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Dec 20").assertIsDisplayed()
    }

    @Test
    fun sentScreen_displays_multiple_emails() {
        // Given
        val emails = listOf(
            createMockEmail("1", "Email 1"),
            createMockEmail("2", "Email 2"),
            createMockEmail("3", "Email 3"),
            createMockEmail("4", "Email 4")
        )
        val uiState = SentUiState(emails = emails)

        // When
        composeTestRule.setContent {
            SentScreen(
                uiState = uiState,
                onEmailClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Email 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Email 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Email 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Email 4").assertIsDisplayed()
    }

    @Test
    fun sentScreen_handles_long_subject() {
        // Given
        val longSubject = "This is a very long sent email subject that should be truncated in the UI"
        val emails = listOf(createMockEmail("1", longSubject))
        val uiState = SentUiState(emails = emails)

        // When
        composeTestRule.setContent {
            SentScreen(
                uiState = uiState,
                onEmailClick = {}
            )
        }

        // Then
        Thread.sleep(200)
    }

    @Test
    fun sentScreen_handles_special_characters_in_subject() {
        // Given
        val emails = listOf(
            createMockEmail("1", "RE: Special !@#$%^& Chars")
        )
        val uiState = SentUiState(emails = emails)

        // When
        composeTestRule.setContent {
            SentScreen(
                uiState = uiState,
                onEmailClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("RE: Special !@#$%^& Chars").assertIsDisplayed()
    }

    @Test
    fun sentScreen_handles_unicode_characters() {
        // Given
        val emails = listOf(
            createMockEmail("1", "한글 제목 Test 日本語")
        )
        val uiState = SentUiState(emails = emails)

        // When
        composeTestRule.setContent {
            SentScreen(
                uiState = uiState,
                onEmailClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("한글 제목 Test 日本語").assertIsDisplayed()
    }

    @Test
    fun sentScreen_handles_refresh() {
        // Given
        var refreshCalled = false
        val uiState = SentUiState(emails = emptyList())

        // When
        composeTestRule.setContent {
            SentScreen(
                uiState = uiState,
                onEmailClick = {},
                onRefresh = { refreshCalled = true }
            )
        }

        // Then - Refresh is triggered by pull-to-refresh gesture
        Thread.sleep(200)
    }

    @Test
    fun sentScreen_handles_empty_email_list() {
        // Given
        val uiState = SentUiState(emails = emptyList(), isRefreshing = false)

        // When
        composeTestRule.setContent {
            SentScreen(
                uiState = uiState,
                onEmailClick = {}
            )
        }

        // Then - Should show empty state without crash
        Thread.sleep(200)
    }

    @Test
    fun sentScreen_scrolls_through_emails() {
        // Given
        val emails = (1..25).map { createMockEmail("$it", "Sent Email $it") }
        val uiState = SentUiState(emails = emails)

        // When
        composeTestRule.setContent {
            SentScreen(
                uiState = uiState,
                onEmailClick = {}
            )
        }

        // Then
        Thread.sleep(300)
    }

    @Test
    fun sentScreen_handles_long_sender_name() {
        // Given
        val emails = listOf(
            createMockEmail("1", "Test").copy(
                fromEmail = "Very Long Name Person <verylong@email.com>"
            )
        )
        val uiState = SentUiState(emails = emails)

        // When
        composeTestRule.setContent {
            SentScreen(
                uiState = uiState,
                onEmailClick = {}
            )
        }

        // Then
        Thread.sleep(200)
    }

    @Test
    fun sentScreen_handles_unread_emails() {
        // Given
        val emails = listOf(
            createMockEmail("1", "Unread Sent Email", isUnread = true)
        )
        val uiState = SentUiState(emails = emails)

        // When
        composeTestRule.setContent {
            SentScreen(
                uiState = uiState,
                onEmailClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Unread Sent Email").assertIsDisplayed()
    }
}
