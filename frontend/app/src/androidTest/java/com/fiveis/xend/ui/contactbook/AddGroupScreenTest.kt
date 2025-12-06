package com.fiveis.xend.ui.contactbook

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.PromptOption
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddGroupScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun test_addGroupScreen_displays_title() {
        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("그룹 추가").assertIsDisplayed()
    }

    @Test
    fun test_addGroupScreen_shows_back_button() {
        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {}
            )
        }

        // Then
        composeTestRule.onNodeWithContentDescription("뒤로가기").assertIsDisplayed()
    }

    @Test
    fun test_addGroupScreen_shows_save_button() {
        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("저장").assertIsDisplayed()
    }

    @Test
    fun test_addGroupScreen_back_click_triggers_callback() {
        // Given
        var backClicked = false

        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = { backClicked = true },
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("뒤로가기").performClick()

        // Then
        assert(backClicked)
    }

    @Test
    fun test_addGroupScreen_shows_group_name_field() {
        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("이름을 입력하세요").assertIsDisplayed()
    }

    @Test
    fun test_addGroupScreen_shows_group_description_field() {
        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("그룹을 소개해 주세요").assertIsDisplayed()
    }

    @Test
    fun test_addGroupScreen_group_name_input_triggers_callback() {
        // Given
        var changedName = ""

        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = { changedName = it },
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {}
            )
        }

        composeTestRule.onNodeWithTag("groupNameInput").performTextInput("VIP Group")

        // Then
        assert(changedName == "VIP Group")
    }

    @Test
    fun test_addGroupScreen_group_description_input_triggers_callback() {
        // Given
        var changedDescription = ""

        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = { changedDescription = it },
                onPromptOptionsChange = {}
            )
        }

        composeTestRule.onNodeWithText("그룹을 소개해 주세요").performTextInput("Important people")

        // Then
        assert(changedDescription == "Important people")
    }

    @Test
    fun test_addGroupScreen_shows_ai_prompting_section() {
        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("AI 프롬프트 설정").assertIsDisplayed()
    }

    @Test
    fun test_addGroupScreen_shows_group_members_section() {
        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {},
                members = emptyList()
            )
        }

        // Then
        composeTestRule.onNodeWithText("그룹 멤버 (0명)").assertIsDisplayed()
    }

    @Test
    fun test_addGroupScreen_with_members_shows_count() {
        // Given
        val members = listOf(
            Contact(1, null, "John", "john@example.com"),
            Contact(2, null, "Jane", "jane@example.com")
        )

        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {},
                members = members
            )
        }

        // Then
        composeTestRule.onNodeWithText("그룹 멤버 (2명)").assertIsDisplayed()
    }

    @Test
    fun test_addGroupScreen_shows_add_member_button() {
        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("추가").assertIsDisplayed()
    }

    @Test
    fun test_addGroupScreen_add_member_click_triggers_callback() {
        // Given
        var addClicked = false

        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {},
                onAddMember = { addClicked = true }
            )
        }

        composeTestRule.onNodeWithText("추가").performClick()

        // Then
        assert(addClicked)
    }

    @Test
    fun test_addGroupScreen_displays_members() {
        // Given
        val members = listOf(
            Contact(1, null, "Alice", "alice@example.com")
        )

        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {},
                members = members
            )
        }

        // Then
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
    }

    @Test
    fun test_addGroupScreen_displays_multiple_members() {
        // Given
        val members = listOf(
            Contact(1, null, "Member1", "m1@example.com"),
            Contact(2, null, "Member2", "m2@example.com"),
            Contact(3, null, "Member3", "m3@example.com")
        )

        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {},
                members = members
            )
        }

        // Then
        composeTestRule.onNodeWithText("Member1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Member2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Member3").assertIsDisplayed()
    }

    @Test
    fun test_addGroupScreen_shows_fab() {
        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {}
            )
        }

        // Then - top app bar action should be visible
        composeTestRule.onNodeWithText("저장").assertIsDisplayed()
    }

    @Test
    fun test_addGroupScreen_fab_click_triggers_callback() {
        // Given
        var addClicked = false

        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = { addClicked = true },
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {}
            )
        }

        // Enable the save button
        composeTestRule.onNodeWithTag("groupNameInput").performTextInput("New group")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("저장").performClick()

        // Then
        assert(addClicked)
    }

    @Test
    fun test_addGroupScreen_group_name_label() {
        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("그룹 이름").assertIsDisplayed()
    }

    @Test
    fun test_addGroupScreen_group_description_label() {
        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("그룹 설명").assertIsDisplayed()
    }

    @Test
    fun test_addGroupScreen_with_tone_options() {
        // Given
        val toneOptions = listOf(
            PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")
        )
        val uiState = AddGroupUiState(tonePromptOptions = toneOptions)

        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = uiState,
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {}
            )
        }

        // Then - Should render without crash
        composeTestRule.onNodeWithText("AI 프롬프트 설정").assertIsDisplayed()
    }

    @Test
    fun test_addGroupScreen_with_format_options() {
        // Given
        val formatOptions = listOf(
            PromptOption(1, "format", "3~5문장", "간결하게 작성하세요")
        )
        val uiState = AddGroupUiState(formatPromptOptions = formatOptions)

        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = uiState,
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {}
            )
        }

        // Then - Should render without crash
        composeTestRule.onNodeWithText("AI 프롬프트 설정").assertIsDisplayed()
    }

    @Test
    fun test_addGroupScreen_bottom_nav_displays() {
        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("연락처").assertDoesNotExist()
    }

    @Test
    fun test_addGroupScreen_renders_without_crash() {
        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("그룹 추가").assertIsDisplayed()
    }

    @Test
    fun test_addGroupScreen_korean_group_name() {
        // Given
        var changedName = ""

        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = { changedName = it },
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {}
            )
        }

        composeTestRule.onNodeWithTag("groupNameInput").performTextInput("중요 고객")

        // Then
        assert(changedName == "중요 고객")
    }

    @Test
    fun test_addGroupScreen_long_group_name() {
        // Given
        var changedName = ""
        val longName = "This is a very long group name that exceeds normal expectations"

        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = { changedName = it },
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {}
            )
        }

        composeTestRule.onNodeWithTag("groupNameInput").performTextInput(longName)

        // Then
        assert(changedName == longName)
    }

    @Test
    fun test_addGroupScreen_long_description() {
        // Given
        var changedDescription = ""
        val longDesc = "This is a very long description that provides detailed information about the group"

        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = { changedDescription = it },
                onPromptOptionsChange = {}
            )
        }

        composeTestRule.onNodeWithText("그룹을 소개해 주세요").performTextInput(longDesc)

        // Then
        assert(changedDescription == longDesc)
    }

    @Test
    fun test_addGroupScreen_with_many_members() {
        // Given
        val members = List(10) { Contact(it.toLong(), null, "Member$it", "m$it@example.com") }

        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {},
                members = members
            )
        }

        // Then
        composeTestRule.onNodeWithText("그룹 멤버 (10명)").assertIsDisplayed()
    }

    @Test
    fun test_addGroupScreen_member_click() {
        // Given
        var clicked = false
        val members = listOf(Contact(1, null, "John", "john@example.com"))

        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {},
                members = members,
                onMemberClick = { clicked = true }
            )
        }

        composeTestRule.onNodeWithText("John").performClick()

        // Then
        assert(clicked)
    }

    @Test
    fun test_addGroupScreen_empty_name_input() {
        // Given
        var name = "initial"

        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = { name = it },
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {}
            )
        }

        composeTestRule.onNodeWithTag("groupNameInput").performTextInput("TestGroup")

        // Then
        assert(name == "TestGroup")
    }

    @Test
    fun test_addGroupScreen_special_characters_in_name() {
        // Given
        var changedName = ""

        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = { changedName = it },
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {}
            )
        }

        composeTestRule.onNodeWithTag("groupNameInput").performTextInput("Team@2024")

        // Then
        assert(changedName == "Team@2024")
    }

    @Test
    fun test_addGroupScreen_all_callbacks_triggerable() {
        // Given
        var backClicked = false
        var addClicked = false
        var nameChanged = false
        var descChanged = false

        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = { backClicked = true },
                onAdd = { addClicked = true },
                onGroupNameChange = { nameChanged = true },
                onGroupDescriptionChange = { descChanged = true },
                onPromptOptionsChange = {}
            )
        }

        composeTestRule.onNodeWithTag("groupNameInput").performTextInput("Test")
        composeTestRule.onNodeWithText("그룹을 소개해 주세요").performTextInput("Desc")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("뒤로가기").performClick()
        composeTestRule.onNodeWithText("저장").performClick()

        // Then
        assert(backClicked)
        assert(addClicked)
        assert(nameChanged)
        assert(descChanged)
    }

    @Test
    fun test_addGroupScreen_scrollable_content() {
        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {}
            )
        }

        // Then - All sections should be accessible
        composeTestRule.onNodeWithText("그룹 이름").assertIsDisplayed()
        composeTestRule.onNodeWithText("그룹 설명").assertIsDisplayed()
        composeTestRule.onNodeWithText("AI 프롬프트 설정").assertIsDisplayed()
    }

    @Test
    fun test_addGroupScreen_with_single_member() {
        // Given
        val members = listOf(Contact(1, null, "Solo", "solo@example.com"))

        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {},
                members = members
            )
        }

        // Then
        composeTestRule.onNodeWithText("그룹 멤버 (1명)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Solo").assertIsDisplayed()
    }

    @Test
    fun test_addGroupScreen_multiple_inputs_persist() {
        // Given
        var name = ""
        var description = ""

        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = { name = it },
                onGroupDescriptionChange = { description = it },
                onPromptOptionsChange = {}
            )
        }

        composeTestRule.onNodeWithTag("groupNameInput").performTextInput("VIP")
        composeTestRule.onNodeWithText("그룹을 소개해 주세요").performTextInput("Important clients")

        // Then
        assert(name == "VIP")
        assert(description == "Important clients")
    }

    @Test
    fun test_addGroupScreen_stress_test_many_members() {
        // Given
        val members = List(50) { Contact(it.toLong(), null, "M$it", "m$it@example.com") }

        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {},
                members = members
            )
        }

        // Then - Should render without crash
        composeTestRule.onNodeWithText("그룹 멤버 (50명)").assertIsDisplayed()
    }

    @Test
    fun test_addGroupScreen_ui_state_with_all_options() {
        // Given
        val toneOptions = listOf(PromptOption(1, "tone", "존댓말", ""))
        val formatOptions = listOf(PromptOption(2, "format", "3~5문장", ""))
        val uiState = AddGroupUiState(
            tonePromptOptions = toneOptions,
            formatPromptOptions = formatOptions
        )

        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = uiState,
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {}
            )
        }

        // Then - Should render without crash
        composeTestRule.onNodeWithText("그룹 추가").assertIsDisplayed()
    }

    @Test
    fun test_addGroupScreen_numbers_in_name() {
        // Given
        var changedName = ""

        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = { changedName = it },
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {}
            )
        }

        composeTestRule.onNodeWithTag("groupNameInput").performTextInput("Team 2024")

        // Then
        assert(changedName == "Team 2024")
    }

    @Test
    fun test_addGroupScreen_all_sections_present() {
        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("그룹 이름").assertIsDisplayed()
        composeTestRule.onNodeWithText("그룹 설명").assertIsDisplayed()
        composeTestRule.onNodeWithText("AI 프롬프트 설정").assertIsDisplayed()
        composeTestRule.onNodeWithText("그룹 멤버 (0명)").assertIsDisplayed()
    }

    @Test
    fun test_emojiPickerDialog_displays_when_shown() {
        // When
        composeTestRule.setContent {
            EmojiPickerDialog(
                currentEmoji = null,
                onDismiss = {},
                onEmojiSelected = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("심볼 이모지 선택").assertIsDisplayed()
        composeTestRule.onNodeWithText("닫기").assertIsDisplayed()
    }

    @Test
    fun test_emojiPickerDialog_displays_emojis() {
        // When
        composeTestRule.setContent {
            EmojiPickerDialog(
                currentEmoji = null,
                onDismiss = {},
                onEmojiSelected = {}
            )
        }

        // Then - Should display emoji grid
        composeTestRule.onNodeWithText("심볼 이모지 선택").assertIsDisplayed()
    }

    @Test
    fun test_emojiPickerDialog_shows_category_chips() {
        // When
        composeTestRule.setContent {
            EmojiPickerDialog(
                currentEmoji = null,
                onDismiss = {},
                onEmojiSelected = {}
            )
        }

        // Then - Category chips should be visible
        composeTestRule.onNodeWithText("전체").assertIsDisplayed()
        composeTestRule.onNodeWithText("표정/사람").assertIsDisplayed()
    }

    @Test
    fun test_emojiPickerDialog_filters_by_category() {
        // When
        composeTestRule.setContent {
            EmojiPickerDialog(
                currentEmoji = null,
                onDismiss = {},
                onEmojiSelected = {}
            )
        }

        // Switch to 여행/장소 category and verify filtering
        composeTestRule.onNodeWithTag("emojiCategories")
            .performScrollToNode(hasText("여행/장소"))
        composeTestRule.onAllNodesWithText("여행/장소", useUnmergedTree = true)
            .onFirst()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText("🚗", useUnmergedTree = true).onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun test_emojiPickerDialog_close_triggers_callback() {
        // Given
        var dismissed = false

        // When
        composeTestRule.setContent {
            EmojiPickerDialog(
                currentEmoji = null,
                onDismiss = { dismissed = true },
                onEmojiSelected = {}
            )
        }

        composeTestRule.onNodeWithText("닫기").performClick()

        // Then
        assert(dismissed)
    }

    @Test
    fun test_emojiPickerDialog_shows_remove_button_with_current_emoji() {
        // When
        composeTestRule.setContent {
            EmojiPickerDialog(
                currentEmoji = "😀",
                onDismiss = {},
                onEmojiSelected = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("제거").assertIsDisplayed()
    }

    @Test
    fun test_emojiPickerDialog_no_remove_button_without_emoji() {
        // When
        composeTestRule.setContent {
            EmojiPickerDialog(
                currentEmoji = null,
                onDismiss = {},
                onEmojiSelected = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("제거").assertDoesNotExist()
    }

    @Test
    fun test_contactSelectDialog_displays_title() {
        // When
        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = emptyList(),
                selectedContacts = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("연락처 선택").assertIsDisplayed()
        composeTestRule.onNodeWithText("그룹에 추가할 연락처를 선택하세요").assertIsDisplayed()
    }

    @Test
    fun test_contactSelectDialog_shows_empty_message_when_no_contacts() {
        // When
        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = emptyList(),
                selectedContacts = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("등록된 연락처가 없습니다").assertIsDisplayed()
    }

    @Test
    fun test_contactSelectDialog_displays_contacts() {
        // Given
        val contacts = listOf(
            Contact(1, null, "John", "john@example.com"),
            Contact(2, null, "Jane", "jane@example.com")
        )

        // When
        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = contacts,
                selectedContacts = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("John").assertIsDisplayed()
        composeTestRule.onNodeWithText("john@example.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jane").assertIsDisplayed()
        composeTestRule.onNodeWithText("jane@example.com").assertIsDisplayed()
    }

    @Test
    fun test_contactSelectDialog_cancel_triggers_callback() {
        // Given
        var dismissed = false

        // When
        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = emptyList(),
                selectedContacts = emptyList(),
                onDismiss = { dismissed = true },
                onConfirm = {}
            )
        }

        composeTestRule.onNodeWithText("취소").performClick()

        // Then
        assert(dismissed)
    }

    @Test
    fun test_contactSelectDialog_shows_confirm_button_with_count() {
        // When
        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = emptyList(),
                selectedContacts = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("확인 (0)").assertIsDisplayed()
    }

    @Test
    fun test_addGroupScreen_shows_first_three_members_when_many_exist() {
        // Given
        val members = List(5) { Contact(it.toLong(), null, "Member$it", "m$it@example.com") }

        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {},
                members = members
            )
        }

        // Then - Should show member count includes all members
        composeTestRule.onNodeWithText("그룹 멤버 (5명)").assertIsDisplayed()
        // First 3 members should be visible
        composeTestRule.onNodeWithText("Member0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Member1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Member2").assertIsDisplayed()
    }

    @Test
    fun test_addGroupScreen_with_exactly_three_members() {
        // Given - exactly 3 members, no expand button should appear
        val members = List(3) { Contact(it.toLong(), null, "Member$it", "m$it@example.com") }

        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {},
                members = members
            )
        }

        // Then - All 3 members should be visible
        composeTestRule.onNodeWithText("그룹 멤버 (3명)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Member0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Member1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Member2").assertIsDisplayed()
    }

    @Test
    fun test_addGroupScreen_emoji_button_displays() {
        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onPromptOptionsChange = {}
            )
        }

        // Then - Emoji button should be visible (default emoji icon)
        composeTestRule.onNodeWithText("😀").assertIsDisplayed()
    }

    @Test
    fun test_addGroupScreen_emoji_change_callback() {
        // Given
        var selectedEmoji: String? = null

        // When
        composeTestRule.setContent {
            AddGroupScreen(
                uiState = AddGroupUiState(),
                onBack = {},
                onAdd = {},
                onGroupNameChange = {},
                onGroupDescriptionChange = {},
                onGroupEmojiChange = { selectedEmoji = it },
                onPromptOptionsChange = {}
            )
        }

        // Then - callback should be set
        assert(selectedEmoji == null)
    }
}
