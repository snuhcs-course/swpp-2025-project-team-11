package com.fiveis.xend.ui.inbox

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fiveis.xend.data.repository.InboxRepository
import com.fiveis.xend.network.RetrofitClient
import com.fiveis.xend.ui.compose.MailComposeActivity
import com.fiveis.xend.ui.theme.XendTheme
import com.fiveis.xend.ui.view.MailDetailActivity

class InboxActivity : ComponentActivity() {
    private val viewModel: InboxViewModel by viewModels { InboxViewModelFactory(this.applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    onFabClick = {
                        startActivity(Intent(this@InboxActivity, MailComposeActivity::class.java))
                    },
                    onRefresh = viewModel::loadEmails,
                    onLoadMore = viewModel::loadMoreEmails
                )
            }
        }
    }
}

class InboxViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InboxViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val mailApiService = RetrofitClient.getMailApiService(context)
            val repository = InboxRepository(mailApiService)
            return InboxViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
