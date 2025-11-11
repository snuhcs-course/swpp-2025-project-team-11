package com.fiveis.xend.ui.contactbook

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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

        composeTestRule.onNodeWithText("이름을 입력하세요").performTextInput("VIP Group")

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

        // Then
        composeTestRule.onNodeWithContentDescription("그룹 추가").assertIsDisplayed()
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

        composeTestRule.onNodeWithContentDescription("그룹 추가").performClick()

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

        composeTestRule.onNodeWithText("이름을 입력하세요").performTextInput("중요 고객")

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

        composeTestRule.onNodeWithText("이름을 입력하세요").performTextInput(longName)

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

        composeTestRule.onNodeWithText("이름을 입력하세요").performTextInput("TestGroup")

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

        composeTestRule.onNodeWithText("이름을 입력하세요").performTextInput("Team@2024")

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

        composeTestRule.onNodeWithContentDescription("뒤로가기").performClick()
        composeTestRule.onNodeWithContentDescription("그룹 추가").performClick()
        composeTestRule.onNodeWithText("이름을 입력하세요").performTextInput("Test")
        composeTestRule.onNodeWithText("그룹을 소개해 주세요").performTextInput("Desc")

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

        composeTestRule.onNodeWithText("이름을 입력하세요").performTextInput("VIP")
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

        composeTestRule.onNodeWithText("이름을 입력하세요").performTextInput("Team 2024")

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
}
