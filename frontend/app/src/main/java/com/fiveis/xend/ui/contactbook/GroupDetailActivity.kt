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

class GroupDetailActivity : ComponentActivity() {
    companion object {
        const val EXTRA_GROUP_ID = "extra_group_id"
        const val EXTRA_GROUP_COLOR = "extra_group_color"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val groupId = intent.getLongExtra(EXTRA_GROUP_ID, -1L)
        require(groupId > 0) { "GroupDetailActivity requires EXTRA_GROUP_ID" }

        val groupColorInt = intent.getIntExtra(EXTRA_GROUP_COLOR, Color.Black.toArgb())
        val groupColor = Color(groupColorInt)

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
                    Box(Modifier.fillMaxSize()) { CircularProgressIndicator() }
                }
            }
        }
    }
}
