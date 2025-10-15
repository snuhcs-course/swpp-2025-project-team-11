package com.fiveis.xend.ui.contactbook

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fiveis.xend.ui.inbox.InboxActivity

class ContactBookActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val viewModel: ContactBookViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()

                ContactBookScreen(
                    uiState = uiState,
                    onTabSelected = viewModel::onTabSelected,
                    onGroupClick = viewModel::onGroupClick,
                    onContactClick = viewModel::onContactClick,
                    onBottomNavChange = {
                        if (it == "inbox") {
                            startActivity(Intent(this, InboxActivity::class.java))
                        }
                    }
                )
            }
        }
    }
}
