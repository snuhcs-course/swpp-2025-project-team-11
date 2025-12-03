package com.fiveis.xend.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fiveis.xend.data.model.DraftItem
import com.fiveis.xend.data.model.EmailItem
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailDao {
    @Query("SELECT * FROM emails ORDER BY cachedAt DESC")
    fun getAllEmails(): Flow<List<EmailItem>>

    @Query(
        "SELECT * FROM emails WHERE sourceLabel LIKE '%' || :label || '%' ORDER BY dateTimestamp DESC"
    )
    fun getEmailsByLabel(label: String): Flow<List<EmailItem>>

    /**
     * Get INBOX emails excluding SENT (to avoid showing self-sent emails in inbox)
     */
    @Query("SELECT * FROM emails WHERE sourceLabel LIKE '%INBOX%' ORDER BY dateTimestamp DESC")
    fun getInboxEmails(): Flow<List<EmailItem>>

    @Query("SELECT * FROM emails WHERE id = :emailId")
    suspend fun getEmailById(emailId: String): EmailItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmails(emails: List<EmailItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmail(email: EmailItem)

    @Query("DELETE FROM emails")
    suspend fun deleteAllEmails()

    @Query("DELETE FROM emails WHERE id = :emailId")
    suspend fun deleteEmail(emailId: String)

    @Query("UPDATE emails SET isUnread = :isUnread WHERE id = :emailId")
    suspend fun updateReadStatus(emailId: String, isUnread: Boolean)

    @Query("SELECT COUNT(*) FROM emails")
    suspend fun getEmailCount(): Int

    /**
     * Get the latest email's dateRaw for incremental sync.
     *
     * NOTE: Using cachedAt for sorting because dateRaw format is inconsistent
     * (ISO 8601 vs RFC 2822). This should be fixed by:
     * 1. Backend sending consistent ISO 8601 format, or
     * 2. Adding a separate timestamp field for sorting
     */
    @Query(
        "SELECT date FROM emails WHERE sourceLabel LIKE '%' || :label || '%' ORDER BY cachedAt DESC LIMIT 1"
    )
    suspend fun getLatestEmailDate(label: String): String?

    @Query("SELECT * FROM emails WHERE id IN (:ids)")
    suspend fun getEmailsByIds(ids: List<String>): List<EmailItem>

    @Query(
        """
        SELECT * FROM emails
        WHERE subject LIKE '%' || :query || '%'
        OR fromEmail LIKE '%' || :query || '%'
        ORDER BY dateTimestamp DESC
        """
    )
    fun searchEmails(query: String): Flow<List<EmailItem>>

    // Drafts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: DraftItem): Long

    @Query("SELECT * FROM drafts WHERE id = :id")
    suspend fun getDraft(id: Long): DraftItem?

    @Query(
        "SELECT * FROM drafts WHERE recipients LIKE '%\"' || :recipientEmail || '\"%' ORDER BY timestamp DESC LIMIT 1"
    )
    suspend fun getDraftByRecipient(recipientEmail: String): DraftItem?

    @Query("SELECT * FROM drafts ORDER BY timestamp DESC")
    fun getAllDrafts(): Flow<List<DraftItem>>

    @Query("DELETE FROM drafts WHERE id = :id")
    suspend fun deleteDraft(id: Long)
}
