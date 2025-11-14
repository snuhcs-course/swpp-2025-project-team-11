package com.fiveis.xend.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiveis.xend.data.database.AppDatabase
import com.fiveis.xend.data.repository.AuthRepository
import com.fiveis.xend.data.repository.LogoutResult
import com.fiveis.xend.data.repository.ProfileRepository
import com.fiveis.xend.data.repository.ProfileResult
import com.fiveis.xend.data.source.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val userEmail: String = "",
    val displayName: String = "",
    val info: String = "",
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val isLoggingOut: Boolean = false,
    val logoutError: String? = null,
    val logoutSuccess: Boolean = false,
    val showLogoutFailureDialog: Boolean = false,
    val showLogoutSuccessToast: Boolean = false,
    val profileError: String? = null,
    val saveSuccess: Boolean = false
)

class ProfileViewModel(
    private val context: Context,
    private val authRepository: AuthRepository = AuthRepository(context),
    private val profileRepository: ProfileRepository = ProfileRepository(context)
) : ViewModel() {

    private val tokenManager = TokenManager(context)
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserInfo()
        loadProfile()
    }

    private fun loadUserInfo() {
        val email = tokenManager.getUserEmail() ?: ""
        _uiState.value = _uiState.value.copy(userEmail = email)
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, profileError = null)

            when (val result = profileRepository.getProfile()) {
                is ProfileResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        displayName = result.data.displayName ?: "",
                        info = result.data.info ?: ""
                    )
                }
                is ProfileResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        profileError = result.message
                    )
                }
            }
        }
    }

    fun toggleEditMode() {
        _uiState.value = _uiState.value.copy(
            isEditing = !_uiState.value.isEditing,
            profileError = null,
            saveSuccess = false
        )
    }

    fun updateDisplayName(name: String) {
        _uiState.value = _uiState.value.copy(displayName = name)
    }

    fun updateInfo(info: String) {
        _uiState.value = _uiState.value.copy(info = info)
    }

    fun saveProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, profileError = null, saveSuccess = false)

            val displayName = _uiState.value.displayName.ifBlank { null }
            val info = _uiState.value.info.ifBlank { null }

            when (val result = profileRepository.updateProfile(displayName, info)) {
                is ProfileResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        isEditing = false,
                        displayName = result.data.displayName ?: "",
                        info = result.data.info ?: "",
                        saveSuccess = true
                    )
                }
                is ProfileResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        profileError = result.message
                    )
                }
            }
        }
    }

    fun dismissProfileError() {
        _uiState.value = _uiState.value.copy(profileError = null, saveSuccess = false)
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingOut = true, logoutError = null)

            val refreshToken = tokenManager.getRefreshToken()
            val result = authRepository.logout(refreshToken)

            when (result) {
                is LogoutResult.Success -> {
                    // Toast 표시
                    _uiState.value = _uiState.value.copy(
                        showLogoutSuccessToast = true
                    )

                    // 로컬 데이터 삭제
                    clearLocalData()

                    // 토큰 삭제
                    tokenManager.clearTokens()

                    _uiState.value = _uiState.value.copy(
                        isLoggingOut = false,
                        logoutSuccess = true
                    )
                }
                is LogoutResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isLoggingOut = false,
                        logoutError = result.message,
                        showLogoutFailureDialog = true
                    )
                }
            }
        }
    }

    private suspend fun clearLocalData() {
        try {
            val database = AppDatabase.getDatabase(context)
            database.clearAllTables()
        } catch (e: Exception) {
            // 데이터베이스 클리어 실패는 무시
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(logoutError = null)
    }

    fun dismissLogoutFailureDialog() {
        _uiState.value = _uiState.value.copy(showLogoutFailureDialog = false, logoutError = null)
    }

    fun forceLogout() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoggingOut = true,
                showLogoutFailureDialog = false,
                logoutError = null
            )

            // 로컬 데이터 삭제
            clearLocalData()

            // 토큰 삭제
            tokenManager.clearTokens()

            _uiState.value = _uiState.value.copy(
                isLoggingOut = false,
                logoutSuccess = true
            )
        }
    }
}
