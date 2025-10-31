package com.fiveis.xend.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.fiveis.xend.data.database.entity.ContactContextEntity
import com.fiveis.xend.data.database.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Upsert
    suspend fun upsertContacts(contacts: List<ContactEntity>)

    @Upsert
    suspend fun upsertContexts(contexts: List<ContactContextEntity>)

    @Query("SELECT * FROM contacts")
    suspend fun getAllContacts(): List<ContactEntity>

    @Transaction
    @Query("SELECT * FROM contacts")
    suspend fun getAllWithContext(): List<ContactWithContext>

    @Transaction
    @Query("SELECT * FROM contacts WHERE id = :contactId")
    suspend fun getContactWithContext(contactId: Long): ContactWithContext?

    @Transaction
    @Query("SELECT * FROM contacts WHERE id = :contactId")
    suspend fun getByIdWithGroup(contactId: Long): ContactWithGroupAndContext?

    @Transaction
    @Query("SELECT * FROM contacts WHERE groupId = :groupId")
    suspend fun getContactsByGroupIdWithContext(groupId: Long): List<ContactWithContext>

    @Transaction
    @Query("SELECT * FROM contacts")
    fun observeAllWithContext(): Flow<List<ContactWithContext>>

    @Transaction
    @Query("SELECT * FROM contacts WHERE groupId = :groupId")
    fun observeByGroupIdWithContext(groupId: Long): Flow<List<ContactWithContext>>

    @Transaction
    @Query("SELECT * FROM contacts WHERE id = :contactId")
    fun observeByIdWithGroup(contactId: Long): Flow<ContactWithGroupAndContext?>

    @Query("DELETE FROM contacts")
    suspend fun deleteAllContacts()

    @Query("DELETE FROM contact_contexts")
    suspend fun deleteAllContexts()

    @Query("DELETE FROM contacts WHERE id = :contactId")
    suspend fun deleteById(contactId: Long)

    @Query("UPDATE contacts SET groupId = :groupId WHERE id = :contactId")
    suspend fun updateGroupId(contactId: Long, groupId: Long?)
}
