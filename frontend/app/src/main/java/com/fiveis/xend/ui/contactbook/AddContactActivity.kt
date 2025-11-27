package com.fiveis.xend.ui.contactbook

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fiveis.xend.R
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.ui.inbox.InboxActivity

class AddContactActivity : ComponentActivity() {
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
                val addViewModel: AddContactViewModel = viewModel(
                    factory = AddContactViewModel.Factory(application)
                )
                val addUiState by addViewModel.uiState.collectAsState()

                // 그룹 목록이 필요하면 ContactBookViewModel 활용
                val bookViewModel: ContactBookViewModel = viewModel(
                    factory = ContactBookViewModel.Factory(application)
                )
                val bookUiState by bookViewModel.uiState.collectAsState()

                // AddContactScreen 입력값들 보관
                var name by rememberSaveable { mutableStateOf("") }
                var email by rememberSaveable { mutableStateOf("") }
                var senderRole by rememberSaveable { mutableStateOf<String?>(null) }
                var recipientRole by rememberSaveable { mutableStateOf<String?>(null) }
                var personalPrompt by rememberSaveable { mutableStateOf<String?>(null) }
                var selectedGroup by rememberSaveable { mutableStateOf<Group?>(null) }
                var languagePreference by rememberSaveable { mutableStateOf("") }

                AddContactScreen(
                    groups = bookUiState.groups,
                    onNameChange = { name = it },
                    onEmailChange = { email = it },
                    onSenderRoleChange = { senderRole = it },
                    onRecipientRoleChange = { recipientRole = it },
                    onPersonalPromptChange = { personalPrompt = it },
                    onLanguagePreferenceChange = { languagePreference = it },
                    onGroupChange = { selectedGroup = it },
                    onBack = {
                        finish()
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    },
                    onAdd = {
                        addViewModel.addContact(
                            name = name,
                            email = email,
                            senderRole = senderRole,
                            recipientRole = recipientRole ?: "",
                            personalPrompt = personalPrompt,
                            group = selectedGroup,
                            languagePreference = languagePreference
                        )
                    },
                    onGmailContactsSync = {
                        // TODO: Gmail 동기화 기능 구현 예정
                    },
                    onAddGroupClick = {
                        startActivity(Intent(this, AddGroupActivity::class.java))
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    },
                    onBottomNavChange = { tab ->
                        if (tab == "inbox") {
                            startActivity(Intent(this, InboxActivity::class.java))
                            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        }
                    },
                    showSuccessBanner = addUiState.showSuccessBanner,
                    successMessage = addUiState.successMessage,
                    onDismissSuccessBanner = { addViewModel.dismissSuccessBanner() },
                    errorMessage = addUiState.error,
                    onDismissError = { addViewModel.clearError() }
                )

                // 에러 및 성공 피드백
                LaunchedEffect(addUiState.showSuccessBanner) {
                    if (addUiState.showSuccessBanner) {
                        setResult(RESULT_OK)
                        finish()
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    }
                }

                if (addUiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) { CircularProgressIndicator() }
                }
            }
        }
    }
}
