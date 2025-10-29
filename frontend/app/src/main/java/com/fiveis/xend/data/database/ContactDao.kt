package com.fiveis.xend.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.fiveis.xend.data.database.entity.ContactContextEntity
import com.fiveis.xend.data.database.entity.ContactEntity

@Dao
interface ContactDao {
    @Upsert
    suspend fun upsertContacts(contacts: List<ContactEntity>)

    @Upsert
    suspend fun upsertContexts(contexts: List<ContactContextEntity>)

    @Query("SELECT * FROM contacts")
    suspend fun getAllContacts(): List<ContactEntity>

    @Transaction
    @Query("SELECT * FROM contacts WHERE groupId = :groupId")
    suspend fun getContactsByGroupIdWithContext(groupId: Long): List<ContactWithContext>

    @Transaction
    @Query("SELECT * FROM contacts WHERE id = :contactId")
    suspend fun getContactWithContext(contactId: Long): ContactWithContext?

    @Query("DELETE FROM contacts")
    suspend fun deleteAllContacts()

    @Query("DELETE FROM contact_contexts")
    suspend fun deleteAllContexts()
}
