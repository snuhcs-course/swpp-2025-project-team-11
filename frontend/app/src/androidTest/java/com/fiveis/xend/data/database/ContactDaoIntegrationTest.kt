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
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertContacts_inserts_new_contacts() = runTest {
        val contacts = listOf(
            createMockContact(1L, "John Doe", "john@example.com"),
            createMockContact(2L, "Jane Smith", "jane@example.com")
        )

        contactDao.upsertContacts(contacts)

        val result = contactDao.getAllContacts()
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == 1L })
        assertTrue(result.any { it.id == 2L })
    }

    @Test
    fun upsertContacts_updates_existing_contacts() = runTest {
        val contact1 = createMockContact(1L, "John Doe", "john@example.com")
        contactDao.upsertContacts(listOf(contact1))

        val contact2 = createMockContact(1L, "John Updated", "john@example.com")
        contactDao.upsertContacts(listOf(contact2))

        val result = contactDao.getAllContacts()
        assertEquals(1, result.size)
        assertEquals("John Updated", result[0].name)
    }

    @Test
    fun upsertContacts_with_empty_list_does_not_fail() = runTest {
        contactDao.upsertContacts(emptyList())

        val result = contactDao.getAllContacts()
        assertTrue(result.isEmpty())
    }

    @Test
    fun upsertContexts_inserts_new_contexts() = runTest {
        val contexts = listOf(
            createMockContext(1L, "Manager", "Employee"),
            createMockContext(2L, "Teacher", "Student")
        )

        contactDao.upsertContexts(contexts)

        val contact1 = createMockContact(1L, "John", "john@example.com")
        val contact2 = createMockContact(2L, "Jane", "jane@example.com")
        contactDao.upsertContacts(listOf(contact1, contact2))

        val result = contactDao.getAllWithContext()
        assertEquals(2, result.size)
        assertNotNull(result.find { it.contact.id == 1L }?.context)
        assertNotNull(result.find { it.contact.id == 2L }?.context)
    }

    @Test
    fun upsertContexts_updates_existing_contexts() = runTest {
        val context1 = createMockContext(1L, "Manager", "Employee")
        contactDao.upsertContexts(listOf(context1))

        val context2 = createMockContext(1L, "Director", "Employee")
        contactDao.upsertContexts(listOf(context2))

        val contact = createMockContact(1L, "John", "john@example.com")
        contactDao.upsertContacts(listOf(contact))

        val result = contactDao.getContactWithContext(1L)
        assertNotNull(result)
        assertEquals("Director", result?.context?.senderRole)
    }

    @Test
    fun getAllContacts_returns_all_contacts() = runTest {
        val contacts = listOf(
            createMockContact(1L, "John", "john@example.com"),
            createMockContact(2L, "Jane", "jane@example.com"),
            createMockContact(3L, "Bob", "bob@example.com")
        )
        contactDao.upsertContacts(contacts)

        val result = contactDao.getAllContacts()

        assertEquals(3, result.size)
        assertEquals("John", result.find { it.id == 1L }?.name)
        assertEquals("Jane", result.find { it.id == 2L }?.name)
        assertEquals("Bob", result.find { it.id == 3L }?.name)
    }

    @Test
    fun getAllContacts_returns_empty_list_when_no_contacts() = runTest {
        val result = contactDao.getAllContacts()

        assertTrue(result.isEmpty())
    }

    @Test
    fun getAllWithContext_returns_contacts_with_contexts() = runTest {
        val contacts = listOf(
            createMockContact(1L, "John", "john@example.com"),
            createMockContact(2L, "Jane", "jane@example.com")
        )
        val contexts = listOf(
            createMockContext(1L, "Manager", "Employee")
        )

        contactDao.upsertContacts(contacts)
        contactDao.upsertContexts(contexts)

        val result = contactDao.getAllWithContext()

        assertEquals(2, result.size)
        assertNotNull(result.find { it.contact.id == 1L }?.context)
        assertNull(result.find { it.contact.id == 2L }?.context)
    }

    @Test
    fun getAllWithContext_returns_empty_list_when_no_contacts() = runTest {
        val result = contactDao.getAllWithContext()

        assertTrue(result.isEmpty())
    }

    @Test
    fun getContactWithContext_returns_contact_with_context() = runTest {
        val contact = createMockContact(1L, "John", "john@example.com")
        val context = createMockContext(1L, "Manager", "Employee")

        contactDao.upsertContacts(listOf(contact))
        contactDao.upsertContexts(listOf(context))

        val result = contactDao.getContactWithContext(1L)

        assertNotNull(result)
        assertEquals(1L, result?.contact?.id)
        assertEquals("John", result?.contact?.name)
        assertEquals("Manager", result?.context?.senderRole)
    }

    @Test
    fun getContactWithContext_returns_contact_without_context() = runTest {
        val contact = createMockContact(1L, "John", "john@example.com")
        contactDao.upsertContacts(listOf(contact))

        val result = contactDao.getContactWithContext(1L)

        assertNotNull(result)
        assertEquals(1L, result?.contact?.id)
        assertNull(result?.context)
    }

    @Test
    fun getContactWithContext_returns_null_when_contact_not_found() = runTest {
        val result = contactDao.getContactWithContext(999L)

        assertNull(result)
    }

    @Test
    fun getByIdWithGroup_returns_contact_with_group() = runTest {
        val group = createMockGroup(1L, "Team A")
        groupDao.upsertGroups(listOf(group))

        val contact = createMockContact(1L, "John", "john@example.com", groupId = 1L)
        contactDao.upsertContacts(listOf(contact))

        val result = contactDao.getByIdWithGroup(1L)

        assertNotNull(result)
        assertEquals(1L, result?.contact?.id)
        assertEquals("John", result?.contact?.name)
        assertNotNull(result?.group)
        assertEquals("Team A", result?.group?.name)
    }

    @Test
    fun getByIdWithGroup_returns_contact_without_group() = runTest {
        val contact = createMockContact(1L, "John", "john@example.com", groupId = null)
        contactDao.upsertContacts(listOf(contact))

        val result = contactDao.getByIdWithGroup(1L)

        assertNotNull(result)
        assertEquals(1L, result?.contact?.id)
        assertNull(result?.group)
    }

    @Test
    fun getByIdWithGroup_returns_null_when_contact_not_found() = runTest {
        val result = contactDao.getByIdWithGroup(999L)

        assertNull(result)
    }

    @Test
    fun getByIdWithGroup_returns_contact_with_context_and_group() = runTest {
        val group = createMockGroup(1L, "Team A")
        groupDao.upsertGroups(listOf(group))

        val contact = createMockContact(1L, "John", "john@example.com", groupId = 1L)
        val context = createMockContext(1L, "Manager", "Employee")

        contactDao.upsertContacts(listOf(contact))
        contactDao.upsertContexts(listOf(context))

        val result = contactDao.getByIdWithGroup(1L)

        assertNotNull(result)
        assertEquals(1L, result?.contact?.id)
        assertNotNull(result?.context)
        assertEquals("Manager", result?.context?.senderRole)
        assertNotNull(result?.group)
        assertEquals("Team A", result?.group?.name)
    }

    @Test
    fun getContactsByGroupIdWithContext_returns_contacts_in_group() = runTest {
        val groups = listOf(
            createMockGroup(1L, "Team A"),
            createMockGroup(2L, "Team B")
        )
        groupDao.upsertGroups(groups)

        val contacts = listOf(
            createMockContact(1L, "John", "john@example.com", groupId = 1L),
            createMockContact(2L, "Jane", "jane@example.com", groupId = 1L),
            createMockContact(3L, "Bob", "bob@example.com", groupId = 2L)
        )
        contactDao.upsertContacts(contacts)

        val result = contactDao.getContactsByGroupIdWithContext(1L)

        assertEquals(2, result.size)
        assertTrue(result.any { it.contact.id == 1L })
        assertTrue(result.any { it.contact.id == 2L })
    }

    @Test
    fun getContactsByGroupIdWithContext_returns_empty_list_when_no_contacts_in_group() = runTest {
        val result = contactDao.getContactsByGroupIdWithContext(999L)

        assertTrue(result.isEmpty())
    }

    @Test
    fun getContactsByGroupIdWithContext_includes_contexts() = runTest {
        val groups = listOf(
            createMockGroup(1L, "Team A")
        )
        groupDao.upsertGroups(groups)

        val contacts = listOf(
            createMockContact(1L, "John", "john@example.com", groupId = 1L),
            createMockContact(2L, "Jane", "jane@example.com", groupId = 1L)
        )
        val contexts = listOf(
            createMockContext(1L, "Manager", "Employee")
        )

        contactDao.upsertContacts(contacts)
        contactDao.upsertContexts(contexts)

        val result = contactDao.getContactsByGroupIdWithContext(1L)

        assertEquals(2, result.size)
        assertNotNull(result.find { it.contact.id == 1L }?.context)
        assertNull(result.find { it.contact.id == 2L }?.context)
    }

    @Test
    fun observeAllWithContext_emits_contacts() = runTest {
        val contacts = listOf(
            createMockContact(1L, "John", "john@example.com"),
            createMockContact(2L, "Jane", "jane@example.com")
        )
        contactDao.upsertContacts(contacts)

        val result = contactDao.observeAllWithContext().first()

        assertEquals(2, result.size)
    }

    @Test
    fun observeAllWithContext_emits_updates_when_contacts_change() = runTest {
        val contacts1 = listOf(createMockContact(1L, "John", "john@example.com"))
        contactDao.upsertContacts(contacts1)

        var result = contactDao.observeAllWithContext().first()
        assertEquals(1, result.size)

        val contacts2 = listOf(createMockContact(2L, "Jane", "jane@example.com"))
        contactDao.upsertContacts(contacts2)

        result = contactDao.observeAllWithContext().first()
        assertEquals(2, result.size)
    }

    @Test
    fun observeByGroupIdWithContext_emits_contacts_in_group() = runTest {
        val groups = listOf(
            createMockGroup(1L, "Team A")
        )
        groupDao.upsertGroups(groups)

        val contacts = listOf(
            createMockContact(1L, "John", "john@example.com", groupId = 1L),
            createMockContact(2L, "Jane", "jane@example.com", groupId = 1L)
        )
        contactDao.upsertContacts(contacts)

        val result = contactDao.observeByGroupIdWithContext(1L).first()

        assertEquals(2, result.size)
    }

    @Test
    fun observeByGroupIdWithContext_emits_empty_list_when_no_contacts() = runTest {
        val result = contactDao.observeByGroupIdWithContext(999L).first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun observeByIdWithGroup_emits_contact() = runTest {
        val groups = listOf(
            createMockGroup(1L, "Team A")
        )
        groupDao.upsertGroups(groups)

        val contact = createMockContact(1L, "John", "john@example.com", groupId = 1L)
        contactDao.upsertContacts(listOf(contact))

        val result = contactDao.observeByIdWithGroup(1L).first()

        assertNotNull(result)
        assertEquals(1L, result?.contact?.id)
        assertEquals("Team A", result?.group?.name)
    }

    @Test
    fun observeByIdWithGroup_emits_null_when_contact_not_found() = runTest {
        val result = contactDao.observeByIdWithGroup(999L).first()

        assertNull(result)
    }

    @Test
    fun deleteAllContacts_removes_all_contacts() = runTest {
        val contacts = listOf(
            createMockContact(1L, "John", "john@example.com"),
            createMockContact(2L, "Jane", "jane@example.com")
        )
        contactDao.upsertContacts(contacts)

        contactDao.deleteAllContacts()

        val result = contactDao.getAllContacts()
        assertTrue(result.isEmpty())
    }

    @Test
    fun deleteAllContacts_on_empty_database_does_not_fail() = runTest {
        contactDao.deleteAllContacts()

        val result = contactDao.getAllContacts()
        assertTrue(result.isEmpty())
    }

    @Test
    fun deleteAllContexts_removes_all_contexts() = runTest {
        val contacts = listOf(
            createMockContact(1L, "John", "john@example.com"),
            createMockContact(2L, "Jane", "jane@example.com")
        )
        val contexts = listOf(
            createMockContext(1L, "Manager", "Employee"),
            createMockContext(2L, "Teacher", "Student")
        )

        contactDao.upsertContacts(contacts)
        contactDao.upsertContexts(contexts)

        contactDao.deleteAllContexts()

        val result = contactDao.getAllWithContext()
        assertEquals(2, result.size)
        assertNull(result[0].context)
        assertNull(result[1].context)
    }

    @Test
    fun deleteAllContexts_on_empty_database_does_not_fail() = runTest {
        contactDao.deleteAllContexts()

        val result = contactDao.getAllWithContext()
        assertTrue(result.isEmpty())
    }

    @Test
    fun deleteById_removes_specific_contact() = runTest {
        val contacts = listOf(
            createMockContact(1L, "John", "john@example.com"),
            createMockContact(2L, "Jane", "jane@example.com")
        )
        contactDao.upsertContacts(contacts)

        contactDao.deleteById(1L)

        val result = contactDao.getAllContacts()
        assertEquals(1, result.size)
        assertEquals(2L, result[0].id)
    }

    @Test
    fun deleteById_on_nonexistent_contact_does_not_fail() = runTest {
        contactDao.deleteById(999L)

        val result = contactDao.getAllContacts()
        assertTrue(result.isEmpty())
    }

    @Test
    fun updateGroupId_changes_contact_group() = runTest {
        val groups = listOf(
            createMockGroup(1L, "Team A"),
            createMockGroup(2L, "Team B")
        )
        groupDao.upsertGroups(groups)

        val contact = createMockContact(1L, "John", "john@example.com", groupId = 1L)
        contactDao.upsertContacts(listOf(contact))

        contactDao.updateGroupId(1L, 2L)

        val result = contactDao.getByIdWithGroup(1L)
        assertNotNull(result)
        assertEquals(2L, result?.contact?.groupId)
        assertEquals("Team B", result?.group?.name)
    }

    @Test
    fun updateGroupId_to_null_removes_group_association() = runTest {
        val groups = listOf(
            createMockGroup(1L, "Team A")
        )
        groupDao.upsertGroups(groups)

        val contact = createMockContact(1L, "John", "john@example.com", groupId = 1L)
        contactDao.upsertContacts(listOf(contact))

        contactDao.updateGroupId(1L, null)

        val result = contactDao.getByIdWithGroup(1L)
        assertNotNull(result)
        assertNull(result?.contact?.groupId)
        assertNull(result?.group)
    }

    @Test
    fun updateGroupId_on_nonexistent_contact_does_not_fail() = runTest {
        contactDao.updateGroupId(999L, 1L)

        val result = contactDao.getAllContacts()
        assertTrue(result.isEmpty())
    }

    @Test
    fun contacts_with_special_characters_are_handled_correctly() = runTest {
        val contact = createMockContact(
            1L,
            "O'Brien, Jr.",
            "test+email@example.com"
        )

        contactDao.upsertContacts(listOf(contact))

        val result = contactDao.getAllContacts()
        assertEquals(1, result.size)
        assertEquals("O'Brien, Jr.", result[0].name)
        assertEquals("test+email@example.com", result[0].email)
    }

    @Test
    fun large_number_of_contacts_are_handled_correctly() = runTest {
        val contacts = (1L..100L).map {
            createMockContact(it, "Contact $it", "contact$it@example.com")
        }

        contactDao.upsertContacts(contacts)

        val result = contactDao.getAllContacts()
        assertEquals(100, result.size)
    }

    @Test
    fun context_with_all_fields_populated_is_stored_correctly() = runTest {
        val context = ContactContextEntity(
            contactId = 1L,
            senderRole = "Manager",
            recipientRole = "Employee",
            relationshipDetails = "Direct report",
            personalPrompt = "Use formal language",
            languagePreference = "en-US",
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )
        val contact = createMockContact(1L, "John", "john@example.com")

        contactDao.upsertContexts(listOf(context))
        contactDao.upsertContacts(listOf(contact))

        val result = contactDao.getContactWithContext(1L)

        assertNotNull(result)
        assertEquals("Manager", result?.context?.senderRole)
        assertEquals("Employee", result?.context?.recipientRole)
        assertEquals("Direct report", result?.context?.relationshipDetails)
        assertEquals("Use formal language", result?.context?.personalPrompt)
        assertEquals("en-US", result?.context?.languagePreference)
    }

    @Test
    fun context_with_null_fields_is_stored_correctly() = runTest {
        val context = ContactContextEntity(
            contactId = 1L,
            senderRole = null,
            recipientRole = null,
            relationshipDetails = null,
            personalPrompt = null,
            languagePreference = null,
            createdAt = null,
            updatedAt = null
        )
        val contact = createMockContact(1L, "John", "john@example.com")

        contactDao.upsertContexts(listOf(context))
        contactDao.upsertContacts(listOf(contact))

        val result = contactDao.getContactWithContext(1L)

        assertNotNull(result)
        assertNull(result?.context?.senderRole)
        assertNull(result?.context?.recipientRole)
        assertNull(result?.context?.relationshipDetails)
    }

    private fun createMockContact(
        id: Long,
        name: String,
        email: String,
        groupId: Long? = null
    ) = ContactEntity(
        id = id,
        groupId = groupId,
        name = name,
        email = email,
        createdAt = "2025-01-01T00:00:00Z",
        updatedAt = "2025-01-01T00:00:00Z"
    )

    private fun createMockContext(
        contactId: Long,
        senderRole: String,
        recipientRole: String
    ) = ContactContextEntity(
        contactId = contactId,
        senderRole = senderRole,
        recipientRole = recipientRole,
        relationshipDetails = "Professional",
        personalPrompt = "Be formal",
        languagePreference = "en-US",
        createdAt = "2025-01-01T00:00:00Z",
        updatedAt = "2025-01-01T00:00:00Z"
    )

    private fun createMockGroup(
        id: Long,
        name: String
    ) = GroupEntity(
        id = id,
        name = name,
        description = "Description for $name",
        createdAt = "2025-01-01T00:00:00Z",
        updatedAt = "2025-01-01T00:00:00Z"
    )
}
