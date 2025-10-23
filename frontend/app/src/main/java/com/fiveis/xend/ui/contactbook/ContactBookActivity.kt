package com.fiveis.xend.ui.contactbook

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fiveis.xend.R
import com.fiveis.xend.data.repository.ContactBookTab
import com.fiveis.xend.ui.inbox.InboxActivity

class ContactBookActivity : ComponentActivity() {
    companion object {
        const val START_TAB = "start_tab"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finish()
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                }
            }
        )

        setContent {
            MaterialTheme {
                val viewModel: ContactBookViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()

                val startTab = intent.getStringExtra(START_TAB)
                LaunchedEffect(startTab) {
                    startTab?.let {
                        viewModel.onTabSelected(ContactBookTab.Contacts)
                    }
                }

                val context = LocalContext.current
                val addContactLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK) {
                        viewModel.onTabSelected(ContactBookTab.Contacts)
                    }
                }
                val addGroupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK) {
                        viewModel.onTabSelected(ContactBookTab.Groups)
                    }
                }

                ContactBookScreen(
                    uiState = uiState,
                    onTabSelected = viewModel::onTabSelected,
                    onGroupClick = viewModel::onGroupClick,
                    onContactClick = viewModel::onContactClick,
                    onBottomNavChange = {
                        if (it == "inbox") {
                            startActivity(Intent(this, InboxActivity::class.java))
                            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        }
                    },
                    onAddGroupClick = {
                        addGroupLauncher.launch(Intent(this, AddGroupActivity::class.java))
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    },
                    onAddContactClick = {
                        addContactLauncher.launch(Intent(this, AddContactActivity::class.java))
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    }
                )
            }
        }
    }
}
