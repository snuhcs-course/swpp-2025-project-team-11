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
import androidx.compose.runtime.remember
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
                val addViewModel: AddGroupViewModel = viewModel(
                    factory = AddGroupViewModel.Factory(application)
                )
                val addUiState by addViewModel.uiState.collectAsState()

                val bookViewModel: ContactBookViewModel = viewModel(
                    factory = ContactBookViewModel.Factory(application)
                )
                val bookUiState by bookViewModel.uiState.collectAsState()

                // 연락처 목록 로드
                LaunchedEffect(Unit) {
                    bookViewModel.onTabSelected(com.fiveis.xend.data.repository.ContactBookTab.Contacts)
                }

                // AddGroupScreen 입력값들 보관
                var name by rememberSaveable { mutableStateOf("") }
                var description by rememberSaveable { mutableStateOf("") }
                var emoji by rememberSaveable { mutableStateOf<String?>(null) }
                var options by rememberSaveable { mutableStateOf(emptyList<PromptOption>()) }
                var members by remember { mutableStateOf(emptyList<com.fiveis.xend.data.model.Contact>()) }
                var showContactSelectDialog by remember { mutableStateOf(false) }

                val promptingState = remember(options) {
                    val tones = options.filter {
                        it.key.equals("tone", ignoreCase = true)
                    }.toSet()
                    val formats = options.filter {
                        it.key.equals("format", ignoreCase = true)
                    }.toSet()
                    PromptingUiState(selectedTone = tones, selectedFormat = formats)
                }

                AddGroupScreen(
                    uiState = addUiState,
                    promptingState = promptingState,
                    onBack = {
                        finish()
                        overridePendingTransition(
                            R.anim.slide_in_left,
                            R.anim.slide_out_right
                        )
                    },
                    onAdd = {
                        val msg = "Save: $name, emoji: $emoji, " +
                            "opts: ${options.size}, " +
                            "members: ${members.size}"
                        android.util.Log.d("AddGroupActivity", msg)
                        addViewModel.addGroup(
                            name = name,
                            description = description,
                            emoji = emoji,
                            options = options,
                            members = members
                        )
                    },
                    onGroupNameChange = { name = it },
                    onGroupDescriptionChange = { description = it },
                    onGroupEmojiChange = { emoji = it },
                    onPromptOptionsChange = {
                        options = it.selectedTone.toList() +
                            it.selectedFormat.toList()
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
                    onUpdatePromptOption = { id, nm, pr, onSuccess, onError ->
                        addViewModel.updatePromptOption(
                            optionId = id,
                            name = nm,
                            prompt = pr,
                            onSuccess = onSuccess,
                            onError = onError
                        )
                    },
                    onDeletePromptOption = { id, onSuccess, onError ->
                        addViewModel.deletePromptOption(
                            optionId = id,
                            onSuccess = onSuccess,
                            onError = onError
                        )
                    },
                    members = members,
                    onAddMember = {
                        showContactSelectDialog = true
                    },
                    onMemberClick = {
                        // TODO: 멤버 상세 또는 삭제
                    },
                    onBottomNavChange = { tab ->
                        if (tab == "inbox") {
                            startActivity(
                                Intent(this, InboxActivity::class.java)
                            )
                            overridePendingTransition(
                                R.anim.slide_in_left,
                                R.anim.slide_out_right
                            )
                        }
                    }
                )

                // 연락처 선택 다이얼로그
                if (showContactSelectDialog) {
                    android.util.Log.d(
                        "AddGroupActivity",
                        "Showing dialog with ${bookUiState.contacts.size} contacts"
                    )
                    ContactSelectDialog(
                        contacts = bookUiState.contacts,
                        selectedContacts = members,
                        onDismiss = { showContactSelectDialog = false },
                        onConfirm = { selected ->
                            android.util.Log.d(
                                "AddGroupActivity",
                                "Selected contacts: ${selected.size}"
                            )
                            members = selected
                            android.util.Log.d(
                                "AddGroupActivity",
                                "Members after update: ${members.size}"
                            )
                            showContactSelectDialog = false
                        }
                    )
                }

                // 결과 피드백
                LaunchedEffect(addUiState.error, addUiState.lastSuccessMsg) {
                    addUiState.error?.let {
                        Toast.makeText(
                            this@AddGroupActivity,
                            it,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    addUiState.lastSuccessMsg?.let {
                        Toast.makeText(
                            this@AddGroupActivity,
                            it,
                            Toast.LENGTH_SHORT
                        ).show()
                        setResult(RESULT_OK)
                        finish()
                        overridePendingTransition(
                            R.anim.slide_in_left,
                            R.anim.slide_out_right
                        )
                    }
                }

                if (addUiState.isSubmitting) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) { CircularProgressIndicator() }
                }
            }
        }
    }
}
