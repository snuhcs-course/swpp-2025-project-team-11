package com.fiveis.xend.ui.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fiveis.xend.data.database.AppDatabase
import com.fiveis.xend.ui.compose.ContactLookupViewModel
import com.fiveis.xend.data.repository.InboxRepository
import com.fiveis.xend.network.RetrofitClient
import com.fiveis.xend.ui.theme.XendTheme

class MailDetailActivity : ComponentActivity() {
    private val viewModel: MailDetailViewModel by viewModels {
        MailDetailViewModelFactory(
            context = this.applicationContext,
            messageId = intent.getStringExtra("message_id") ?: ""
        )
    }
    private val contactLookupViewModel: ContactLookupViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ContactLookupViewModel(application) as T
            }
        }
    }

    // Re: 중복 방지 헬퍼 함수
    private fun addReplyPrefix(subject: String): String {
        return if (subject.trim().startsWith("Re:", ignoreCase = true)) {
            subject
        } else {
            "Re: $subject"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XendTheme {
                val uiState by viewModel.uiState.collectAsState()
                val knownContacts by contactLookupViewModel.byEmail.collectAsState()
                MailDetailScreen(
                    uiState = uiState,
                    knownContactsByEmail = knownContacts,
                    onBack = { finish() },
                    onReply = {
                        uiState.mail?.let { mail ->
                            val intent = Intent(this@MailDetailActivity, ReplyComposeActivity::class.java).apply {
                                putExtra("sender_email", mail.fromEmail)
                                putExtra("date", mail.date)
                                putExtra("subject", addReplyPrefix(mail.subject))
                                putExtra("body", mail.body)
                            }
                            startActivity(intent)
                        }
                    },
                    onDownloadAttachment = { attachment ->
                        viewModel.downloadAttachment(attachment)
                    },
                    onAnalyzeAttachment = { attachment ->
                        viewModel.analyzeAttachment(attachment)
                    },
                    onDismissAnalysis = {
                        viewModel.dismissAnalysisPopup()
                    },
                    onClearDownloadResult = {
                        viewModel.clearDownloadResult()
                    },
                    onPreviewAttachment = { attachment ->
                        viewModel.previewAttachment(attachment)
                    },
                    onDismissPreview = {
                        viewModel.dismissPreviewDialog()
                    },
                    onOpenAttachmentExternally = { attachment ->
                        viewModel.openAttachmentExternally(attachment)
                    },
                    onConsumeExternalOpen = {
                        viewModel.consumeExternalOpenContent()
                    },
                    onClearExternalOpenError = {
                        viewModel.clearExternalOpenError()
                    }
                )
            }
        }
    }
}

class MailDetailViewModelFactory(
    private val context: Context,
    private val messageId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MailDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val appContext = context.applicationContext
            val database = AppDatabase.getDatabase(appContext)
            val emailDao = database.emailDao()
            val mailApiService = RetrofitClient.getMailApiService(appContext)
            val repository = InboxRepository(mailApiService, emailDao)
            return MailDetailViewModel(appContext, emailDao, repository, messageId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
