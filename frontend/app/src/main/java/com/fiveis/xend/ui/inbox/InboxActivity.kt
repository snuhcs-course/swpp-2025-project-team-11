package com.fiveis.xend.ui.inbox

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.fiveis.xend.network.RetrofitClient
import com.fiveis.xend.ui.compose.MailComposeActivity
import com.fiveis.xend.ui.contactbook.ContactBookActivity
import com.fiveis.xend.ui.profile.ProfileActivity
import com.fiveis.xend.ui.search.SearchActivity
import com.fiveis.xend.ui.sent.SentActivity
import com.fiveis.xend.ui.theme.XendTheme
import com.fiveis.xend.ui.view.MailDetailActivity
import com.fiveis.xend.utils.EmailUtils

class InboxActivity : ComponentActivity() {
    private val viewModel: InboxViewModel by viewModels { InboxViewModelFactory(this.applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                val uiState by viewModel.uiState.collectAsState()

                InboxScreen(
                    uiState = uiState,
                    onEmailClick = {
                        val intent = Intent(this, MailDetailActivity::class.java)
                        intent.putExtra("message_id", it.id)
                        startActivity(intent)
                    },
                    onOpenSearch = {
                        startActivity(Intent(this, SearchActivity::class.java))
                    },
                    onOpenProfile = {
                        android.util.Log.d("InboxActivity", "Profile button clicked")
                        startActivity(Intent(this, ProfileActivity::class.java))
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    },
                    onFabClick = {
                        startActivity(Intent(this@InboxActivity, MailComposeActivity::class.java))
                    },
                    onRefresh = viewModel::refreshEmails,
                    onLoadMore = viewModel::loadMoreEmails,
                    onBottomNavChange = {
                        when (it) {
                            "sent" -> {
                                startActivity(Intent(this, SentActivity::class.java))
                                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                            }
                            "contacts" -> {
                                startActivity(Intent(this, ContactBookActivity::class.java))
                                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                            }
                        }
                    },
                    onAddContactClick = { email ->
                        viewModel.showAddContactDialog(email)
                    },
                    onDismissSuccessBanner = {
                        viewModel.dismissSuccessBanner()
                    }
                )

                // Show Add Contact Dialog
                if (uiState.showAddContactDialog) {
                    uiState.selectedEmailForContact?.let { email ->
                        AddContactDialog(
                            senderName = EmailUtils.extractSenderName(email.fromEmail),
                            senderEmail = EmailUtils.extractEmailAddress(email.fromEmail),
                            groups = uiState.groups,
                            onDismiss = { viewModel.dismissAddContactDialog() },
                            onConfirm = { name, emailAddr, senderRole, recipientRole, personalPrompt, groupId ->
                                viewModel.addContact(
                                    name,
                                    emailAddr,
                                    senderRole,
                                    recipientRole,
                                    personalPrompt,
                                    groupId
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
            val inboxRepository = InboxRepository(mailApiService, database.emailDao())
            val contactRepository = ContactBookRepository(context)
            return InboxViewModel(inboxRepository, contactRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
