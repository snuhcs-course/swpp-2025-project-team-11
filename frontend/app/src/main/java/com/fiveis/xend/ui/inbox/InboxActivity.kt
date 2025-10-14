package com.fiveis.xend.ui.inbox

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fiveis.xend.ui.compose.MailComposeActivity
import com.fiveis.xend.ui.theme.XendTheme

class InboxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            XendTheme {
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
