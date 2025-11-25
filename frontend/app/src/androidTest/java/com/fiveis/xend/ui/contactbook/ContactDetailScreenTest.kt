package com.fiveis.xend.ui.contactbook

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.ContactContext
import com.fiveis.xend.data.model.Group
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun test_contactDetailScreen_shows_contact_name_and_email() {
        val contact = Contact(
            id = 1L,
            group = null,
            name = "John Doe",
            email = "john@example.com",
            context = null
        )
        val uiState = ContactDetailUiState(
            contact = contact,
            groups = emptyList(),
            isLoading = false,
            error = null,
            isUpdating = false,
            updateError = null
        )

        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {},
                onComposeMail = {},
                onUpdateContact = { _, _, _, _, _, _ -> },
                onClearEditError = {}
            )
        }

        composeTestRule.onNodeWithText("John Doe").assertIsDisplayed()
        composeTestRule.onNodeWithText("john@example.com").assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_shows_back_button() {
        val uiState = ContactDetailUiState(
            contact = Contact(1L, null, "Test", "test@example.com"),
            groups = emptyList()
        )

        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {},
                onComposeMail = {},
                onUpdateContact = { _, _, _, _, _, _ -> },
                onClearEditError = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("뒤로가기").assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_back_button_click() {
        var backClicked = false
        val uiState = ContactDetailUiState(
            contact = Contact(1L, null, "Test", "test@example.com"),
            groups = emptyList()
        )

        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = { backClicked = true },
                onRefresh = {},
                onOpenGroup = {},
                onComposeMail = {},
                onUpdateContact = { _, _, _, _, _, _ -> },
                onClearEditError = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("뒤로가기").performClick()
        assert(backClicked)
    }

    @Test
    fun test_contactDetailScreen_shows_mail_write_button() {
        val uiState = ContactDetailUiState(
            contact = Contact(1L, null, "Test", "test@example.com"),
            groups = emptyList()
        )

        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {},
                onComposeMail = {},
                onUpdateContact = { _, _, _, _, _, _ -> },
                onClearEditError = {}
            )
        }

        composeTestRule.onNodeWithText("메일 쓰기").assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_compose_mail_button_click() {
        var composedContact: Contact? = null
        val contact = Contact(1L, null, "Test", "test@example.com")
        val uiState = ContactDetailUiState(
            contact = contact,
            groups = emptyList()
        )

        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {},
                onComposeMail = { composedContact = it },
                onUpdateContact = { _, _, _, _, _, _ -> },
                onClearEditError = {}
            )
        }

        composeTestRule.onNodeWithText("메일 쓰기").performClick()
        assert(composedContact?.id == contact.id)
    }

    @Test
    fun test_contactDetailScreen_shows_contact_context() {
        val context = ContactContext(
            id = 1L,
            senderRole = "상사",
            recipientRole = "부하직원",
            personalPrompt = "친절하게 대화합니다"
        )
        val contact = Contact(
            id = 1L,
            group = null,
            name = "John Doe",
            email = "john@example.com",
            context = context
        )
        val uiState = ContactDetailUiState(
            contact = contact,
            groups = emptyList()
        )

        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {},
                onComposeMail = {},
                onUpdateContact = { _, _, _, _, _, _ -> },
                onClearEditError = {}
            )
        }

        composeTestRule.onNodeWithText("상사", substring = true).assertExists()
        composeTestRule.onNodeWithText("부하직원", substring = true).assertExists()
        composeTestRule.onNodeWithText("친절하게 대화합니다").assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_shows_group_info_when_member() {
        val group = Group(
            id = 1L,
            name = "VIP Customers",
            description = "Important clients",
            emoji = "⭐",
            options = emptyList(),
            members = emptyList()
        )
        val contact = Contact(
            id = 1L,
            group = group,
            name = "John Doe",
            email = "john@example.com"
        )
        val uiState = ContactDetailUiState(
            contact = contact,
            groups = emptyList()
        )

        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {},
                onComposeMail = {},
                onUpdateContact = { _, _, _, _, _, _ -> },
                onClearEditError = {}
            )
        }

        composeTestRule.onNodeWithText("VIP Customers").assertIsDisplayed()
        composeTestRule.onNodeWithText("Important clients").assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_shows_no_group_message_when_not_member() {
        val contact = Contact(
            id = 1L,
            group = null,
            name = "John Doe",
            email = "john@example.com"
        )
        val uiState = ContactDetailUiState(
            contact = contact,
            groups = emptyList()
        )

        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {},
                onComposeMail = {},
                onUpdateContact = { _, _, _, _, _, _ -> },
                onClearEditError = {}
            )
        }

        composeTestRule.onNodeWithText("소속된 그룹이 없습니다.").assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_group_click_triggers_callback() {
        var openedGroupId: Long? = null
        val group = Group(
            id = 1L,
            name = "VIP Customers",
            description = "Important clients",
            emoji = null,
            options = emptyList(),
            members = emptyList()
        )
        val contact = Contact(
            id = 1L,
            group = group,
            name = "John Doe",
            email = "john@example.com"
        )
        val uiState = ContactDetailUiState(
            contact = contact,
            groups = emptyList()
        )

        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = { openedGroupId = it },
                onComposeMail = {},
                onUpdateContact = { _, _, _, _, _, _ -> },
                onClearEditError = {}
            )
        }

        composeTestRule.onNodeWithText("VIP Customers").performClick()
        assert(openedGroupId == 1L)
    }

    @Test
    fun test_contactDetailScreen_shows_edit_button() {
        val contact = Contact(1L, null, "Test", "test@example.com")
        val uiState = ContactDetailUiState(
            contact = contact,
            groups = emptyList()
        )

        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {},
                onComposeMail = {},
                onUpdateContact = { _, _, _, _, _, _ -> },
                onClearEditError = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("연락처 정보 수정").assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_shows_error_when_contact_null() {
        val uiState = ContactDetailUiState(
            contact = null,
            groups = emptyList(),
            error = "연락처를 찾을 수 없습니다"
        )

        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {},
                onComposeMail = {},
                onUpdateContact = { _, _, _, _, _, _ -> },
                onClearEditError = {}
            )
        }

        composeTestRule.onNodeWithText("연락처를 찾을 수 없습니다").assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_shows_loading_when_contact_null_no_error() {
        val uiState = ContactDetailUiState(
            contact = null,
            groups = emptyList(),
            error = null
        )

        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {},
                onComposeMail = {},
                onUpdateContact = { _, _, _, _, _, _ -> },
                onClearEditError = {}
            )
        }

        composeTestRule.onNodeWithText("불러오는 중...").assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_shows_personal_prompt_section() {
        val contact = Contact(1L, null, "Test", "test@example.com")
        val uiState = ContactDetailUiState(
            contact = contact,
            groups = emptyList()
        )

        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {},
                onComposeMail = {},
                onUpdateContact = { _, _, _, _, _, _ -> },
                onClearEditError = {}
            )
        }

        composeTestRule.onNodeWithText("개인 프롬프트").assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_shows_group_section_label() {
        val contact = Contact(1L, null, "Test", "test@example.com")
        val uiState = ContactDetailUiState(
            contact = contact,
            groups = emptyList()
        )

        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {},
                onComposeMail = {},
                onUpdateContact = { _, _, _, _, _, _ -> },
                onClearEditError = {}
            )
        }

        composeTestRule.onNodeWithText("소속 그룹").assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_edit_dialog_opens_and_closes() {
        val contact = Contact(1L, null, "John Doe", "john@example.com")
        val uiState = ContactDetailUiState(
            contact = contact,
            groups = emptyList()
        )

        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {},
                onComposeMail = {},
                onUpdateContact = { _, _, _, _, _, _ -> },
                onClearEditError = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("연락처 정보 수정").performClick()
        composeTestRule.onNodeWithText("연락처 정보 수정").assertIsDisplayed()
        composeTestRule.onNodeWithText("취소").performClick()
        composeTestRule.onNodeWithText("연락처 정보 수정").assertDoesNotExist()
    }

    @Test
    fun test_contactDetailScreen_edit_contact_name_and_email() {
        var updatedName: String? = null
        var updatedEmail: String? = null
        val contact = Contact(1L, null, "John Doe", "john@example.com")
        val uiState = ContactDetailUiState(
            contact = contact,
            groups = emptyList()
        )

        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {},
                onComposeMail = {},
                onUpdateContact = { name, email, _, _, _, _ ->
                    updatedName = name
                    updatedEmail = email
                },
                onClearEditError = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("연락처 정보 수정").performClick()
        composeTestRule.waitForIdle()

        // Find editable text fields by their initial values and replace text
        composeTestRule.onNode(hasText("John Doe") and hasSetTextAction())
            .performTextReplacement("Jane Doe")
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasText("john@example.com") and hasSetTextAction())
            .performTextReplacement("jane@example.com")
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText("저장")[0].performClick()

        assert(updatedName == "Jane Doe")
        assert(updatedEmail == "jane@example.com")
    }

    @Test
    fun test_contactDetailScreen_edit_contact_context() {
        var updateCalled = false
        val context = ContactContext(
            id = 1L,
            senderRole = "상사",
            recipientRole = "부하직원",
            personalPrompt = "친절하게"
        )
        val contact = Contact(1L, null, "John Doe", "john@example.com", context)
        val uiState = ContactDetailUiState(
            contact = contact,
            groups = emptyList()
        )

        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {},
                onComposeMail = {},
                onUpdateContact = { _, _, _, _, _, _ ->
                    updateCalled = true
                },
                onClearEditError = {}
            )
        }

        // Open edit dialog
        composeTestRule.onNodeWithContentDescription("연락처 정보 수정").performClick()
        composeTestRule.waitForIdle()

        // Just click save without changing anything
        composeTestRule.onAllNodesWithText("저장")[0].performClick()

        // Verify update was called
        assert(updateCalled)
    }

    @Test
    fun test_contactDetailScreen_shows_update_error() {
        val contact = Contact(1L, null, "John Doe", "john@example.com")
        val uiState = ContactDetailUiState(
            contact = contact,
            groups = emptyList(),
            updateError = "연락처 업데이트에 실패했습니다"
        )

        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {},
                onComposeMail = {},
                onUpdateContact = { _, _, _, _, _, _ -> },
                onClearEditError = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("연락처 정보 수정").performClick()
        composeTestRule.onNodeWithText("연락처 업데이트에 실패했습니다").assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_shows_loading_state() {
        val uiState = ContactDetailUiState(
            contact = null,
            groups = emptyList(),
            isLoading = true
        )

        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {},
                onComposeMail = {},
                onUpdateContact = { _, _, _, _, _, _ -> },
                onClearEditError = {}
            )
        }

        composeTestRule.onNodeWithText("불러오는 중...").assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_shows_updating_state() {
        val contact = Contact(1L, null, "John Doe", "john@example.com")
        val uiState = ContactDetailUiState(
            contact = contact,
            groups = emptyList(),
            isUpdating = true
        )

        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {},
                onComposeMail = {},
                onUpdateContact = { _, _, _, _, _, _ -> },
                onClearEditError = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("연락처 정보 수정").performClick()
        composeTestRule.onAllNodesWithText("저장")[0].assertIsNotEnabled()
    }

    @Test
    fun test_contactDetailScreen_shows_contact_with_no_context() {
        val contact = Contact(1L, null, "John Doe", "john@example.com", null)
        val uiState = ContactDetailUiState(
            contact = contact,
            groups = emptyList()
        )

        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {},
                onComposeMail = {},
                onUpdateContact = { _, _, _, _, _, _ -> },
                onClearEditError = {}
            )
        }

        composeTestRule.onNodeWithText("John Doe").assertIsDisplayed()
        composeTestRule.onNodeWithText("john@example.com").assertIsDisplayed()
    }

    @Test
    fun test_contactDetailScreen_edit_group_selection() {
        var updatedGroupId: Long? = null
        val group1 = Group(1L, "VIP", "Important", null, emptyList(), emptyList())
        val group2 = Group(2L, "Team", "Colleagues", null, emptyList(), emptyList())
        val contact = Contact(1L, null, "John Doe", "john@example.com")
        val uiState = ContactDetailUiState(
            contact = contact,
            groups = listOf(group1, group2)
        )

        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {},
                onComposeMail = {},
                onUpdateContact = { _, _, _, _, _, groupId ->
                    updatedGroupId = groupId
                },
                onClearEditError = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("연락처 정보 수정").performClick()
        composeTestRule.waitForIdle()
        // Click on the group selection dropdown which shows "그룹 없음"
        composeTestRule.onNodeWithText("그룹 없음").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("VIP").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("저장")[0].performClick()

        assert(updatedGroupId == 1L)
    }

    @Test
    fun test_contactDetailScreen_shows_multiple_groups() {
        val group1 = Group(1L, "VIP", "Important", "⭐", emptyList(), emptyList())
        val group2 = Group(2L, "Team", "Colleagues", "👥", emptyList(), emptyList())
        val contact = Contact(1L, group1, "John Doe", "john@example.com")
        val uiState = ContactDetailUiState(
            contact = contact,
            groups = listOf(group1, group2)
        )

        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {},
                onComposeMail = {},
                onUpdateContact = { _, _, _, _, _, _ -> },
                onClearEditError = {}
            )
        }

        composeTestRule.onNodeWithText("VIP").assertIsDisplayed()
        composeTestRule.onNodeWithText("Important").assertIsDisplayed()
    }

    // ContactDetailScreen does not have pull-to-refresh functionality
    // This test is removed as it doesn't match the actual implementation

    @Test
    fun test_contactDetailScreen_shows_context_labels() {
        val context = ContactContext(
            id = 1L,
            senderRole = "상사",
            recipientRole = "부하직원",
            personalPrompt = "친절하게 대화합니다"
        )
        val contact = Contact(1L, null, "John Doe", "john@example.com", context)
        val uiState = ContactDetailUiState(
            contact = contact,
            groups = emptyList()
        )

        composeTestRule.setContent {
            ContactDetailScreen(
                themeColor = Color.Blue,
                uiState = uiState,
                onBack = {},
                onRefresh = {},
                onOpenGroup = {},
                onComposeMail = {},
                onUpdateContact = { _, _, _, _, _, _ -> },
                onClearEditError = {}
            )
        }

        composeTestRule.onNodeWithText("John Doe 님께 나는", substring = true).assertExists()
        composeTestRule.onNodeWithText("나에게 John Doe 님은", substring = true).assertExists()
        composeTestRule.onNodeWithText("개인 프롬프트").assertIsDisplayed()
    }
}
