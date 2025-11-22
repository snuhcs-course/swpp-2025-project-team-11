package com.fiveis.xend.data.repository

import com.fiveis.xend.data.database.EmailDao
import com.fiveis.xend.data.model.DraftItem
import com.fiveis.xend.network.MailApiService
import kotlinx.coroutines.flow.Flow

class InboxRepository(
    mailApiService: MailApiService,
    emailDao: EmailDao
) : BaseMailRepository(
    mailApiService = mailApiService,
    emailDao = emailDao,
    label = "INBOX",
    logTag = "InboxRepository"
) {
    // Drafts operations - specific to inbox only
    suspend fun saveDraft(draft: DraftItem): Long {
        return emailDao.insertDraft(draft)
    }

    suspend fun getDraft(id: Long): DraftItem? {
        return emailDao.getDraft(id)
    }

    suspend fun getDraftByRecipient(recipientEmail: String): DraftItem? {
        return emailDao.getDraftByRecipient(recipientEmail)
    }

    fun getAllDrafts(): Flow<List<DraftItem>> {
        return emailDao.getAllDrafts()
    }

    suspend fun deleteDraft(id: Long) {
        emailDao.deleteDraft(id)
    }
}
