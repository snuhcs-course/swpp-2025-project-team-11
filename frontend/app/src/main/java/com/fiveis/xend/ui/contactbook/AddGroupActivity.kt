package com.fiveis.xend.ui.contactbook

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fiveis.xend.R
import com.fiveis.xend.data.model.PromptOption
import com.fiveis.xend.ui.inbox.InboxActivity

class AddGroupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                val addViewModel: AddGroupViewModel = viewModel()
                val addUiState by addViewModel.uiState.collectAsState()

                val bookViewModel: ContactBookViewModel = viewModel()
                val bookUiState by bookViewModel.uiState.collectAsState()

                // AddGroupScreen 입력값들 보관
                var name by rememberSaveable { mutableStateOf("") }
                var description by rememberSaveable { mutableStateOf("") }
                var options by rememberSaveable { mutableStateOf(emptyList<PromptOption>()) }

                AddGroupScreen(
                    uiState = addUiState,
                    onBack = {
                        finish()
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    },
                    onAdd = {
                        addViewModel.addGroup(
                            name = name,
                            description = description,
                            options = options
                        )
                    },
                    onGroupNameChange = { name = it },
                    onGroupDescriptionChange = { description = it },
                    onPromptOptionsChange = {
                        options = it.selectedTone.toList() + it.selectedFormat.toList()
                    },
                    onAddPromptOption = { key, nm, pr, onSuccess, onError ->
                        addViewModel.addPromptOption(
                            key = key,
                            name = nm,
                            prompt = pr,
                            onSuccess = onSuccess,
                            onError = onError
                        )
                    },
                    members = emptyList(),
                    onAddMember = {
                        // TODO
                    },
                    onMemberClick = {
                        // TODO
                    },
                    onBottomNavChange = { tab ->
                        if (tab == "inbox") {
                            startActivity(Intent(this, InboxActivity::class.java))
                            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        }
                    }
                )

                // 결과 피드백
                LaunchedEffect(addUiState.error, addUiState.lastSuccessMsg) {
                    addUiState.error?.let {
                        Toast.makeText(this@AddGroupActivity, it, Toast.LENGTH_SHORT).show()
                    }
                    addUiState.lastSuccessMsg?.let {
                        Toast.makeText(this@AddGroupActivity, it, Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    }
                }

                if (addUiState.isLoading) {
                    Box(Modifier.fillMaxSize()) { CircularProgressIndicator() }
                }
            }
        }
    }
}
