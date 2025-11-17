package com.fiveis.xend.ui.sent

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
import com.fiveis.xend.R
import com.fiveis.xend.data.database.AppDatabase
import com.fiveis.xend.data.repository.SentRepository
import com.fiveis.xend.network.RetrofitClient
import com.fiveis.xend.ui.compose.MailComposeActivity
import com.fiveis.xend.ui.contactbook.ContactBookActivity
import com.fiveis.xend.ui.inbox.InboxActivity
import com.fiveis.xend.ui.search.SearchActivity
import com.fiveis.xend.ui.theme.XendTheme
import com.fiveis.xend.ui.view.MailDetailActivity

class SentActivity : ComponentActivity() {
    private val viewModel: SentViewModel by viewModels { SentViewModelFactory(this.applicationContext) }

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

                SentScreen(
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
                        startActivity(Intent(this, com.fiveis.xend.ui.profile.ProfileActivity::class.java))
                    },
                    onFabClick = {
                        startActivity(Intent(this@SentActivity, MailComposeActivity::class.java))
                    },
                    onRefresh = viewModel::refreshEmails,
                    onLoadMore = viewModel::loadMoreEmails,
                    onBottomNavChange = {
                        when (it) {
                            "inbox" -> {
                                startActivity(Intent(this, InboxActivity::class.java))
                                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                            }
                            "contacts" -> {
                                startActivity(Intent(this, ContactBookActivity::class.java))
                                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                            }
                        }
                    }
                )
            }
        }
    }
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
