package com.fiveis.xend.ui.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.Contact
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiPromptPreviewDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aiPromptPreviewDialog_displays_contacts() {
        // Given
        val contacts = listOf(
            Contact(id = 1, name = "Alice", email = "alice@test.com", group = null),
            Contact(id = 2, name = "Bob", email = "bob@test.com", group = null)
        )

        // When
        composeTestRule.setContent {
            AiPromptPreviewDialog(contacts = contacts, onDismiss = {})
        }

        // Then
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("alice@test.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
        composeTestRule.onNodeWithText("bob@test.com").assertIsDisplayed()
    }

    @Test
    fun aiPromptPreviewDialog_dismiss_callback() {
        // Given
        var dismissClicked = false
        val contacts = listOf(
            Contact(id = 1, name = "Alice", email = "alice@test.com", group = null)
        )

        // When
        composeTestRule.setContent {
            AiPromptPreviewDialog(contacts = contacts, onDismiss = { dismissClicked = true })
        }

        // Then
        composeTestRule.onNodeWithText("확인").performClick()
        assert(dismissClicked)
    }
    
    // Note: Testing the network loading state of AiPromptPreviewDialog is hard here because
    // it calls RetrofitClient internally within a LaunchedEffect.
    // To test loading/error/success states, we would need to mock the network response.
    // Since we cannot easily inject the network client into the Composable (it uses a Singleton),
    // we might skip the network state verification in this UI unit test and rely on integration tests
    // or manual verification, or use MockWebServer if we can configure the singleton to point to localhost.
}
