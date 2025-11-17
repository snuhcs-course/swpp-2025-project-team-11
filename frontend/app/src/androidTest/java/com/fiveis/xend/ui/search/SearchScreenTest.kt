package com.fiveis.xend.ui.search

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.EmailItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createMockEmail(id: String, subject: String, from: String = "test@test.com"): EmailItem {
        return EmailItem(
            id = id,
            threadId = "thread_$id",
            subject = subject,
            fromEmail = from,
            snippet = "Email snippet",
            date = "2024.12.19",
            dateRaw = "2024-12-19T10:00:00Z",
            isUnread = false,
            labelIds = listOf("INBOX"),
            body = "Email body"
        )
    }

    @Test
    fun searchScreen_displays_emptyState() {
        // When
        composeTestRule.setContent {
            SearchScreen(
                searchQuery = "",
                searchResults = emptyList(),
                onQueryChange = {},
                onEmailClick = {},
                onBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("메일 제목이나 보낸 사람으로 검색하세요").assertIsDisplayed()
    }

    @Test
    fun searchScreen_displays_noResults() {
        // When
        composeTestRule.setContent {
            SearchScreen(
                searchQuery = "nonexistent",
                searchResults = emptyList(),
                onQueryChange = {},
                onEmailClick = {},
                onBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("검색 결과가 없습니다").assertIsDisplayed()
        composeTestRule.onNodeWithText("\"nonexistent\"에 대한 메일을 찾을 수 없습니다").assertIsDisplayed()
    }

    @Test
    fun searchScreen_displays_searchBar() {
        // When
        composeTestRule.setContent {
            SearchScreen(
                searchQuery = "",
                searchResults = emptyList(),
                onQueryChange = {},
                onEmailClick = {},
                onBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("메일 검색...").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("뒤로가기").assertIsDisplayed()
    }

    @Test
    fun searchScreen_backButton_triggers_callback() {
        // Given
        var backClicked = false

        // When
        composeTestRule.setContent {
            SearchScreen(
                searchQuery = "",
                searchResults = emptyList(),
                onQueryChange = {},
                onEmailClick = {},
                onBack = { backClicked = true }
            )
        }

        // Then
        composeTestRule.onNodeWithContentDescription("뒤로가기").performClick()
        assert(backClicked)
    }

    @Test
    fun searchScreen_displays_clearButton_whenQueryNotEmpty() {
        // When
        composeTestRule.setContent {
            SearchScreen(
                searchQuery = "test query",
                searchResults = emptyList(),
                onQueryChange = {},
                onEmailClick = {},
                onBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithContentDescription("검색어 지우기").assertIsDisplayed()
    }

    @Test
    fun searchScreen_clearButton_triggers_callback() {
        // Given
        var clearedQuery = ""

        // When
        composeTestRule.setContent {
            SearchScreen(
                searchQuery = "test query",
                searchResults = emptyList(),
                onQueryChange = { clearedQuery = it },
                onEmailClick = {},
                onBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithContentDescription("검색어 지우기").performClick()
        assert(clearedQuery == "")
    }

    @Test
    fun searchScreen_displays_searchResults() {
        // Given
        val results = listOf(
            createMockEmail("1", "Result 1"),
            createMockEmail("2", "Result 2"),
            createMockEmail("3", "Result 3")
        )

        // When
        composeTestRule.setContent {
            SearchScreen(
                searchQuery = "test",
                searchResults = results,
                onQueryChange = {},
                onEmailClick = {},
                onBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Result 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Result 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Result 3").assertIsDisplayed()
    }

    @Test
    fun searchScreen_emailClick_triggers_callback() {
        // Given
        var clickedEmail: EmailItem? = null
        val email = createMockEmail("1", "Clickable Email")

        // When
        composeTestRule.setContent {
            SearchScreen(
                searchQuery = "test",
                searchResults = listOf(email),
                onQueryChange = {},
                onEmailClick = { clickedEmail = it },
                onBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Clickable Email").performClick()
        assert(clickedEmail?.id == "1")
    }

    @Test
    fun searchScreen_displays_unreadIndicator() {
        // Given
        val results = listOf(
            createMockEmail("1", "Unread Email").copy(isUnread = true)
        )

        // When
        composeTestRule.setContent {
            SearchScreen(
                searchQuery = "test",
                searchResults = results,
                onQueryChange = {},
                onEmailClick = {},
                onBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Unread Email").assertIsDisplayed()
    }

    @Test
    fun searchScreen_displays_fromEmail() {
        // Given
        val results = listOf(
            createMockEmail("1", "Test", "sender@example.com")
        )

        // When
        composeTestRule.setContent {
            SearchScreen(
                searchQuery = "test",
                searchResults = results,
                onQueryChange = {},
                onEmailClick = {},
                onBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("sender@example.com").assertIsDisplayed()
    }

    @Test
    fun searchScreen_displays_snippet() {
        // Given
        val results = listOf(
            createMockEmail("1", "Test").copy(snippet = "Custom snippet text")
        )

        // When
        composeTestRule.setContent {
            SearchScreen(
                searchQuery = "test",
                searchResults = results,
                onQueryChange = {},
                onEmailClick = {},
                onBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Custom snippet text").assertIsDisplayed()
    }

    @Test
    fun searchScreen_handles_multipleResults() {
        // Given
        val results = (1..10).map { createMockEmail("$it", "Email $it") }

        // When
        composeTestRule.setContent {
            SearchScreen(
                searchQuery = "test",
                searchResults = results,
                onQueryChange = {},
                onEmailClick = {},
                onBack = {}
            )
        }

        // Then
        Thread.sleep(200)
        composeTestRule.onNodeWithText("Email 1").assertIsDisplayed()
    }

    @Test
    fun searchScreen_handles_specialCharacters() {
        // Given
        val results = listOf(
            createMockEmail("1", "Special !@#$%^& Characters")
        )

        // When
        composeTestRule.setContent {
            SearchScreen(
                searchQuery = "special",
                searchResults = results,
                onQueryChange = {},
                onEmailClick = {},
                onBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Special !@#$%^& Characters").assertIsDisplayed()
    }

    @Test
    fun searchScreen_handles_unicodeCharacters() {
        // Given
        val results = listOf(
            createMockEmail("1", "한글 제목 Test 日本語")
        )

        // When
        composeTestRule.setContent {
            SearchScreen(
                searchQuery = "한글",
                searchResults = results,
                onQueryChange = {},
                onEmailClick = {},
                onBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("한글 제목 Test 日本語").assertIsDisplayed()
    }

    @Test
    fun searchScreen_handles_longSubject() {
        // Given
        val longSubject = "This is a very long email subject that should be truncated in the UI"
        val results = listOf(createMockEmail("1", longSubject))

        // When
        composeTestRule.setContent {
            SearchScreen(
                searchQuery = "long",
                searchResults = results,
                onQueryChange = {},
                onEmailClick = {},
                onBack = {}
            )
        }

        // Then
        Thread.sleep(200)
    }
}
