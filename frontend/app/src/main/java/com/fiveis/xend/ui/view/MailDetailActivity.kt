package com.fiveis.xend.ui.view

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fiveis.xend.data.database.AppDatabase
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            XendTheme {
                val uiState by viewModel.uiState.collectAsState()
                MailDetailScreen(uiState = uiState, onBack = { finish() })
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
            val mailApiService = RetrofitClient.getMailApiService(context)
            val database = AppDatabase.getDatabase(context)
            val repository = InboxRepository(mailApiService, database.emailDao())
            return MailDetailViewModel(repository, messageId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
