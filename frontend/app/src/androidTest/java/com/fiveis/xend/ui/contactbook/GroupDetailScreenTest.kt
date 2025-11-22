package com.fiveis.xend.ui.contactbook

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.model.PromptOption
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GroupDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun test_groupDetailScreen_shows_group_name() {
        val group = Group(
            id = 1L,
            name = "VIP Customers",
            description = "Important clients",
            emoji = "⭐",
            options = emptyList(),
            members = emptyList()
        )
        val uiState = GroupDetailUiState(
            group = group,
            contacts = emptyList(),
            tonePromptOptions = emptyList(),
            formatPromptOptions = emptyList()
        )

        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRemoveMember = {},
                onAddMembers = {},
                onRenameGroup = { _, _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        composeTestRule.onNodeWithText("VIP Customers").assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_shows_back_button() {
        val group = Group(
            id = 1L,
            name = "Test Group",
            description = null,
            emoji = null,
            options = emptyList(),
            members = emptyList()
        )
        val uiState = GroupDetailUiState(
            group = group,
            contacts = emptyList()
        )

        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRemoveMember = {},
                onAddMembers = {},
                onRenameGroup = { _, _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("뒤로가기").assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_back_button_click() {
        var backClicked = false
        val group = Group(
            id = 1L,
            name = "Test Group",
            description = null,
            emoji = null,
            options = emptyList(),
            members = emptyList()
        )
        val uiState = GroupDetailUiState(
            group = group,
            contacts = emptyList()
        )

        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = { backClicked = true },
                onRefresh = {},
                onMemberClick = {},
                onRemoveMember = {},
                onAddMembers = {},
                onRenameGroup = { _, _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("뒤로가기").performClick()
        assert(backClicked)
    }

    @Test
    fun test_groupDetailScreen_shows_group_description() {
        val group = Group(
            id = 1L,
            name = "VIP Customers",
            description = "Our most important clients",
            emoji = null,
            options = emptyList(),
            members = emptyList()
        )
        val uiState = GroupDetailUiState(
            group = group,
            contacts = emptyList()
        )

        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRemoveMember = {},
                onAddMembers = {},
                onRenameGroup = { _, _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        composeTestRule.onNodeWithText("Our most important clients").assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_shows_member_count() {
        val members = listOf(
            Contact(1L, null, "John", "john@example.com"),
            Contact(2L, null, "Jane", "jane@example.com")
        )
        val group = Group(
            id = 1L,
            name = "Team",
            description = null,
            emoji = null,
            options = emptyList(),
            members = members
        )
        val uiState = GroupDetailUiState(
            group = group,
            contacts = emptyList()
        )

        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRemoveMember = {},
                onAddMembers = {},
                onRenameGroup = { _, _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        composeTestRule.onNodeWithText("멤버 2명").assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_shows_members() {
        val members = listOf(
            Contact(1L, null, "Alice", "alice@example.com"),
            Contact(2L, null, "Bob", "bob@example.com")
        )
        val group = Group(
            id = 1L,
            name = "Team",
            description = null,
            emoji = null,
            options = emptyList(),
            members = members
        )
        val uiState = GroupDetailUiState(
            group = group,
            contacts = emptyList()
        )

        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRemoveMember = {},
                onAddMembers = {},
                onRenameGroup = { _, _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("alice@example.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
        composeTestRule.onNodeWithText("bob@example.com").assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_member_click_triggers_callback() {
        var clickedContact: Contact? = null
        val member = Contact(1L, null, "Alice", "alice@example.com")
        val group = Group(
            id = 1L,
            name = "Team",
            description = null,
            emoji = null,
            options = emptyList(),
            members = listOf(member)
        )
        val uiState = GroupDetailUiState(
            group = group,
            contacts = emptyList()
        )

        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = { clickedContact = it },
                onRemoveMember = {},
                onAddMembers = {},
                onRenameGroup = { _, _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        composeTestRule.onNodeWithText("Alice").performClick()
        assert(clickedContact?.id == member.id)
    }

    @Test
    fun test_groupDetailScreen_shows_add_member_button() {
        val group = Group(
            id = 1L,
            name = "Team",
            description = null,
            emoji = null,
            options = emptyList(),
            members = emptyList()
        )
        val uiState = GroupDetailUiState(
            group = group,
            contacts = emptyList()
        )

        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRemoveMember = {},
                onAddMembers = {},
                onRenameGroup = { _, _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("그룹에 멤버 추가").assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_shows_edit_button() {
        val group = Group(
            id = 1L,
            name = "Team",
            description = null,
            emoji = null,
            options = emptyList(),
            members = emptyList()
        )
        val uiState = GroupDetailUiState(
            group = group,
            contacts = emptyList()
        )

        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRemoveMember = {},
                onAddMembers = {},
                onRenameGroup = { _, _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("그룹 정보 수정").assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_shows_prompt_options_section() {
        val group = Group(
            id = 1L,
            name = "Team",
            description = null,
            emoji = null,
            options = emptyList(),
            members = emptyList()
        )
        val uiState = GroupDetailUiState(
            group = group,
            contacts = emptyList()
        )

        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRemoveMember = {},
                onAddMembers = {},
                onRenameGroup = { _, _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        composeTestRule.onNodeWithText("AI 프롬프트 설정").assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_shows_prompt_options() {
        val options = listOf(
            PromptOption(1L, "tone", "존댓말", "존댓말을 사용하세요"),
            PromptOption(2L, "format", "3~5문장", "간결하게 작성하세요")
        )
        val group = Group(
            id = 1L,
            name = "Team",
            description = null,
            emoji = null,
            options = options,
            members = emptyList()
        )
        val uiState = GroupDetailUiState(
            group = group,
            contacts = emptyList()
        )

        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRemoveMember = {},
                onAddMembers = {},
                onRenameGroup = { _, _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        composeTestRule.onNodeWithText("문체 스타일").assertIsDisplayed()
        composeTestRule.onNodeWithText("형식 가이드").assertIsDisplayed()
        composeTestRule.onNodeWithText("존댓말").assertIsDisplayed()
        composeTestRule.onNodeWithText("3~5문장").assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_shows_no_prompt_message_when_empty() {
        val group = Group(
            id = 1L,
            name = "Team",
            description = null,
            emoji = null,
            options = emptyList(),
            members = emptyList()
        )
        val uiState = GroupDetailUiState(
            group = group,
            contacts = emptyList()
        )

        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRemoveMember = {},
                onAddMembers = {},
                onRenameGroup = { _, _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        composeTestRule.onNodeWithText("설정된 프롬프트가 없습니다.\n프롬프트를 설정해 더 나은 메일 생성을 경험하세요!")
            .assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_shows_error_when_group_null() {
        val uiState = GroupDetailUiState(
            group = null,
            contacts = emptyList(),
            error = "그룹을 찾을 수 없습니다"
        )

        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRemoveMember = {},
                onAddMembers = {},
                onRenameGroup = { _, _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        composeTestRule.onNodeWithText("그룹을 찾을 수 없습니다").assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_shows_loading_when_group_null_no_error() {
        val uiState = GroupDetailUiState(
            group = null,
            contacts = emptyList(),
            error = null
        )

        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRemoveMember = {},
                onAddMembers = {},
                onRenameGroup = { _, _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        composeTestRule.onNodeWithText("불러오는 중...").assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_shows_group_emoji() {
        val group = Group(
            id = 1L,
            name = "Team",
            description = null,
            emoji = "🔥",
            options = emptyList(),
            members = emptyList()
        )
        val uiState = GroupDetailUiState(
            group = group,
            contacts = emptyList()
        )

        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRemoveMember = {},
                onAddMembers = {},
                onRenameGroup = { _, _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        composeTestRule.onNodeWithText("🔥").assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_shows_prompt_edit_button() {
        val group = Group(
            id = 1L,
            name = "Team",
            description = null,
            emoji = null,
            options = emptyList(),
            members = emptyList()
        )
        val uiState = GroupDetailUiState(
            group = group,
            contacts = emptyList()
        )

        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRemoveMember = {},
                onAddMembers = {},
                onRenameGroup = { _, _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("프롬프트 수정").assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_rename_dialog_opens_and_closes() {
        val group = Group(
            id = 1L,
            name = "Original Name",
            description = "Original Description",
            emoji = null,
            options = emptyList(),
            members = emptyList()
        )
        val uiState = GroupDetailUiState(
            group = group,
            contacts = emptyList()
        )

        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRemoveMember = {},
                onAddMembers = {},
                onRenameGroup = { _, _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("그룹 정보 수정").performClick()
        composeTestRule.onNodeWithText("그룹 정보 수정").assertIsDisplayed()
        composeTestRule.onNodeWithText("취소").performClick()
        composeTestRule.onNodeWithText("그룹 정보 수정").assertDoesNotExist()
    }

    @Test
    fun test_groupDetailScreen_rename_group_triggers_callback() {
        var renamedName: String? = null
        var renamedDescription: String? = null
        var renamedEmoji: String? = null
        val group = Group(
            id = 1L,
            name = "Original Name",
            description = "Original Description",
            emoji = null,
            options = emptyList(),
            members = emptyList()
        )
        val uiState = GroupDetailUiState(
            group = group,
            contacts = emptyList()
        )

        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRemoveMember = {},
                onAddMembers = {},
                onRenameGroup = { name, desc, emoji ->
                    renamedName = name
                    renamedDescription = desc
                    renamedEmoji = emoji
                },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("그룹 정보 수정").performClick()
        composeTestRule.waitForIdle()

        // Find editable text fields by their initial values and replace text
        composeTestRule.onNode(hasText("Original Name") and hasSetTextAction())
            .performTextReplacement("New Name")
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasText("Original Description") and hasSetTextAction())
            .performTextReplacement("New Description")
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText("저장")[0].performClick()

        assert(renamedName == "New Name")
        assert(renamedDescription == "New Description")
    }

    @Test
    fun test_groupDetailScreen_add_members_dialog_opens_and_closes() {
        val group = Group(
            id = 1L,
            name = "Team",
            description = null,
            emoji = null,
            options = emptyList(),
            members = emptyList()
        )
        val contact1 = Contact(1L, null, "Alice", "alice@example.com")
        val contact2 = Contact(2L, null, "Bob", "bob@example.com")
        val uiState = GroupDetailUiState(
            group = group,
            contacts = listOf(contact1, contact2)
        )

        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRemoveMember = {},
                onAddMembers = {},
                onRenameGroup = { _, _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("그룹에 멤버 추가").performClick()
        composeTestRule.onNodeWithText("멤버 추가").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
        composeTestRule.onNodeWithText("취소").performClick()
        composeTestRule.onNodeWithText("멤버 추가").assertDoesNotExist()
    }

    @Test
    fun test_groupDetailScreen_add_members_selection_triggers_callback() {
        var addedContactIds: List<Long>? = null
        val group = Group(
            id = 1L,
            name = "Team",
            description = null,
            emoji = null,
            options = emptyList(),
            members = emptyList()
        )
        val contact1 = Contact(1L, null, "Alice", "alice@example.com")
        val contact2 = Contact(2L, null, "Bob", "bob@example.com")
        val uiState = GroupDetailUiState(
            group = group,
            contacts = listOf(contact1, contact2)
        )

        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRemoveMember = {},
                onAddMembers = { ids -> addedContactIds = ids },
                onRenameGroup = { _, _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("그룹에 멤버 추가").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Alice").performClick()
        composeTestRule.waitForIdle()
        // Find the button that contains "추가" with a count
        composeTestRule.onNode(hasText("추가 (1)") or hasText("추가")).performClick()

        assert(addedContactIds?.contains(1L) == true)
    }

    @Test
    fun test_groupDetailScreen_remove_member_menu_shows_delete() {
        val member = Contact(1L, null, "Alice", "alice@example.com")
        val group = Group(
            id = 1L,
            name = "Team",
            description = null,
            emoji = null,
            options = emptyList(),
            members = listOf(member)
        )
        val uiState = GroupDetailUiState(
            group = group,
            contacts = emptyList()
        )

        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRemoveMember = {},
                onAddMembers = {},
                onRenameGroup = { _, _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        // Click the more menu button instead of long click
        composeTestRule.onNodeWithContentDescription("더보기(멤버 삭제)").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("멤버 삭제").assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_remove_member_triggers_callback() {
        var removedContact: Contact? = null
        val member = Contact(1L, null, "Alice", "alice@example.com")
        val group = Group(
            id = 1L,
            name = "Team",
            description = null,
            emoji = null,
            options = emptyList(),
            members = listOf(member)
        )
        val uiState = GroupDetailUiState(
            group = group,
            contacts = emptyList()
        )

        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRemoveMember = { removedContact = it },
                onAddMembers = {},
                onRenameGroup = { _, _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        // Click the more menu button
        composeTestRule.onNodeWithContentDescription("더보기(멤버 삭제)").performClick()
        composeTestRule.waitForIdle()
        // Click "멤버 삭제" in the dropdown menu
        composeTestRule.onNodeWithText("멤버 삭제").performClick()
        composeTestRule.waitForIdle()
        // Confirm deletion in the confirmation dialog
        composeTestRule.onAllNodesWithText("삭제")[0].performClick()

        assert(removedContact?.id == 1L)
    }

    @Test
    fun test_groupDetailScreen_prompt_edit_button_opens_bottom_sheet() {
        val group = Group(
            id = 1L,
            name = "Team",
            description = null,
            emoji = null,
            options = emptyList(),
            members = emptyList()
        )
        val uiState = GroupDetailUiState(
            group = group,
            contacts = emptyList(),
            tonePromptOptions = listOf(
                PromptOption(1L, "tone", "존댓말", "존댓말을 사용하세요"),
                PromptOption(2L, "tone", "반말", "반말을 사용하세요")
            ),
            formatPromptOptions = listOf(
                PromptOption(3L, "format", "3~5문장", "간결하게 작성하세요")
            )
        )

        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRemoveMember = {},
                onAddMembers = {},
                onRenameGroup = { _, _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("프롬프트 수정").performClick()
        composeTestRule.waitForIdle()
        // The bottom sheet shows "AI 프롬프트 설정" - use onFirst to avoid finding both the section title and dialog title
        composeTestRule.onAllNodesWithText("AI 프롬프트 설정").onFirst().assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_save_prompt_options_triggers_callback() {
        var savedPromptIds: List<Long>? = null
        val group = Group(
            id = 1L,
            name = "Team",
            description = null,
            emoji = null,
            options = emptyList(),
            members = emptyList()
        )
        val uiState = GroupDetailUiState(
            group = group,
            contacts = emptyList(),
            tonePromptOptions = listOf(
                PromptOption(1L, "tone", "존댓말", "존댓말을 사용하세요"),
                PromptOption(2L, "tone", "반말", "반말을 사용하세요")
            ),
            formatPromptOptions = listOf(
                PromptOption(3L, "format", "3~5문장", "간결하게 작성하세요")
            )
        )

        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRemoveMember = {},
                onAddMembers = {},
                onRenameGroup = { _, _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = { ids ->
                    savedPromptIds = ids
                },
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("프롬프트 수정").performClick()
        composeTestRule.onNodeWithText("존댓말").performClick()
        composeTestRule.onAllNodesWithText("저장")[0].performClick()

        assert(savedPromptIds?.contains(1L) == true)
    }

    @Test
    fun test_groupDetailScreen_shows_rename_error() {
        val group = Group(
            id = 1L,
            name = "Team",
            description = null,
            emoji = null,
            options = emptyList(),
            members = emptyList()
        )
        val uiState = GroupDetailUiState(
            group = group,
            contacts = emptyList(),
            renameError = "그룹 이름 변경에 실패했습니다"
        )

        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRemoveMember = {},
                onAddMembers = {},
                onRenameGroup = { _, _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("그룹 정보 수정").performClick()
        composeTestRule.onNodeWithText("그룹 이름 변경에 실패했습니다").assertIsDisplayed()
    }

    @Test
    fun test_groupDetailScreen_multiple_members_display() {
        val members = listOf(
            Contact(1L, null, "Alice", "alice@example.com"),
            Contact(2L, null, "Bob", "bob@example.com"),
            Contact(3L, null, "Charlie", "charlie@example.com")
        )
        val group = Group(
            id = 1L,
            name = "Team",
            description = null,
            emoji = null,
            options = emptyList(),
            members = members
        )
        val uiState = GroupDetailUiState(
            group = group,
            contacts = emptyList()
        )

        composeTestRule.setContent {
            GroupDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onMemberClick = {},
                onRemoveMember = {},
                onAddMembers = {},
                onRenameGroup = { _, _, _ -> },
                onClearRenameError = {},
                onRefreshPromptOptions = {},
                onSavePromptOptions = {},
                onAddPromptOption = { _, _, _, _, _ -> },
                onClearPromptError = {}
            )
        }

        composeTestRule.onNodeWithText("멤버 3명").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
        composeTestRule.onNodeWithText("Charlie").assertIsDisplayed()
    }
}
