package com.fiveis.xend.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.EmailItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmailDaoIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var emailDao: EmailDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        emailDao = database.emailDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertEmail_and_getEmailById_returns_inserted_email() = runTest {
        val email = createMockEmailItem("1")

        emailDao.insertEmail(email)

        val result = emailDao.getEmailById("1")

        assertNotNull(result)
        assertEquals(email.id, result?.id)
        assertEquals(email.subject, result?.subject)
        assertEquals(email.fromEmail, result?.fromEmail)
    }

    @Test
    fun insertEmails_and_getAllEmails_returns_all_emails() = runTest {
        val emails = listOf(
            createMockEmailItem("1", cachedAt = 3L),
            createMockEmailItem("2", cachedAt = 2L),
            createMockEmailItem("3", cachedAt = 1L)
        )

        emailDao.insertEmails(emails)

        // Add a small delay to ensure DB write is complete
        kotlinx.coroutines.delay(100)

        val result = emailDao.getAllEmails().first()

        assertEquals(3, result.size)
        // getAllEmails orders by cachedAt DESC, so highest cachedAt comes first
        assertEquals("1", result[0].id) // cachedAt = 3L
        assertEquals("2", result[1].id) // cachedAt = 2L
        assertEquals("3", result[2].id) // cachedAt = 1L
    }

    @Test
    fun insertEmails_with_onConflict_replace_updates_existing_email() = runTest {
        val email1 = createMockEmailItem("1", subject = "Original Subject")
        emailDao.insertEmail(email1)

        val email2 = createMockEmailItem("1", subject = "Updated Subject")
        emailDao.insertEmail(email2)

        val result = emailDao.getEmailById("1")

        assertNotNull(result)
        assertEquals("Updated Subject", result?.subject)
    }

    @Test
    fun getAllEmails_returns_emails_ordered_by_date_desc() = runTest {
        val email1 = createMockEmailItem("1", date = "2025-01-01T10:00:00Z", cachedAt = 1L)
        val email2 = createMockEmailItem("2", date = "2025-01-03T10:00:00Z", cachedAt = 3L)
        val email3 = createMockEmailItem("3", date = "2025-01-02T10:00:00Z", cachedAt = 2L)

        emailDao.insertEmails(listOf(email1, email2, email3))

        val result = emailDao.getAllEmails().first()

        assertEquals(3, result.size)
        assertEquals("2", result[0].id)
        assertEquals("3", result[1].id)
        assertEquals("1", result[2].id)
    }

    @Test
    fun deleteEmail_removes_email_from_database() = runTest {
        val email = createMockEmailItem("1")
        emailDao.insertEmail(email)

        emailDao.deleteEmail("1")

        val result = emailDao.getEmailById("1")
        assertNull(result)
    }

    @Test
    fun deleteAllEmails_removes_all_emails_from_database() = runTest {
        val emails = listOf(
            createMockEmailItem("1"),
            createMockEmailItem("2"),
            createMockEmailItem("3")
        )
        emailDao.insertEmails(emails)

        emailDao.deleteAllEmails()

        val result = emailDao.getAllEmails().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun updateReadStatus_changes_email_read_status() = runTest {
        val email = createMockEmailItem("1", isUnread = true)
        emailDao.insertEmail(email)

        emailDao.updateReadStatus("1", false)

        val result = emailDao.getEmailById("1")
        assertNotNull(result)
        assertEquals(false, result?.isUnread)
    }

    @Test
    fun getEmailCount_returns_correct_count() = runTest {
        val emails = listOf(
            createMockEmailItem("1"),
            createMockEmailItem("2"),
            createMockEmailItem("3")
        )
        emailDao.insertEmails(emails)

        val count = emailDao.getEmailCount()

        assertEquals(3, count)
    }

    @Test
    fun getEmailCount_returns_zero_when_no_emails() = runTest {
        // Ensure database is clean
        emailDao.deleteAllEmails()

        val count = emailDao.getEmailCount()

        assertEquals(0, count)
    }

    @Test
    fun searchEmails_by_subject_returns_matching_emails() = runTest {
        val emails = listOf(
            createMockEmailItem("1", subject = "Important Meeting"),
            createMockEmailItem("2", subject = "Weekly Report"),
            createMockEmailItem("3", subject = "Important Update")
        )
        emailDao.insertEmails(emails)

        val result = emailDao.searchEmails("Important").first()

        assertEquals(2, result.size)
        assertTrue(result.any { it.id == "1" })
        assertTrue(result.any { it.id == "3" })
    }

    @Test
    fun searchEmails_by_fromEmail_returns_matching_emails() = runTest {
        val emails = listOf(
            createMockEmailItem("1", fromEmail = "john@example.com"),
            createMockEmailItem("2", fromEmail = "jane@example.com"),
            createMockEmailItem("3", fromEmail = "john@company.com")
        )
        emailDao.insertEmails(emails)

        val result = emailDao.searchEmails("john").first()

        assertEquals(2, result.size)
        assertTrue(result.any { it.id == "1" })
        assertTrue(result.any { it.id == "3" })
    }

    @Test
    fun searchEmails_with_no_matches_returns_empty_list() = runTest {
        val emails = listOf(
            createMockEmailItem("1", subject = "Meeting"),
            createMockEmailItem("2", subject = "Report")
        )
        emailDao.insertEmails(emails)

        val result = emailDao.searchEmails("NoMatch").first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun searchEmails_is_case_insensitive() = runTest {
        val emails = listOf(
            createMockEmailItem("1", subject = "IMPORTANT Meeting"),
            createMockEmailItem("2", subject = "important Report")
        )
        emailDao.insertEmails(emails)

        val result = emailDao.searchEmails("important").first()

        assertEquals(2, result.size)
    }

    @Test
    fun getAllEmails_returns_empty_list_when_no_emails() = runTest {
        val result = emailDao.getAllEmails().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun getEmailById_returns_null_when_email_not_found() = runTest {
        val result = emailDao.getEmailById("nonexistent")

        assertNull(result)
    }

    @Test
    fun insertEmails_with_empty_list_does_not_fail() = runTest {
        emailDao.insertEmails(emptyList())

        val result = emailDao.getAllEmails().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun multiple_inserts_and_deletes_work_correctly() = runTest {
        emailDao.insertEmail(createMockEmailItem("1"))
        emailDao.insertEmail(createMockEmailItem("2"))

        var count = emailDao.getEmailCount()
        assertEquals(2, count)

        emailDao.deleteEmail("1")

        count = emailDao.getEmailCount()
        assertEquals(1, count)

        emailDao.insertEmail(createMockEmailItem("3"))

        count = emailDao.getEmailCount()
        assertEquals(2, count)

        val result = emailDao.getAllEmails().first()
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == "2" })
        assertTrue(result.any { it.id == "3" })
    }

    @Test
    fun updateReadStatus_on_nonexistent_email_does_not_fail() = runTest {
        emailDao.updateReadStatus("nonexistent", false)

        val count = emailDao.getEmailCount()
        assertEquals(0, count)
    }

    @Test
    fun deleteEmail_on_nonexistent_email_does_not_fail() = runTest {
        emailDao.deleteEmail("nonexistent")

        val count = emailDao.getEmailCount()
        assertEquals(0, count)
    }

    @Test
    fun emails_with_special_characters_are_handled_correctly() = runTest {
        val email = createMockEmailItem(
            id = "test-id-123",
            subject = "Test!@#$%^&*()_+-=[]{}|;':\",./<>?",
            fromEmail = "test+email@example.com"
        )

        emailDao.insertEmail(email)

        val result = emailDao.getEmailById("test-id-123")
        assertNotNull(result)
        assertEquals(email.subject, result?.subject)
        assertEquals(email.fromEmail, result?.fromEmail)
    }

    @Test
    fun large_number_of_emails_are_handled_correctly() = runTest {
        val emails = (1..50).map { createMockEmailItem(it.toString()) }

        emailDao.insertEmails(emails)

        val count = emailDao.getEmailCount()
        assertEquals(50, count)

        val result = emailDao.getAllEmails().first()
        assertEquals(50, result.size)
    }

    private fun createMockEmailItem(
        id: String,
        subject: String = "Subject $id",
        fromEmail: String = "sender$id@example.com",
        date: String = "2025-01-01T00:00:00Z",
        isUnread: Boolean = true,
        cachedAt: Long = System.currentTimeMillis()
    ) = EmailItem(
        id = id,
        threadId = "thread_$id",
        subject = subject,
        fromEmail = fromEmail,
        snippet = "Snippet $id",
        date = date,
        dateRaw = "Wed, 1 Jan 2025 00:00:00 +0000",
        isUnread = isUnread,
        labelIds = listOf("INBOX"),
        cachedAt = cachedAt
    )
}
