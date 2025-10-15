package com.fiveis.xend.ui.inbox

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fiveis.xend.R
import com.fiveis.xend.ui.compose.MailComposeActivity
import com.fiveis.xend.ui.contactbook.ContactBookActivity

class InboxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            MaterialTheme {
                val viewModel: InboxViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()

                InboxScreen(
                    uiState = uiState,
                    onTabSelected = viewModel::onTabSelected,
                    onEmailClick = viewModel::onEmailClick,
                    onFabClick = {
                        startActivity(Intent(this, MailComposeActivity::class.java))
                    },
                    onBottomNavChange = {
                        if (it == "contacts") {
                            startActivity(Intent(this, ContactBookActivity::class.java))
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        }
                    }
                )
            }
        }
    }
}
