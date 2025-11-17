package com.fiveis.xend.ui.contactbook

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.Contact
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactSelectDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun test_contactSelectDialog_displays_title() {
        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = emptyList(),
                selectedContacts = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        composeTestRule.onNodeWithText("연락처 선택").assertIsDisplayed()
    }

    @Test
    fun test_contactSelectDialog_displays_description() {
        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = emptyList(),
                selectedContacts = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        composeTestRule.onNodeWithText("그룹에 추가할 연락처를 선택하세요").assertIsDisplayed()
    }

    @Test
    fun test_contactSelectDialog_shows_empty_message_when_no_contacts() {
        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = emptyList(),
                selectedContacts = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        composeTestRule.onNodeWithText("등록된 연락처가 없습니다").assertIsDisplayed()
    }

    @Test
    fun test_contactSelectDialog_displays_contacts() {
        val contacts = listOf(
            Contact(1, null, "Alice", "alice@example.com"),
            Contact(2, null, "Bob", "bob@example.com")
        )

        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = contacts,
                selectedContacts = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("alice@example.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
        composeTestRule.onNodeWithText("bob@example.com").assertIsDisplayed()
    }

    @Test
    fun test_contactSelectDialog_shows_cancel_button() {
        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = emptyList(),
                selectedContacts = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        composeTestRule.onNodeWithText("취소").assertIsDisplayed()
    }

    @Test
    fun test_contactSelectDialog_shows_confirm_button() {
        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = emptyList(),
                selectedContacts = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        composeTestRule.onNodeWithText("확인 (0)").assertIsDisplayed()
    }

    @Test
    fun test_contactSelectDialog_cancel_triggers_callback() {
        var dismissed = false

        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = emptyList(),
                selectedContacts = emptyList(),
                onDismiss = { dismissed = true },
                onConfirm = {}
            )
        }

        composeTestRule.onNodeWithText("취소").performClick()

        assert(dismissed)
    }

    @Test
    fun test_contactSelectDialog_confirm_triggers_callback() {
        var confirmed = false

        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = emptyList(),
                selectedContacts = emptyList(),
                onDismiss = {},
                onConfirm = { confirmed = true }
            )
        }

        composeTestRule.onNodeWithText("확인 (0)").performClick()

        assert(confirmed)
    }

    @Test
    fun test_contactSelectDialog_contact_click_selects() {
        val contacts = listOf(Contact(1, null, "Alice", "alice@example.com"))

        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = contacts,
                selectedContacts = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        composeTestRule.onNodeWithText("Alice").performClick()
        composeTestRule.onNodeWithText("확인 (1)").assertIsDisplayed()
    }

    @Test
    fun test_contactSelectDialog_contact_click_deselects() {
        val contacts = listOf(Contact(1, null, "Alice", "alice@example.com"))

        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = contacts,
                selectedContacts = contacts,
                onDismiss = {},
                onConfirm = {}
            )
        }

        composeTestRule.onNodeWithText("확인 (1)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice").performClick()
        composeTestRule.onNodeWithText("확인 (0)").assertIsDisplayed()
    }

    @Test
    fun test_contactSelectDialog_multiple_selections() {
        val contacts = listOf(
            Contact(1, null, "Alice", "alice@example.com"),
            Contact(2, null, "Bob", "bob@example.com"),
            Contact(3, null, "Charlie", "charlie@example.com")
        )

        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = contacts,
                selectedContacts = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        composeTestRule.onNodeWithText("Alice").performClick()
        composeTestRule.onNodeWithText("Bob").performClick()
        composeTestRule.onNodeWithText("확인 (2)").assertIsDisplayed()
    }

    @Test
    fun test_contactSelectDialog_preselected_contacts() {
        val contacts = listOf(
            Contact(1, null, "Alice", "alice@example.com"),
            Contact(2, null, "Bob", "bob@example.com")
        )
        val selected = listOf(contacts[0])

        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = contacts,
                selectedContacts = selected,
                onDismiss = {},
                onConfirm = {}
            )
        }

        composeTestRule.onNodeWithText("확인 (1)").assertIsDisplayed()
    }

    @Test
    fun test_contactSelectDialog_displays_check_icon_for_selected() {
        val contacts = listOf(Contact(1, null, "Alice", "alice@example.com"))

        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = contacts,
                selectedContacts = contacts,
                onDismiss = {},
                onConfirm = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("선택됨").assertIsDisplayed()
    }

    @Test
    fun test_contactSelectDialog_contact_initial_avatar() {
        val contacts = listOf(Contact(1, null, "Alice", "alice@example.com"))

        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = contacts,
                selectedContacts = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        composeTestRule.onNodeWithText("A").assertIsDisplayed()
    }

    @Test
    fun test_contactSelectDialog_returns_selected_contacts_on_confirm() {
        val contacts = listOf(
            Contact(1, null, "Alice", "alice@example.com"),
            Contact(2, null, "Bob", "bob@example.com")
        )
        var confirmedContacts = emptyList<Contact>()

        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = contacts,
                selectedContacts = emptyList(),
                onDismiss = {},
                onConfirm = { confirmedContacts = it }
            )
        }

        composeTestRule.onNodeWithText("Alice").performClick()
        composeTestRule.onNodeWithText("확인 (1)").performClick()

        assert(confirmedContacts.size == 1)
        assert(confirmedContacts[0].name == "Alice")
    }

    @Test
    fun test_contactSelectDialog_with_many_contacts() {
        val contacts = List(20) { Contact(it.toLong(), null, "Contact$it", "c$it@example.com") }

        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = contacts,
                selectedContacts = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        composeTestRule.onNodeWithText("Contact0").assertIsDisplayed()
    }

    @Test
    fun test_contactSelectDialog_select_all_contacts() {
        val contacts = listOf(
            Contact(1, null, "Alice", "alice@example.com"),
            Contact(2, null, "Bob", "bob@example.com"),
            Contact(3, null, "Charlie", "charlie@example.com")
        )

        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = contacts,
                selectedContacts = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        composeTestRule.onNodeWithText("Alice").performClick()
        composeTestRule.onNodeWithText("Bob").performClick()
        composeTestRule.onNodeWithText("Charlie").performClick()
        composeTestRule.onNodeWithText("확인 (3)").assertIsDisplayed()
    }

    @Test
    fun test_contactSelectDialog_single_contact() {
        val contacts = listOf(Contact(1, null, "Solo", "solo@example.com"))

        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = contacts,
                selectedContacts = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        composeTestRule.onNodeWithText("Solo").assertIsDisplayed()
        composeTestRule.onNodeWithText("solo@example.com").assertIsDisplayed()
    }

    @Test
    fun test_contactSelectDialog_toggle_selection_multiple_times() {
        val contacts = listOf(Contact(1, null, "Alice", "alice@example.com"))

        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = contacts,
                selectedContacts = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        composeTestRule.onNodeWithText("Alice").performClick()
        composeTestRule.onNodeWithText("확인 (1)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice").performClick()
        composeTestRule.onNodeWithText("확인 (0)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice").performClick()
        composeTestRule.onNodeWithText("확인 (1)").assertIsDisplayed()
    }

    @Test
    fun test_contactSelectDialog_korean_names() {
        val contacts = listOf(
            Contact(1, null, "김철수", "kim@example.com"),
            Contact(2, null, "이영희", "lee@example.com")
        )

        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = contacts,
                selectedContacts = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        composeTestRule.onNodeWithText("김철수").assertIsDisplayed()
        composeTestRule.onNodeWithText("이영희").assertIsDisplayed()
    }

    @Test
    fun test_contactSelectDialog_long_names() {
        val contacts = listOf(
            Contact(1, null, "Very Long Contact Name That Exceeds Normal Expectations", "long@example.com")
        )

        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = contacts,
                selectedContacts = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        composeTestRule.onNodeWithText("Very Long Contact Name That Exceeds Normal Expectations").assertExists()
    }

    @Test
    fun test_contactSelectDialog_special_characters_in_email() {
        val contacts = listOf(
            Contact(1, null, "Test", "test+special@example.com")
        )

        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = contacts,
                selectedContacts = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        composeTestRule.onNodeWithText("test+special@example.com").assertIsDisplayed()
    }

    @Test
    fun test_contactSelectDialog_empty_name_shows_question_mark() {
        val contacts = listOf(Contact(1, null, "", "test@example.com"))

        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = contacts,
                selectedContacts = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        composeTestRule.onNodeWithText("?").assertIsDisplayed()
    }
}
