package com.fiveis.xend.ui.contactbook

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fiveis.xend.R
import com.fiveis.xend.ui.theme.StableColor

class GroupDetailActivity : ComponentActivity() {
    companion object {
        const val EXTRA_GROUP_ID = "extra_group_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val groupId = intent.getLongExtra(EXTRA_GROUP_ID, -1L)
        if (groupId < 0L) {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            return
        }

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
                val groupColor = StableColor.forId(groupId)

                val viewModelFactory = remember { GroupDetailViewModelFactory(application) }
                val vm: GroupDetailViewModel = viewModel(factory = viewModelFactory)
                LaunchedEffect(groupId) { vm.load(groupId, force = true) }
                val state by vm.uiState.collectAsStateWithLifecycle()

                GroupDetailScreen(
                    themeColor = groupColor,
                    uiState = state,
                    onBack = {
                        finish()
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    },
                    onRefresh = { vm.refresh() },
                    onMemberClick = { contact ->
                        // TODO: link to ContactDetail
                        startActivity(
                            Intent(this, ContactDetailActivity::class.java)
                                .putExtra(ContactDetailActivity.EXTRA_CONTACT_ID, contact.id)
                        )
                    },
                    onRenameGroup = { vm.renameGroup(it) },
                    onClearRenameError = { vm.clearRenameError() }
                )

                if (state.isLoading && state.group == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) { CircularProgressIndicator() }
                }
            }
        }
    }
}

private class GroupDetailViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroupDetailViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
