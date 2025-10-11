package com.fiveis.xend

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fiveis.xend.ui.inbox.InboxScreen
import com.fiveis.xend.ui.inbox.InboxViewModel

class InboxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val viewModel: InboxViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()

                InboxScreen(
                    uiState = uiState,
                    onTabSelected = viewModel::onTabSelected,
                    onEmailClick = viewModel::onEmailClick,
                    onFabClick = {
                        startActivity(Intent(this, MailComposeActivity::class.java))
                    }
                )
            }
        }
    }
}
