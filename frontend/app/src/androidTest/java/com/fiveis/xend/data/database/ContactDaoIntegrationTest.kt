package com.fiveis.xend.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.database.entity.ContactContextEntity
import com.fiveis.xend.data.database.entity.ContactEntity
import com.fiveis.xend.data.database.entity.GroupEntity
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
class ContactDaoIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var contactDao: ContactDao
    private lateinit var groupDao: GroupDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        contactDao = database.contactDao()
        groupDao = database.groupDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun test_upsertAndGetContacts() = runTest {
        val contacts = listOf(
            ContactEntity(
                id = 1,
                email = "test1@example.com",
                name = "Test User 1",
                groupId = null
            ),
            ContactEntity(
                id = 2,
                email = "test2@example.com",
                name = "Test User 2",
                groupId = null
            )
        )

        contactDao.upsertContacts(contacts)

        val result = contactDao.getAllContacts()
        assertEquals(2, result.size)
        assertEquals("test1@example.com", result[0].email)
        assertEquals("test2@example.com", result[1].email)
    }

    @Test
    fun test_upsertContexts() = runTest {
        val contact = ContactEntity(
            id = 1,
            email = "test@example.com",
            name = "Test User",
            groupId = null
        )
        contactDao.upsertContacts(listOf(contact))

        val contexts = listOf(
            ContactContextEntity(
                contactId = 1,
                senderRole = "Manager",
                recipientRole = "Employee",
                personalPrompt = "Work relationship"
            )
        )
        contactDao.upsertContexts(contexts)

        val result = contactDao.getContactWithContext(1)
        assertNotNull(result)
        assertEquals("Manager", result?.context?.senderRole)
    }

    @Test
    fun test_getAllWithContext() = runTest {
        val contact = ContactEntity(
            id = 1,
            email = "test@example.com",
            name = "Test User",
            groupId = null
        )
        contactDao.upsertContacts(listOf(contact))

        val context = ContactContextEntity(
            contactId = 1,
            senderRole = "Friend",
            recipientRole = "Friend",
            personalPrompt = null
        )
        contactDao.upsertContexts(listOf(context))

        val result = contactDao.getAllWithContext()
        assertEquals(1, result.size)
        assertEquals("test@example.com", result[0].contact.email)
        assertEquals("Friend", result[0].context?.senderRole)
    }

    @Test
    fun test_getByIdWithGroup() = runTest {
        val group = GroupEntity(
            id = 1,
            name = "Test Group",
            emoji = "\uD83D\uDC65"
        )
        groupDao.upsertGroups(listOf(group))

        val contact = ContactEntity(
            id = 1,
            email = "test@example.com",
            name = "Test User",
            groupId = 1
        )
        contactDao.upsertContacts(listOf(contact))

        val result = contactDao.getByIdWithGroup(1)
        assertNotNull(result)
        assertEquals("test@example.com", result?.contact?.email)
        assertEquals("Test Group", result?.group?.name)
    }

    @Test
    fun test_getContactsByGroupIdWithContext() = runTest {
        // First create groups
        val groups = listOf(
            GroupEntity(id = 10, name = "Group 10", emoji = "📁"),
            GroupEntity(id = 20, name = "Group 20", emoji = "📂")
        )
        groupDao.upsertGroups(groups)

        val contacts = listOf(
            ContactEntity(id = 1, email = "user1@test.com", name = "User 1", groupId = 10),
            ContactEntity(id = 2, email = "user2@test.com", name = "User 2", groupId = 10),
            ContactEntity(id = 3, email = "user3@test.com", name = "User 3", groupId = 20)
        )
        contactDao.upsertContacts(contacts)

        val result = contactDao.getContactsByGroupIdWithContext(10)
        assertEquals(2, result.size)
        assertTrue(result.all { it.contact.groupId == 10L })
    }

    @Test
    fun test_observeAllWithContext() = runTest {
        val contact = ContactEntity(
            id = 1,
            email = "test@example.com",
            name = "Test User",
            groupId = null
        )
        contactDao.upsertContacts(listOf(contact))

        val result = contactDao.observeAllWithContext().first()
        assertEquals(1, result.size)
        assertEquals("test@example.com", result[0].contact.email)
    }

    @Test
    fun test_observeByGroupIdWithContext() = runTest {
        // First create group
        val group = GroupEntity(id = 5, name = "Group 5", emoji = "📁")
        groupDao.upsertGroups(listOf(group))

        val contacts = listOf(
            ContactEntity(id = 1, email = "user1@test.com", name = "User 1", groupId = 5),
            ContactEntity(id = 2, email = "user2@test.com", name = "User 2", groupId = 5)
        )
        contactDao.upsertContacts(contacts)

        val result = contactDao.observeByGroupIdWithContext(5).first()
        assertEquals(2, result.size)
    }

    @Test
    fun test_searchByNameOrEmail() = runTest {
        val contacts = listOf(
            ContactEntity(id = 1, email = "john@test.com", name = "John Doe", groupId = null),
            ContactEntity(id = 2, email = "jane@test.com", name = "Jane Smith", groupId = null),
            ContactEntity(id = 3, email = "bob@example.com", name = "Bob Johnson", groupId = null)
        )
        contactDao.upsertContacts(contacts)

        val result = contactDao.searchByNameOrEmail("john").first()
        assertEquals(2, result.size)
        assertTrue(result.any { it.contact.name == "John Doe" })
        assertTrue(result.any { it.contact.name == "Bob Johnson" })
    }

    @Test
    fun test_searchByEmail() = runTest {
        val contacts = listOf(
            ContactEntity(id = 1, email = "test@example.com", name = "Test User", groupId = null),
            ContactEntity(id = 2, email = "other@test.com", name = "Other User", groupId = null)
        )
        contactDao.upsertContacts(contacts)

        val result = contactDao.searchByNameOrEmail("example").first()
        assertEquals(1, result.size)
        assertEquals("test@example.com", result[0].contact.email)
    }

    @Test
    fun test_deleteAllContacts() = runTest {
        val contacts = listOf(
            ContactEntity(id = 1, email = "test1@test.com", name = "User 1", groupId = null),
            ContactEntity(id = 2, email = "test2@test.com", name = "User 2", groupId = null)
        )
        contactDao.upsertContacts(contacts)

        contactDao.deleteAllContacts()

        val result = contactDao.getAllContacts()
        assertEquals(0, result.size)
    }

    @Test
    fun test_deleteAllContexts() = runTest {
        val contact = ContactEntity(
            id = 1,
            email = "test@example.com",
            name = "Test User",
            groupId = null
        )
        contactDao.upsertContacts(listOf(contact))

        val context = ContactContextEntity(
            contactId = 1,
            senderRole = "Friend",
            recipientRole = "Friend",
            personalPrompt = null
        )
        contactDao.upsertContexts(listOf(context))

        contactDao.deleteAllContexts()

        val result = contactDao.getContactWithContext(1)
        assertNotNull(result)
        assertNull(result?.context)
    }

    @Test
    fun test_deleteById() = runTest {
        val contacts = listOf(
            ContactEntity(id = 1, email = "test1@test.com", name = "User 1", groupId = null),
            ContactEntity(id = 2, email = "test2@test.com", name = "User 2", groupId = null)
        )
        contactDao.upsertContacts(contacts)

        contactDao.deleteById(1)

        val result = contactDao.getAllContacts()
        assertEquals(1, result.size)
        assertEquals(2L, result[0].id)
    }

    @Test
    fun test_updateGroupId() = runTest {
        // First create group
        val group = GroupEntity(id = 100, name = "Group 100", emoji = "📁")
        groupDao.upsertGroups(listOf(group))

        val contact = ContactEntity(
            id = 1,
            email = "test@example.com",
            name = "Test User",
            groupId = null
        )
        contactDao.upsertContacts(listOf(contact))

        contactDao.updateGroupId(1, 100)

        val result = contactDao.getAllContacts()
        assertEquals(100L, result[0].groupId)
    }

    @Test
    fun test_updateGroupIdToNull() = runTest {
        // First create group
        val group = GroupEntity(id = 50, name = "Group 50", emoji = "📁")
        groupDao.upsertGroups(listOf(group))

        val contact = ContactEntity(
            id = 1,
            email = "test@example.com",
            name = "Test User",
            groupId = 50
        )
        contactDao.upsertContacts(listOf(contact))

        contactDao.updateGroupId(1, null)

        val result = contactDao.getAllContacts()
        assertNull(result[0].groupId)
    }
}
