package com.fiveis.xend.ui.profile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fiveis.xend.R
import com.fiveis.xend.ui.login.MainActivity
import com.fiveis.xend.ui.theme.XendTheme

class ProfileActivity : ComponentActivity() {

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(this.applicationContext)
    }

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
            XendTheme {
                val uiState by viewModel.uiState.collectAsState()

                // 로그아웃 성공 Toast 제거 (화면 전환으로 충분)

                // 로그아웃 성공 시 로그인 화면으로 이동
                if (uiState.logoutSuccess) {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }

                ProfileScreen(
                    uiState = uiState,
                    onLogout = {
                        viewModel.logout()
                    },
                    onForceLogout = {
                        viewModel.forceLogout()
                    },
                    onDismissLogoutFailureDialog = {
                        viewModel.dismissLogoutFailureDialog()
                    },
                    onToggleEditMode = {
                        viewModel.toggleEditMode()
                    },
                    onUpdateDisplayName = { name ->
                        viewModel.updateDisplayName(name)
                    },
                    onUpdateInfo = { info ->
                        viewModel.updateInfo(info)
                    },
                    onUpdateLanguagePreference = { language ->
                        viewModel.updateLanguagePreference(language)
                    },
                    onSaveProfile = {
                        viewModel.saveProfile()
                    },
                    onDismissProfileError = {
                        viewModel.dismissProfileError()
                    },
                    onBack = {
                        finish()
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    }
                )
            }
        }
    }
}

class ProfileViewModelFactory(
    private val context: android.content.Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
