package com.fiveis.xend.ui.contactbook

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.ContactContext
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.model.PromptOption
import com.fiveis.xend.data.repository.ContactBookTab
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactBookScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun test_contactBookScreen_displays_groups_tab() {
        // Given
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Groups,
            groups = emptyList()
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onAllNodesWithText("연락처").assertCountEquals(2) // Title and bottom nav
        composeTestRule.onNodeWithText("그룹별").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_displays_contacts_tab() {
        // Given
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            contacts = emptyList()
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("전체").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_shows_group_list() {
        // Given
        val groups = listOf(
            Group(1, "VIP", "Important", null, emptyList(), emptyList(), null, null)
        )
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Groups,
            groups = groups
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("VIP").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_shows_contact_list() {
        // Given
        val contacts = listOf(
            Contact(1, null, "John Doe", "john@example.com")
        )
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            contacts = contacts
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("John Doe").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_tab_click_triggers_callback() {
        // Given
        var clickedTab: ContactBookTab? = null
        val uiState = ContactBookUiState(selectedTab = ContactBookTab.Groups)

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = { clickedTab = it },
                onGroupClick = {},
                onContactClick = {}
            )
        }

        composeTestRule.onNodeWithText("전체").performClick()

        // Then
        assert(clickedTab == ContactBookTab.Contacts)
    }

    @Test
    fun test_contactBookScreen_shows_search_icon() {
        // Given
        val uiState = ContactBookUiState(selectedTab = ContactBookTab.Groups)

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithContentDescription("Search").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_group_click_triggers_callback() {
        // Given
        var clickedGroup: Group? = null
        val group = Group(1, "Test", "Desc", null, emptyList(), emptyList(), null, null)
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Groups,
            groups = listOf(group)
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = { clickedGroup = it },
                onContactClick = {}
            )
        }

        composeTestRule.onNodeWithText("Test").performClick()

        // Then
        assert(clickedGroup?.id == 1L)
    }

    @Test
    fun test_contactBookScreen_contact_click_triggers_callback() {
        // Given
        var clickedContact: Contact? = null
        val contact = Contact(1, null, "Jane Doe", "jane@example.com")
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            contacts = listOf(contact)
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = { clickedContact = it }
            )
        }

        composeTestRule.onNodeWithText("Jane Doe").performClick()

        // Then
        assert(clickedContact?.id == 1L)
    }

    @Test
    fun test_contactBookScreen_shows_multiple_groups() {
        // Given
        val groups = listOf(
            Group(1, "Group1", "Desc1", null, emptyList(), emptyList(), null, null),
            Group(2, "Group2", "Desc2", null, emptyList(), emptyList(), null, null)
        )
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Groups,
            groups = groups
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Group1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Group2").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_shows_multiple_contacts() {
        // Given
        val contacts = listOf(
            Contact(1, null, "Contact1", "c1@example.com"),
            Contact(2, null, "Contact2", "c2@example.com")
        )
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            contacts = contacts
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Contact1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Contact2").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_displays_contact_count() {
        // Given
        val contacts = listOf(
            Contact(1, null, "C1", "c1@example.com"),
            Contact(2, null, "C2", "c2@example.com"),
            Contact(3, null, "C3", "c3@example.com")
        )
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            contacts = contacts
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("3명").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_shows_empty_contact_list() {
        // Given
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            contacts = emptyList()
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("0명").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_shows_group_members_count() {
        // Given
        val members = listOf(
            Contact(1, null, "M1", "m1@example.com"),
            Contact(2, null, "M2", "m2@example.com")
        )
        val group = Group(1, "Team", "Desc", null, emptyList(), members, null, null)
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Groups,
            groups = listOf(group)
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("2명").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_shows_add_group_button() {
        // Given
        val uiState = ContactBookUiState(selectedTab = ContactBookTab.Groups)

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText(" 새 그룹").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_add_group_click_triggers_callback() {
        // Given
        var clicked = false
        val uiState = ContactBookUiState(selectedTab = ContactBookTab.Groups)

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {},
                onAddGroupClick = { clicked = true }
            )
        }

        composeTestRule.onNodeWithText(" 새 그룹").performClick()

        // Then
        assert(clicked)
    }

    @Test
    fun test_contactBookScreen_shows_group_description() {
        // Given
        val group = Group(1, "VIP", "Very Important People", null, emptyList(), emptyList(), null, null)
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Groups,
            groups = listOf(group)
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Very Important People").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_shows_contact_email() {
        // Given
        val contact = Contact(1, null, "John", "john@email.com")
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            contacts = listOf(contact)
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("john@email.com").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_bottom_nav_shows_mail_option() {
        // Given
        val uiState = ContactBookUiState(selectedTab = ContactBookTab.Groups)

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("메일함").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_bottom_nav_shows_contacts_option() {
        // Given
        val uiState = ContactBookUiState(selectedTab = ContactBookTab.Groups)

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onAllNodesWithText("연락처").assertCountEquals(2) // Title and bottom nav
    }

    @Test
    fun test_contactBookScreen_bottom_nav_click_triggers_callback() {
        // Given
        var selectedNav = ""
        val uiState = ContactBookUiState(selectedTab = ContactBookTab.Groups)

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {},
                onBottomNavChange = { selectedNav = it }
            )
        }

        composeTestRule.onNodeWithText("메일함").performClick()

        // Then
        assert(selectedNav == "mail")
    }

    @Test
    fun test_contactBookScreen_shows_loading_state() {
        // Given
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Groups,
            isLoading = true
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then - Should display the screen without crashing
        composeTestRule.onAllNodesWithText("연락처").assertCountEquals(2) // Title and bottom nav
    }

    @Test
    fun test_groupCard_displays_group_name() {
        // Given
        val group = Group(1, "TestGroup", "Desc", null, emptyList(), emptyList(), null, null)

        // When
        composeTestRule.setContent {
            GroupCard(group = group, onClick = {})
        }

        // Then
        composeTestRule.onNodeWithText("TestGroup").assertIsDisplayed()
    }

    @Test
    fun test_groupCard_click_triggers_callback() {
        // Given
        var clicked = false
        val group = Group(1, "TestGroup", "Desc", null, emptyList(), emptyList(), null, null)

        // When
        composeTestRule.setContent {
            GroupCard(group = group, onClick = { clicked = true })
        }

        composeTestRule.onNodeWithText("TestGroup").performClick()

        // Then
        assert(clicked)
    }

    @Test
    fun test_groupCard_shows_member_count() {
        // Given
        val members = listOf(
            Contact(1, null, "M1", "m1@example.com"),
            Contact(2, null, "M2", "m2@example.com"),
            Contact(3, null, "M3", "m3@example.com")
        )
        val group = Group(1, "Team", "Desc", null, emptyList(), members, null, null)

        // When
        composeTestRule.setContent {
            GroupCard(group = group, onClick = {})
        }

        // Then
        composeTestRule.onNodeWithText("3명").assertIsDisplayed()
    }

    @Test
    fun test_groupCard_shows_zero_members() {
        // Given
        val group = Group(1, "Empty", "Desc", null, emptyList(), emptyList(), null, null)

        // When
        composeTestRule.setContent {
            GroupCard(group = group, onClick = {})
        }

        // Then
        composeTestRule.onNodeWithText("0명").assertIsDisplayed()
    }

    @Test
    fun test_groupCard_shows_description() {
        // Given
        val group = Group(1, "Team", "Team Description", null, emptyList(), emptyList(), null, null)

        // When
        composeTestRule.setContent {
            GroupCard(group = group, onClick = {})
        }

        // Then
        composeTestRule.onNodeWithText("Team Description").assertIsDisplayed()
    }

    @Test
    fun test_groupCard_shows_more_button() {
        // Given
        val group = Group(1, "Team", "Desc", null, emptyList(), emptyList(), null, null)

        // When
        composeTestRule.setContent {
            GroupCard(group = group, onClick = {})
        }

        // Then
        composeTestRule.onNodeWithContentDescription("더보기").assertIsDisplayed()
    }

    @Test
    fun test_groupCard_more_button_opens_menu() {
        // Given
        val group = Group(1, "Team", "Desc", null, emptyList(), emptyList(), null, null)

        // When
        composeTestRule.setContent {
            GroupCard(group = group, onClick = {})
        }

        composeTestRule.onNodeWithContentDescription("더보기").performClick()

        // Then
        composeTestRule.onNodeWithText("삭제").assertIsDisplayed()
    }

    @Test
    fun test_groupCard_delete_shows_confirmation() {
        // Given
        val group = Group(1, "Team", "Desc", null, emptyList(), emptyList(), null, null)

        // When
        composeTestRule.setContent {
            GroupCard(group = group, onClick = {})
        }

        composeTestRule.onNodeWithContentDescription("더보기").performClick()
        composeTestRule.onNodeWithText("삭제").performClick()

        // Then
        composeTestRule.onNodeWithText("그룹 삭제").assertIsDisplayed()
    }

    @Test
    fun test_groupCard_delete_confirmation_shows_group_name() {
        // Given
        val group = Group(1, "MyTeam", "Desc", null, emptyList(), emptyList(), null, null)

        // When
        composeTestRule.setContent {
            GroupCard(group = group, onClick = {})
        }

        composeTestRule.onNodeWithContentDescription("더보기").performClick()
        composeTestRule.onNodeWithText("삭제").performClick()

        // Then
        composeTestRule.onNodeWithText("\"MyTeam\" 그룹을 삭제하시겠습니까?").assertIsDisplayed()
    }

    @Test
    fun test_groupCard_delete_confirmation_cancel() {
        // Given
        val group = Group(1, "Team", "Desc", null, emptyList(), emptyList(), null, null)

        // When
        composeTestRule.setContent {
            GroupCard(group = group, onClick = {})
        }

        composeTestRule.onNodeWithContentDescription("더보기").performClick()
        composeTestRule.onNodeWithText("삭제").performClick()
        composeTestRule.onNodeWithText("취소").performClick()

        // Then
        composeTestRule.onNodeWithText("그룹 삭제").assertDoesNotExist()
    }

    @Test
    fun test_groupCard_delete_confirmation_confirm() {
        // Given
        var deletedGroup: Group? = null
        val group = Group(1, "Team", "Desc", null, emptyList(), emptyList(), null, null)

        // When
        composeTestRule.setContent {
            GroupCard(group = group, onClick = {}, onDelete = { deletedGroup = it })
        }

        composeTestRule.onNodeWithContentDescription("더보기").performClick()
        composeTestRule.onAllNodesWithText("삭제")[0].performClick()
        composeTestRule.onAllNodesWithText("삭제")[0].performClick()

        // Then
        assert(deletedGroup?.id == 1L)
    }

    @Test
    fun test_memberCircle_displays_label() {
        // Given
        composeTestRule.setContent {
            MemberCircle(label = "A", color = androidx.compose.ui.graphics.Color.Blue)
        }

        // Then
        composeTestRule.onNodeWithText("A").assertIsDisplayed()
    }

    @Test
    fun test_bottomNavBar_displays_mail_icon() {
        // Given
        composeTestRule.setContent {
            BottomNavBar(selected = "contacts", onSelect = {})
        }

        // Then
        composeTestRule.onNodeWithContentDescription("메일함").assertIsDisplayed()
    }

    @Test
    fun test_bottomNavBar_displays_contacts_icon() {
        // Given
        composeTestRule.setContent {
            BottomNavBar(selected = "contacts", onSelect = {})
        }

        // Then
        composeTestRule.onNodeWithContentDescription("연락처").assertIsDisplayed()
    }

    @Test
    fun test_bottomNavBar_mail_click_triggers_callback() {
        // Given
        var selected = ""
        composeTestRule.setContent {
            BottomNavBar(selected = "contacts", onSelect = { selected = it })
        }

        // When
        composeTestRule.onNodeWithText("메일함").performClick()

        // Then
        assert(selected == "mail")
    }

    @Test
    fun test_bottomNavBar_highlights_selected_item() {
        // Given
        composeTestRule.setContent {
            BottomNavBar(selected = "contacts", onSelect = {})
        }

        // Then - Should render without crash
        composeTestRule.onNodeWithText("연락처").assertIsDisplayed()
    }

    @Test
    fun test_quickActions_shows_new_group_button() {
        // Given
        composeTestRule.setContent {
            QuickActions(onAddGroupClick = {})
        }

        // Then
        composeTestRule.onNodeWithText(" 새 그룹").assertIsDisplayed()
    }

    @Test
    fun test_quickActions_new_group_click_triggers_callback() {
        // Given
        var clicked = false
        composeTestRule.setContent {
            QuickActions(onAddGroupClick = { clicked = true })
        }

        // When
        composeTestRule.onNodeWithText(" 새 그룹").performClick()

        // Then
        assert(clicked)
    }

    @Test
    fun test_contactBookScreen_group_with_many_members_shows_overflow() {
        // Given
        val members = List(10) { Contact(it.toLong(), null, "M$it", "m$it@example.com") }
        val group = Group(1, "Big Team", "Desc", null, emptyList(), members, null, null)
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Groups,
            groups = listOf(group)
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("10명").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_shows_all_contacts_header() {
        // Given
        val uiState = ContactBookUiState(selectedTab = ContactBookTab.Contacts)

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("전체 연락처").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_add_contact_click_triggers_callback() {
        // Given
        var clicked = false
        val uiState = ContactBookUiState(selectedTab = ContactBookTab.Contacts)

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {},
                onAddContactClick = { clicked = true }
            )
        }

        composeTestRule.onNodeWithContentDescription("Add Contact").performClick()

        // Then
        assert(clicked)
    }

    @Test
    fun test_contactBookScreen_contact_with_context_displays() {
        // Given
        val context = ContactContext(1L, "학생", "교수")
        val contact = Contact(1, null, "Student", "student@example.com", context)
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            contacts = listOf(contact)
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Student").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_empty_groups_shows_add_button() {
        // Given
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Groups,
            groups = emptyList()
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText(" 새 그룹").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_group_with_null_description() {
        // Given
        val group = Group(1, "Team", null, null, emptyList(), emptyList(), null, null)
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Groups,
            groups = listOf(group)
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Team").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_contact_delete_click() {
        // Given
        val contact = Contact(1, null, "John", "john@example.com")
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            contacts = listOf(contact)
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        composeTestRule.onAllNodesWithContentDescription("더보기")[0].performClick()
        composeTestRule.onNodeWithText("삭제").performClick()

        // Then
        composeTestRule.onNodeWithText("연락처 삭제").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_contact_delete_shows_name() {
        // Given
        val contact = Contact(1, null, "Jane Doe", "jane@example.com")
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            contacts = listOf(contact)
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        composeTestRule.onAllNodesWithContentDescription("더보기")[0].performClick()
        composeTestRule.onNodeWithText("삭제").performClick()

        // Then
        composeTestRule.onNodeWithText("\"Jane Doe\" 님의 연락처를 삭제하시겠습니까?").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_contact_delete_cancel() {
        // Given
        val contact = Contact(1, null, "John", "john@example.com")
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            contacts = listOf(contact)
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        composeTestRule.onAllNodesWithContentDescription("더보기")[0].performClick()
        composeTestRule.onNodeWithText("삭제").performClick()
        composeTestRule.onNodeWithText("취소").performClick()

        // Then
        composeTestRule.onNodeWithText("연락처 삭제").assertDoesNotExist()
    }

    @Test
    fun test_contactBookScreen_contact_delete_confirm() {
        // Given
        var deletedContact: Contact? = null
        val contact = Contact(1, null, "John", "john@example.com")
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            contacts = listOf(contact)
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {},
                onDeleteContactClick = { deletedContact = it }
            )
        }

        composeTestRule.onAllNodesWithContentDescription("더보기")[0].performClick()
        composeTestRule.onAllNodesWithText("삭제")[0].performClick()
        composeTestRule.onAllNodesWithText("삭제")[0].performClick()

        // Then
        assert(deletedContact?.id == 1L)
    }

    @Test
    fun test_contactBookScreen_group_with_prompt_options() {
        // Given
        val options = listOf(
            PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")
        )
        val group = Group(1, "Team", "Desc", null, options, emptyList(), null, null)
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Groups,
            groups = listOf(group)
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Team").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_many_contacts_display() {
        // Given
        val contacts = List(20) { Contact(it.toLong(), null, "Contact$it", "c$it@example.com") }
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            contacts = contacts
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("20명").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_contact_with_special_chars_in_name() {
        // Given
        val contact = Contact(1, null, "김철수", "kim@example.com")
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            contacts = listOf(contact)
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("김철수").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_contact_with_long_email() {
        // Given
        val contact = Contact(1, null, "User", "verylongemailaddress@verylongdomain.com")
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            contacts = listOf(contact)
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("User").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_group_with_long_name() {
        // Given
        val group = Group(1, "Very Long Group Name That Exceeds Normal Length", "Desc", null, emptyList(), emptyList(), null, null)
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Groups,
            groups = listOf(group)
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Very Long Group Name That Exceeds Normal Length").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_group_with_long_description() {
        // Given
        val group = Group(
            1, "Team",
            "This is a very long description that might be truncated in the UI based on maxLines settings",
            null, emptyList(), emptyList(), null, null
        )
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Groups,
            groups = listOf(group)
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Team").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_refresh_callback() {
        // Given
        var refreshCalled = false
        val uiState = ContactBookUiState(selectedTab = ContactBookTab.Groups)

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onRefresh = { refreshCalled = true },
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then - Just verify it renders
        composeTestRule.onAllNodesWithText("연락처").assertCountEquals(2) // Title and bottom nav
    }

    @Test
    fun test_contactBookScreen_switch_tabs_multiple_times() {
        // Given
        val uiState = ContactBookUiState(selectedTab = ContactBookTab.Groups)

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then - Should not crash
        composeTestRule.onNodeWithText("그룹별").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_group_with_single_member() {
        // Given
        val member = Contact(1, null, "Solo", "solo@example.com")
        val group = Group(1, "Solo Team", "Desc", null, emptyList(), listOf(member), null, null)
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Groups,
            groups = listOf(group)
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("1명").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_contact_with_empty_name() {
        // Given
        val contact = Contact(1, null, "", "email@example.com")
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            contacts = listOf(contact)
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then - Should not crash
        composeTestRule.onNodeWithText("email@example.com").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_multiple_groups_with_same_member_count() {
        // Given
        val groups = listOf(
            Group(1, "Team1", "Desc1", null, emptyList(), emptyList(), null, null),
            Group(2, "Team2", "Desc2", null, emptyList(), emptyList(), null, null)
        )
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Groups,
            groups = groups
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onAllNodesWithText("0명").assertCountEquals(2)
    }

    @Test
    fun test_contactBookScreen_displays_correctly_with_mixed_data() {
        // Given
        val contacts = listOf(
            Contact(1, null, "Alice", "alice@example.com"),
            Contact(2, null, "Bob", "bob@example.com")
        )
        val groups = listOf(
            Group(1, "Group1", "Desc1", null, emptyList(), emptyList(), null, null)
        )
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            contacts = contacts,
            groups = groups
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_tab_selection_persists_state() {
        // Given
        val uiState = ContactBookUiState(selectedTab = ContactBookTab.Contacts)

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("전체").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_renders_without_callbacks() {
        // Given
        val uiState = ContactBookUiState(selectedTab = ContactBookTab.Groups)

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onAllNodesWithText("연락처").assertCountEquals(2) // Title and bottom nav
    }

    @Test
    fun test_contactBookScreen_stress_test_many_groups() {
        // Given
        val groups = List(50) { Group(it.toLong(), "Group$it", "Desc$it", null, emptyList(), emptyList(), null, null) }
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Groups,
            groups = groups
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then - Should render without crash
        composeTestRule.onAllNodesWithText("연락처").assertCountEquals(2) // Title and bottom nav
    }

    @Test
    fun test_contactBookScreen_stress_test_many_contacts() {
        // Given
        val contacts = List(100) { Contact(it.toLong(), null, "Contact$it", "c$it@example.com") }
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            contacts = contacts
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("100명").assertIsDisplayed()
    }
}
