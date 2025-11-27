package com.fiveis.xend.ui.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fiveis.xend.data.database.AppDatabase
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.repository.ContactBookRepository
import com.fiveis.xend.data.repository.InboxRepository
import com.fiveis.xend.network.RetrofitClient
import com.fiveis.xend.ui.compose.ContactLookupViewModel
import com.fiveis.xend.ui.inbox.AddContactDialog
import com.fiveis.xend.ui.theme.XendTheme
import kotlinx.coroutines.launch

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
        val appContext = applicationContext
        setContent {
            XendTheme {
                val uiState by viewModel.uiState.collectAsState()
                val knownContacts by contactLookupViewModel.byEmail.collectAsState()
                val contactRepository = remember { ContactBookRepository(appContext) }
                val coroutineScope = rememberCoroutineScope()
                var showAddContactDialog by remember { mutableStateOf(false) }
                var pendingContactName by remember { mutableStateOf("") }
                var pendingContactEmail by remember { mutableStateOf("") }
                var availableGroups by remember { mutableStateOf<List<Group>>(emptyList()) }
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                    contactRepository.observeGroups().collect { groups ->
                        availableGroups = groups
                    }
                }
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
                    },
                    onAddContactClick = { name, email ->
                        pendingContactName = name
                        pendingContactEmail = email
                        showAddContactDialog = true
                    }
                )

                if (showAddContactDialog) {
                    AddContactDialog(
                        senderName = pendingContactName.ifBlank { pendingContactEmail },
                        senderEmail = pendingContactEmail,
                        groups = availableGroups,
                        onDismiss = { showAddContactDialog = false },
                        onConfirm = { name, email, senderRole, recipientRole, personalPrompt, groupId, language ->
                            coroutineScope.launch {
                                try {
                                    contactRepository.addContact(
                                        name = name,
                                        email = email,
                                        groupId = groupId,
                                        senderRole = senderRole,
                                        recipientRole = recipientRole,
                                        personalPrompt = personalPrompt,
                                        languagePreference = language
                                    )
                                    // 연락처 추가 성공 - 다이얼로그 닫기
                                    showAddContactDialog = false
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "연락처 추가 실패: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    )
                }
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
