package com.fiveis.xend.ui.search

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
import com.fiveis.xend.ui.theme.XendTheme
import com.fiveis.xend.ui.view.MailDetailActivity

class SearchActivity : ComponentActivity() {
    private val viewModel: SearchViewModel by viewModels {
        SearchViewModelFactory(this.applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            XendTheme {
                val uiState by viewModel.uiState.collectAsState()

                SearchScreen(
                    searchQuery = uiState.query,
                    searchResults = uiState.results,
                    onQueryChange = viewModel::onQueryChange,
                    onEmailClick = { email ->
                        val intent = Intent(this, MailDetailActivity::class.java)
                        intent.putExtra("message_id", email.id)
                        val isSentMail = email.labelIds.contains("SENT")
                        intent.putExtra("is_sent_mail", isSentMail)
                        startActivity(intent)
                    },
                    onBack = { finish() }
                )
            }
        }
    }
}

class SearchViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val database = AppDatabase.getDatabase(context)
            return SearchViewModel(database.emailDao()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
