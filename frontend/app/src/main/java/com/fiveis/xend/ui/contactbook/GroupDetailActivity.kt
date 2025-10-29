package com.fiveis.xend.ui.contactbook

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

                val vm: GroupDetailViewModel = viewModel()
                LaunchedEffect(groupId) { vm.load(groupId) }
                val state by vm.uiState.collectAsState()

                GroupDetailScreen(
                    themeColor = groupColor,
                    uiState = state,
                    onBack = {
                        finish()
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    },
                    onRefresh = { vm.load(groupId, force = true) },
                    onMemberClick = { contact ->
                        // TODO: link to ContactDetail
                        // startActivity(Intent(this, ContactDetailActivity::class.java)
                        //   .putExtra(ContactDetailActivity.EXTRA_CONTACT_ID, contact.id))
                    }
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
