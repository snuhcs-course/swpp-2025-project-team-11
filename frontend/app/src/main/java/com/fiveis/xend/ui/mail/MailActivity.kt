package com.fiveis.xend.ui.mail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fiveis.xend.R
import com.fiveis.xend.data.database.AppDatabase
import com.fiveis.xend.data.repository.ContactBookRepository
import com.fiveis.xend.data.repository.InboxRepository
import com.fiveis.xend.data.repository.SentRepository
import com.fiveis.xend.network.RetrofitClient
import com.fiveis.xend.ui.compose.MailComposeActivity
import com.fiveis.xend.ui.contactbook.ContactBookActivity
import com.fiveis.xend.ui.inbox.AddContactDialog
import com.fiveis.xend.ui.inbox.InboxViewModel
import com.fiveis.xend.ui.search.SearchActivity
import com.fiveis.xend.ui.sent.SentViewModel
import com.fiveis.xend.ui.theme.XendTheme
import com.fiveis.xend.ui.view.MailDetailActivity

class MailActivity : ComponentActivity() {

    companion object {
        const val REQUEST_CODE_COMPOSE = 1001
    }

    private val inboxViewModel: InboxViewModel by viewModels { InboxViewModelFactory(this.applicationContext) }
    private lateinit var composeLauncher: ActivityResultLauncher<Intent> // Declare launcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        composeLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == MailComposeActivity.RESULT_DRAFT_SAVED) {
                inboxViewModel.showDraftSavedBanner()
            }
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finish()
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }
        )

        setContent {
            XendTheme {
                // Use already initialized ViewModel
                val sentViewModel: SentViewModel = viewModel(
                    factory = SentViewModelFactory(this.applicationContext)
                )

                val inboxUiState by inboxViewModel.uiState.collectAsState()
                val sentUiState by sentViewModel.uiState.collectAsState()

                MailScreen(
                    inboxUiState = inboxUiState,
                    sentUiState = sentUiState,
                    onEmailClick = {
                        val intent = Intent(this, MailDetailActivity::class.java)
                        intent.putExtra("message_id", it.id)
                        startActivity(intent)
                    },
                    onAddContactClick = { email ->
                        inboxViewModel.showAddContactDialog(email)
                    },
                    onOpenSearch = {
                        startActivity(Intent(this, SearchActivity::class.java))
                    },
                    onFabClick = {
                        composeLauncher.launch(Intent(this@MailActivity, MailComposeActivity::class.java))
                    },
                    onInboxRefresh = inboxViewModel::refreshEmails,
                    onInboxLoadMore = inboxViewModel::loadMoreEmails,
                    onSentRefresh = sentViewModel::refreshEmails,
                    onSentLoadMore = sentViewModel::loadMoreEmails,
                    onBottomNavChange = {
                        when (it) {
                            "contacts" -> {
                                startActivity(Intent(this, ContactBookActivity::class.java))
                                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                            }
                        }
                    },
                    onDismissSuccessBanner = {
                        inboxViewModel.dismissSuccessBanner()
                    },
                    showDraftSavedBanner = inboxViewModel.uiState.value.showDraftSavedBanner,
                    onDismissDraftSavedBanner = inboxViewModel::dismissDraftSavedBanner
                )

                if (inboxUiState.showAddContactDialog) {
                    inboxUiState.selectedEmailForContact?.let { email ->
                        AddContactDialog(
                            senderName = extractSenderName(email.fromEmail),
                            senderEmail = extractSenderEmail(email.fromEmail),
                            groups = inboxUiState.groups,
                            onDismiss = { inboxViewModel.dismissAddContactDialog() },
                            onConfirm = { name, emailAddr, senderRole, recipientRole, personalPrompt, groupId ->
                                inboxViewModel.addContact(
                                    name = name,
                                    email = emailAddr,
                                    senderRole = senderRole,
                                    recipientRole = recipientRole,
                                    personalPrompt = personalPrompt,
                                    groupId = groupId
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

class InboxViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InboxViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val mailApiService = RetrofitClient.getMailApiService(context)
            val database = AppDatabase.getDatabase(context)
            val repository = InboxRepository(mailApiService, database.emailDao())
            val contactRepository = ContactBookRepository(context)
            return InboxViewModel(repository, contactRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

private fun extractSenderName(fromEmail: String): String {
    val nameRegex = "(.+?)\\s*<".toRegex()
    val matchResult = nameRegex.find(fromEmail)
    val name = matchResult?.groupValues?.get(1)?.trim()
        ?: fromEmail.substringBefore("<").trim().ifEmpty { fromEmail }
    return name.trim('"', '\'')
}

private fun extractSenderEmail(fromEmail: String): String {
    val emailRegex = "<(.+?)>".toRegex()
    val matchResult = emailRegex.find(fromEmail)
    return matchResult?.groupValues?.get(1)?.trim() ?: fromEmail
}

class SentViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val mailApiService = RetrofitClient.getMailApiService(context)
            val database = AppDatabase.getDatabase(context)
            val repository = SentRepository(mailApiService, database.emailDao())
            return SentViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
