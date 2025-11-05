package com.fiveis.xend.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fiveis.xend.data.model.EmailItem
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailDao {
    @Query("SELECT * FROM emails ORDER BY date DESC")
    fun getAllEmails(): Flow<List<EmailItem>>

    @Query("SELECT * FROM emails WHERE labelIds LIKE '%' || :label || '%' ORDER BY date DESC")
    fun getEmailsByLabel(label: String): Flow<List<EmailItem>>

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
    @Query("SELECT dateRaw FROM emails ORDER BY cachedAt DESC LIMIT 1")
    suspend fun getLatestEmailDate(): String?

    @Query(
        """
        SELECT * FROM emails
        WHERE subject LIKE '%' || :query || '%'
        OR fromEmail LIKE '%' || :query || '%'
        ORDER BY date DESC
        """
    )
    fun searchEmails(query: String): Flow<List<EmailItem>>
}
