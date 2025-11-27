package com.fiveis.xend.ui.contactbook

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.fiveis.xend.R
import com.fiveis.xend.data.repository.ContactBookTab
import com.fiveis.xend.ui.mail.MailActivity
import com.fiveis.xend.ui.theme.XendTheme

class ContactBookActivity : ComponentActivity() {
    companion object {
        const val START_TAB = "start_tab"
    }

    private val contactViewModel: ContactBookViewModel by viewModels {
        ContactBookViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val uiState = contactViewModel.uiState.value
                    if (uiState.isSearchMode) {
                        contactViewModel.closeContactSearch()
                    } else {
                        finish()
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    }
                }
            }
        )

        setContent {
            XendTheme {
                val viewModel = contactViewModel
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
                        viewModel.showContactAddedBanner()
                    }
                }
                val addGroupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK) {
                        viewModel.onTabSelected(ContactBookTab.Groups)
                        viewModel.showGroupAddedBanner()
                    }
                }

                ContactBookScreen(
                    uiState = uiState,
                    onRefresh = { viewModel.refreshAll() },
                    onTabSelected = viewModel::onTabSelected,
                    onDismissSuccessBanner = { viewModel.dismissSuccessBanner() },
                    onGroupClick = { group ->
                        startActivity(
                            Intent(this, GroupDetailActivity::class.java)
                                .putExtra(GroupDetailActivity.EXTRA_GROUP_ID, group.id)
                        )
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    },
                    onContactClick = { contact ->
                        startActivity(
                            Intent(this, ContactDetailActivity::class.java)
                                .putExtra(ContactDetailActivity.EXTRA_CONTACT_ID, contact.id)
                        )
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    },
                    onBottomNavChange = {
                        if (it == "mail") {
                            val intent = Intent(this, MailActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                            }
                            startActivity(intent)
                            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        }
                    },
                    onAddGroupClick = {
                        addGroupLauncher.launch(Intent(this, AddGroupActivity::class.java))
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    },
                    onDeleteGroupClick = {
                        viewModel.onGroupDelete(it.id)
                    },
                    onAddContactClick = {
                        addContactLauncher.launch(Intent(this, AddContactActivity::class.java))
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    },
                    onDeleteContactClick = {
                        viewModel.onContactDelete(it.id)
                    },
                    onSearchIconClick = { viewModel.startContactSearch() },
                    onSearchClose = { viewModel.closeContactSearch() },
                    onSearchQueryChange = viewModel::onContactSearchQueryChange
                )

                LaunchedEffect(uiState.error) {
                    uiState.error?.let {
                        Toast.makeText(this@ContactBookActivity, it, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
