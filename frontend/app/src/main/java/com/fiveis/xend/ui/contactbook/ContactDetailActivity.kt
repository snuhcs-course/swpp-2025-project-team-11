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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fiveis.xend.R

class ContactDetailActivity : ComponentActivity() {
    companion object {
        const val EXTRA_CONTACT_ID = "extra_contact_id"
        const val EXTRA_CONTACT_COLOR = "extra_contact_color"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val contactId = intent.getLongExtra(EXTRA_CONTACT_ID, -1L)
        if (contactId < 0L) {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            return
        }

        val contactColorInt = intent.getIntExtra(EXTRA_CONTACT_COLOR, Color.Black.toArgb())
        val contactColor = Color(contactColorInt)

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
                val vm: ContactDetailViewModel = viewModel()
                LaunchedEffect(contactId) { vm.load(contactId) }
                val state by vm.uiState.collectAsState()

                ContactDetailScreen(
                    themeColor = contactColor,
                    uiState = state,
                    onBack = {
                        finish()
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    },
                    onRefresh = { vm.load(contactId, force = true) },
                    onOpenGroup = { groupId ->
                        startActivity(
                            android.content.Intent(this, GroupDetailActivity::class.java)
                                .putExtra(GroupDetailActivity.EXTRA_GROUP_ID, groupId)
                        )
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    }
                )

                if (state.isLoading && state.contact == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) { CircularProgressIndicator() }
                }
            }
        }
    }
}
