package com.fiveis.xend.ui.contactbook

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.ContactContext
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.model.PromptOption
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetailScreensTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ContactDetailScreen Tests (18 tests)

    @Test
    fun test_contactDetailScreen_displays_title() {
        // Given
        val contact = Contact(1, null, "John Doe", "john@example.com")
        val uiState = ContactDetailUiState(contact = contact)

        // When
        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("연락처 정보").assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_shows_back_button() {
        // Given
        val contact = Contact(1, null, "John", "john@example.com")
        val uiState = ContactDetailUiState(contact = contact)

        // When
        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {}
            )
        }

        // Then
        composeTestRule.onNodeWithContentDescription("뒤로가기").assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_back_click_triggers_callback() {
        // Given
        var backClicked = false
        val contact = Contact(1, null, "John", "john@example.com")
        val uiState = ContactDetailUiState(contact = contact)

        // When
        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = { backClicked = true },
                onRefresh = {},
                onOpenGroup = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("뒤로가기").performClick()

        // Then
        assert(backClicked)
    }

    @Test
    fun test_contactDetailScreen_displays_contact_name() {
        // Given
        val contact = Contact(1, null, "Jane Doe", "jane@example.com")
        val uiState = ContactDetailUiState(contact = contact)

        // When
        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Jane Doe").assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_displays_contact_email() {
        // Given
        val contact = Contact(1, null, "John", "john@example.com")
        val uiState = ContactDetailUiState(contact = contact)

        // When
        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("john@example.com").assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_null_contact_shows_error() {
        // Given
        val uiState = ContactDetailUiState(contact = null, error = "Not found")

        // When
        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Not found").assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_with_context_shows_relationship() {
        // Given
        val context = ContactContext(1, "학생", "교수")
        val contact = Contact(1, null, "Student", "student@example.com", context)
        val uiState = ContactDetailUiState(contact = contact)

        // When
        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("관계").assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_with_context_shows_personal_prompt_label() {
        // Given
        val context = ContactContext(1, "학생", "교수")
        val contact = Contact(1, null, "Student", "student@example.com", context)
        val uiState = ContactDetailUiState(contact = contact)

        // When
        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("개인 프롬프트").assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_with_group_shows_group_name() {
        // Given
        val group = Group(1, "VIP", "Important", emptyList(), emptyList(), null, null)
        val contact = Contact(1, group, "John", "john@example.com")
        val uiState = ContactDetailUiState(contact = contact)

        // When
        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("VIP").assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_group_click_triggers_callback() {
        // Given
        var clickedGroupId: Long = 0
        val group = Group(5, "Team", "Work", emptyList(), emptyList(), null, null)
        val contact = Contact(1, group, "John", "john@example.com")
        val uiState = ContactDetailUiState(contact = contact)

        // When
        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = { clickedGroupId = it }
            )
        }

        composeTestRule.onNodeWithText("Team").performClick()

        // Then
        assert(clickedGroupId == 5L)
    }

    @Test
    fun test_contactDetailScreen_renders_without_crash() {
        // Given
        val contact = Contact(1, null, "Test", "test@example.com")
        val uiState = ContactDetailUiState(contact = contact)

        // When
        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Test").assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_with_korean_name() {
        // Given
        val contact = Contact(1, null, "김철수", "kim@example.com")
        val uiState = ContactDetailUiState(contact = contact)

        // When
        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("김철수").assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_with_long_email() {
        // Given
        val longEmail = "verylongemailaddress@verylongdomain.example.com"
        val contact = Contact(1, null, "User", longEmail)
        val uiState = ContactDetailUiState(contact = contact)

        // When
        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText(longEmail).assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_null_contact_shows_loading_message() {
        // Given
        val uiState = ContactDetailUiState(contact = null)

        // When
        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("불러오는 중...").assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_group_with_description() {
        // Given
        val group = Group(1, "VIP", "Very Important People", emptyList(), emptyList(), null, null)
        val contact = Contact(1, group, "John", "john@example.com")
        val uiState = ContactDetailUiState(contact = contact)

        // When
        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Very Important People").assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_group_with_prompt_options() {
        // Given
        val options = listOf(PromptOption(1, "tone", "존댓말", ""))
        val group = Group(1, "Team", "Work", options, emptyList(), null, null)
        val contact = Contact(1, group, "John", "john@example.com")
        val uiState = ContactDetailUiState(contact = contact)

        // When
        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {}
            )
        }

        // Then - Should render without crash
        composeTestRule.onNodeWithText("John").assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_with_empty_email() {
        // Given
        val contact = Contact(1, null, "User", "")
        val uiState = ContactDetailUiState(contact = contact)

        // When
        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("User").assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_shows_group_section_label() {
        // Given
        val group = Group(1, "Team", null, emptyList(), emptyList(), null, null)
        val contact = Contact(1, group, "John", "john@example.com")
        val uiState = ContactDetailUiState(contact = contact)

        // When
        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("소속 그룹").assertIsDisplayed()
    }

    // GroupDetailScreen Tests (17 tests)

    @Test
    fun test_groupDetailScreen_displays_title() {
        // Given
        val group = Group(1, "VIP", "Important", emptyList(), emptyList(), null, null)
        val uiState = GroupDetailUiState(group = group)

        // When
        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRenameGroup = { _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("그룹 정보").assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_shows_back_button() {
        // Given
        val group = Group(1, "Team", "Work", emptyList(), emptyList(), null, null)
        val uiState = GroupDetailUiState(group = group)

        // When
        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRenameGroup = { _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        // Then
        composeTestRule.onNodeWithContentDescription("뒤로가기").assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_back_click_triggers_callback() {
        // Given
        var backClicked = false
        val group = Group(1, "Team", "Work", emptyList(), emptyList(), null, null)
        val uiState = GroupDetailUiState(group = group)

        // When
        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = { backClicked = true },
                onRefresh = {},
                onMemberClick = {},
                onRenameGroup = { _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("뒤로가기").performClick()

        // Then
        assert(backClicked)
    }

    @Test
    fun test_groupDetailScreen_displays_group_name() {
        // Given
        val group = Group(1, "VIP Team", "Important", emptyList(), emptyList(), null, null)
        val uiState = GroupDetailUiState(group = group)

        // When
        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRenameGroup = { _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("VIP Team").assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_displays_group_description() {
        // Given
        val group = Group(1, "Team", "Very important people", emptyList(), emptyList(), null, null)
        val uiState = GroupDetailUiState(group = group)

        // When
        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRenameGroup = { _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Very important people").assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_null_group_shows_error() {
        // Given
        val uiState = GroupDetailUiState(group = null, error = "Group not found")

        // When
        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRenameGroup = { _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Group not found").assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_shows_member_count() {
        // Given
        val members = listOf(
            Contact(1, null, "John", "john@example.com"),
            Contact(2, null, "Jane", "jane@example.com")
        )
        val group = Group(1, "Team", "Work", emptyList(), members, null, null)
        val uiState = GroupDetailUiState(group = group)

        // When
        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRenameGroup = { _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("멤버 2명").assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_displays_members() {
        // Given
        val members = listOf(
            Contact(1, null, "Alice", "alice@example.com")
        )
        val group = Group(1, "Team", "Work", emptyList(), members, null, null)
        val uiState = GroupDetailUiState(group = group)

        // When
        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRenameGroup = { _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_member_click_triggers_callback() {
        // Given
        var clickedContact: Contact? = null
        val member = Contact(1, null, "Bob", "bob@example.com")
        val group = Group(1, "Team", "Work", emptyList(), listOf(member), null, null)
        val uiState = GroupDetailUiState(group = group)

        // When
        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = { clickedContact = it },
                onRenameGroup = { _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        composeTestRule.onNodeWithText("Bob").performClick()

        // Then
        assert(clickedContact?.id == 1L)
    }

    @Test
    fun test_groupDetailScreen_shows_ai_prompt_section() {
        // Given
        val group = Group(1, "Team", "Work", emptyList(), emptyList(), null, null)
        val uiState = GroupDetailUiState(group = group)

        // When
        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRenameGroup = { _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("AI 프롬프트 설정").assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_with_tone_options() {
        // Given
        val options = listOf(PromptOption(1, "tone", "존댓말", ""))
        val group = Group(1, "Team", "Work", options, emptyList(), null, null)
        val uiState = GroupDetailUiState(group = group)

        // When
        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRenameGroup = { _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("문체 스타일").assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_with_format_options() {
        // Given
        val options = listOf(PromptOption(1, "format", "3~5문장", ""))
        val group = Group(1, "Team", "Work", options, emptyList(), null, null)
        val uiState = GroupDetailUiState(group = group)

        // When
        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRenameGroup = { _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("형식 가이드").assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_empty_prompt_options_shows_message() {
        // Given
        val group = Group(1, "Team", "Work", emptyList(), emptyList(), null, null)
        val uiState = GroupDetailUiState(group = group)

        // When
        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRenameGroup = { _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("설정된 프롬프트가 없습니다", substring = true).assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_null_group_shows_loading() {
        // Given
        val uiState = GroupDetailUiState(group = null)

        // When
        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRenameGroup = { _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("불러오는 중...").assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_renders_without_crash() {
        // Given
        val group = Group(1, "Test", "Desc", emptyList(), emptyList(), null, null)
        val uiState = GroupDetailUiState(group = group)

        // When
        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRenameGroup = { _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Test").assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_with_many_members() {
        // Given
        val members = List(10) { Contact(it.toLong(), null, "M$it", "m$it@example.com") }
        val group = Group(1, "Big Team", "Large", emptyList(), members, null, null)
        val uiState = GroupDetailUiState(group = group)

        // When
        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRenameGroup = { _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("멤버 10명").assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_with_korean_name() {
        // Given
        val group = Group(1, "중요 그룹", "설명", emptyList(), emptyList(), null, null)
        val uiState = GroupDetailUiState(group = group)

        // When
        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRenameGroup = { _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("중요 그룹").assertIsDisplayed()
    }
}
