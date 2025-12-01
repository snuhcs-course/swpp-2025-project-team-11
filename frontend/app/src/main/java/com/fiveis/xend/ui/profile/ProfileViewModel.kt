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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ProfileUiState(
    val userEmail: String = "",
    val displayName: String = "",
    val info: String = "",
    val languagePreference: String = "",
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val isLoggingOut: Boolean = false,
    val logoutError: String? = null,
    val logoutSuccess: Boolean = false,
    val showLogoutFailureDialog: Boolean = false,
    val showLogoutSuccessToast: Boolean = false,
    val profileError: String? = null,
    val saveSuccess: Boolean = false,
    val originalDisplayName: String? = null,
    val originalInfo: String? = null,
    val originalLanguagePreference: String? = null
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
        observeProfile()
        loadProfile()
    }

    private fun loadUserInfo() {
        val email = tokenManager.getUserEmail() ?: ""
        _uiState.value = _uiState.value.copy(userEmail = email)
    }

    private fun observeProfile() {
        viewModelScope.launch {
            profileRepository.observeProfile().collectLatest { profile ->
                profile?.let {
                    _uiState.value = _uiState.value.copy(
                        displayName = it.displayName.orEmpty(),
                        info = it.info.orEmpty(),
                        languagePreference = it.languagePreference.orEmpty(),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, profileError = null)

            when (val result = profileRepository.getProfile()) {
                is ProfileResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false
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
        val current = _uiState.value
        if (!current.isEditing) {
            _uiState.value = current.copy(
                isEditing = true,
                profileError = null,
                saveSuccess = false,
                originalDisplayName = current.displayName,
                originalInfo = current.info,
                originalLanguagePreference = current.languagePreference
            )
        } else {
            val shouldRestore = !current.saveSuccess
            val restoredDisplayName =
                if (shouldRestore) current.originalDisplayName ?: current.displayName else current.displayName
            val restoredInfo = if (shouldRestore) current.originalInfo ?: current.info else current.info
            val restoredLanguagePreference =
                if (shouldRestore) {
                    current.originalLanguagePreference ?: current.languagePreference
                } else {
                    current.languagePreference
                }

            _uiState.value = current.copy(
                isEditing = false,
                profileError = null,
                saveSuccess = false,
                displayName = restoredDisplayName,
                info = restoredInfo,
                languagePreference = restoredLanguagePreference,
                originalDisplayName = null,
                originalInfo = null,
                originalLanguagePreference = null
            )
        }
    }

    fun updateDisplayName(name: String) {
        _uiState.value = _uiState.value.copy(displayName = name)
    }

    fun updateInfo(info: String) {
        _uiState.value = _uiState.value.copy(info = info)
    }

    fun updateLanguagePreference(language: String) {
        _uiState.value = _uiState.value.copy(languagePreference = language)
    }

    fun saveProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, profileError = null, saveSuccess = false)

            val displayName = _uiState.value.displayName
            val info = _uiState.value.info
            val languagePreference = _uiState.value.languagePreference

            when (val result = profileRepository.updateProfile(displayName, info, languagePreference)) {
                is ProfileResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        isEditing = false,
                        displayName = result.data.displayName ?: "",
                        info = result.data.info ?: "",
                        languagePreference = result.data.languagePreference ?: "",
                        saveSuccess = true,
                        originalDisplayName = null,
                        originalInfo = null,
                        originalLanguagePreference = null
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
