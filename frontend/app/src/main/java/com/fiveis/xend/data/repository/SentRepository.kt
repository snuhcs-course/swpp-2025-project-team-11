package com.fiveis.xend.data.repository

import com.fiveis.xend.data.database.EmailDao
import com.fiveis.xend.network.MailApiService

class SentRepository(
    mailApiService: MailApiService,
    emailDao: EmailDao
) : BaseMailRepository(
    mailApiService = mailApiService,
    emailDao = emailDao,
    label = "SENT",
    logTag = "SentRepository"
)
