package com.fiveis.xend.ui.compose

import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.Group
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MailComposeHelperTest {

    @Test
    fun bannerState_creates_with_all_parameters() {
        val action: () -> Unit = {}
        val banner = BannerState(
            message = "Test Message",
            type = BannerType.SUCCESS,
            autoDismiss = true,
            actionText = "Action",
            onActionClick = action
        )

        assertEquals("Test Message", banner.message)
        assertEquals(BannerType.SUCCESS, banner.type)
        assertTrue(banner.autoDismiss)
        assertEquals("Action", banner.actionText)
        assertNotNull(banner.onActionClick)
    }

    @Test
    fun bannerState_creates_with_minimal_parameters() {
        val banner = BannerState(
            message = "Simple Message",
            type = BannerType.INFO
        )

        assertEquals("Simple Message", banner.message)
        assertEquals(BannerType.INFO, banner.type)
        assertFalse(banner.autoDismiss)
        assertNull(banner.actionText)
        assertNull(banner.onActionClick)
    }

    @Test
    fun bannerState_error_type() {
        val banner = BannerState(
            message = "Error occurred",
            type = BannerType.ERROR
        )

        assertEquals("Error occurred", banner.message)
        assertEquals(BannerType.ERROR, banner.type)
    }

    @Test
    fun sendUiState_default_values() {
        val state = SendUiState()

        assertFalse(state.isSending)
        assertNull(state.error)
        assertNull(state.lastSuccessMsg)
    }

    @Test
    fun sendUiState_with_sending_state() {
        val state = SendUiState(isSending = true)

        assertTrue(state.isSending)
        assertNull(state.error)
        assertNull(state.lastSuccessMsg)
    }

    @Test
    fun sendUiState_with_error() {
        val state = SendUiState(error = "Send failed")

        assertFalse(state.isSending)
        assertEquals("Send failed", state.error)
        assertNull(state.lastSuccessMsg)
    }

    @Test
    fun sendUiState_with_success_message() {
        val state = SendUiState(lastSuccessMsg = "Sent successfully")

        assertFalse(state.isSending)
        assertNull(state.error)
        assertEquals("Sent successfully", state.lastSuccessMsg)
    }

    @Test
    fun composeVmFactory_constants_are_correct() {
        assertEquals(1001, MailComposeActivity.REQUEST_CODE_COMPOSE)
    }

    @Test
    fun contact_known_vs_unknown_identification() {
        val knownContact = Contact(id = 1L, name = "Known", email = "known@example.com", group = null)
        val unknownContact = Contact(id = -1L, name = "Unknown", email = "unknown@example.com", group = null)

        assertTrue(knownContact.id >= 0L)
        assertTrue(unknownContact.id < 0L)
    }

    @Test
    fun contact_email_normalization_scenario() {
        val contact1 = Contact(id = 1L, name = "User1", email = "test@EXAMPLE.com", group = null)
        val contact2 = Contact(id = 2L, name = "User2", email = "test@example.com", group = null)

        // Emails should be normalized to lowercase for comparison
        assertEquals(
            contact1.email.lowercase().trim(),
            contact2.email.lowercase().trim()
        )
    }

    @Test
    fun contact_duplicate_detection_by_email() {
        val contacts = listOf(
            Contact(id = 1L, name = "User1", email = "test1@example.com", group = null),
            Contact(id = 2L, name = "User2", email = "test2@example.com", group = null)
        )

        val newEmail = "test1@example.com"
        val isDuplicate = contacts.any { it.email.equals(newEmail, ignoreCase = true) }

        assertTrue(isDuplicate)
    }

    @Test
    fun contact_no_duplicate_when_different_email() {
        val contacts = listOf(
            Contact(id = 1L, name = "User1", email = "test1@example.com", group = null),
            Contact(id = 2L, name = "User2", email = "test2@example.com", group = null)
        )

        val newEmail = "test3@example.com"
        val isDuplicate = contacts.any { it.email.equals(newEmail, ignoreCase = true) }

        assertFalse(isDuplicate)
    }

    @Test
    fun contact_list_empty_check() {
        val emptyList = emptyList<Contact>()
        val nonEmptyList = listOf(Contact(id = 1L, name = "User", email = "test@example.com", group = null))

        assertTrue(emptyList.isEmpty())
        assertFalse(nonEmptyList.isEmpty())
    }

    @Test
    fun contact_filter_by_email() {
        val contacts = listOf(
            Contact(id = 1L, name = "User1", email = "test1@example.com", group = null),
            Contact(id = 2L, name = "User2", email = "test2@example.com", group = null),
            Contact(id = 3L, name = "User3", email = "test3@example.com", group = null)
        )

        val emailToRemove = "test2@example.com"
        val filtered = contacts.filterNot { it.email == emailToRemove }

        assertEquals(2, filtered.size)
        assertFalse(filtered.any { it.email == emailToRemove })
    }

    @Test
    fun contact_map_to_emails() {
        val contacts = listOf(
            Contact(id = 1L, name = "User1", email = "test1@example.com", group = null),
            Contact(id = 2L, name = "User2", email = "test2@example.com", group = null)
        )

        val emails = contacts.map { it.email }

        assertEquals(2, emails.size)
        assertTrue(emails.contains("test1@example.com"))
        assertTrue(emails.contains("test2@example.com"))
    }

    @Test
    fun banner_type_enum_values() {
        val info = BannerType.INFO
        val success = BannerType.SUCCESS
        val error = BannerType.ERROR

        assertNotNull(info)
        assertNotNull(success)
        assertNotNull(error)
    }

    @Test
    fun contact_with_group() {
        val group = Group(id = 1L, name = "Work")
        val contact = Contact(
            id = 1L,
            name = "User",
            email = "test@example.com",
            group = group
        )

        assertEquals("Work", contact.group?.name)
    }

    @Test
    fun contact_without_group() {
        val contact = Contact(
            id = 1L,
            name = "User",
            email = "test@example.com",
            group = null
        )

        assertNull(contact.group)
    }

    @Test
    fun sendUiState_all_states_combination() {
        val state = SendUiState(
            isSending = true,
            error = "Error message",
            lastSuccessMsg = "Success message"
        )

        assertTrue(state.isSending)
        assertEquals("Error message", state.error)
        assertEquals("Success message", state.lastSuccessMsg)
    }

    @Test
    fun contact_email_extraction_with_angle_brackets() {
        val rawInput = "John Doe <john@example.com>"
        val emailPattern = Regex("<([^>]+)>")
        val match = emailPattern.find(rawInput)
        val extractedEmail = match?.groupValues?.getOrNull(1)?.trim() ?: rawInput

        assertEquals("john@example.com", extractedEmail)
    }

    @Test
    fun contact_email_extraction_without_angle_brackets() {
        val rawInput = "john@example.com"
        val emailPattern = Regex("<([^>]+)>")
        val match = emailPattern.find(rawInput)
        val extractedEmail = match?.groupValues?.getOrNull(1)?.trim() ?: rawInput

        assertEquals("john@example.com", extractedEmail)
    }

    @Test
    fun contact_name_or_email_display() {
        val contactWithName = Contact(id = 1L, name = "John Doe", email = "john@example.com", group = null)
        val contactWithoutName = Contact(id = -1L, name = "test@example.com", email = "test@example.com", group = null)

        assertEquals("John Doe", contactWithName.name)
        assertEquals("test@example.com", contactWithoutName.name)
    }

    @Test
    fun bannerState_with_action_only() {
        val action: () -> Unit = {}
        val banner = BannerState(
            message = "Message with action",
            type = BannerType.INFO,
            actionText = "Click Me",
            onActionClick = action
        )

        assertEquals("Click Me", banner.actionText)
        assertNotNull(banner.onActionClick)
        assertFalse(banner.autoDismiss) // default is false
    }

    @Test
    fun multiple_contacts_sorting_by_id() {
        val contacts = listOf(
            Contact(id = 3L, name = "User3", email = "test3@example.com", group = null),
            Contact(id = 1L, name = "User1", email = "test1@example.com", group = null),
            Contact(id = 2L, name = "User2", email = "test2@example.com", group = null)
        )

        val sorted = contacts.sortedBy { it.id }

        assertEquals(1L, sorted[0].id)
        assertEquals(2L, sorted[1].id)
        assertEquals(3L, sorted[2].id)
    }

    @Test
    fun contact_replace_in_list() {
        val contacts = listOf(
            Contact(id = 1L, name = "User1", email = "test1@example.com", group = null),
            Contact(id = -1L, name = "test2@example.com", email = "test2@example.com", group = null)
        )

        val newContact = Contact(id = 2L, name = "User2", email = "test2@example.com", group = null)

        val updated = contacts.map { existing ->
            if (existing.email.equals(newContact.email, ignoreCase = true)) {
                newContact
            } else {
                existing
            }
        }

        assertEquals(2L, updated[1].id)
        assertEquals("User2", updated[1].name)
    }

    @Test
    fun empty_subject_replacement() {
        val subject = ""
        val displaySubject = subject.ifBlank { "(제목 없음)" }

        assertEquals("(제목 없음)", displaySubject)
    }

    @Test
    fun non_empty_subject_no_replacement() {
        val subject = "Important Email"
        val displaySubject = subject.ifBlank { "(제목 없음)" }

        assertEquals("Important Email", displaySubject)
    }
}
