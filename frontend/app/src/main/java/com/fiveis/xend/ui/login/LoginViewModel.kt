package com.fiveis.xend.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiveis.xend.data.repository.AuthRepository
import com.fiveis.xend.data.repository.AuthResult
import com.fiveis.xend.data.source.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 로그인 화면 UI 상태
 */
data class LoginUiState(
    val isLoggedIn: Boolean = false,
    val userEmail: String = "",
    val messages: String = "",
    val isLoading: Boolean = false
)

/**
 * 로그인 화면 ViewModel
 */
class LoginViewModel(
    private val tokenManager: TokenManager,
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        checkSavedTokens()
    }

    /**
     * 저장된 토큰 확인
     */
    private fun checkSavedTokens() {
        val accessToken = tokenManager.getAccessToken()
        val savedEmail = tokenManager.getUserEmail()

        if (!accessToken.isNullOrEmpty() && !savedEmail.isNullOrEmpty()) {
            _uiState.update {
                it.copy(
                    isLoggedIn = true,
                    userEmail = savedEmail,
                    messages = "저장된 세션으로 로그인됨"
                )
            }
            Log.d("LoginViewModel", "Access Token: ${accessToken.take(20)}...")
        } else {
            _uiState.update {
                it.copy(
                    isLoggedIn = false,
                    userEmail = "",
                    messages = ""
                )
            }
        }
    }

    /**
     * Auth Code를 서버로 전송하고 JWT 토큰 받기
     */
    fun handleAuthCodeReceived(authCode: String, email: String) {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            when (val result = authRepository.sendAuthCodeToServer(authCode)) {
                is AuthResult.Success -> {
                    tokenManager.saveTokens(
                        access = result.accessToken,
                        refresh = result.refreshToken,
                        email = email
                    )
                    _uiState.update {
                        it.copy(
                            isLoggedIn = true,
                            userEmail = email,
                            messages = "✅ 서버 통신 성공 & 토큰 저장 완료",
                            isLoading = false
                        )
                    }
                }
                is AuthResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            messages = result.message,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    /**
     * 로그아웃
     */
    fun logout() {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val accessToken = tokenManager.getAccessToken()
            val refreshToken = tokenManager.getRefreshToken()

            // 서버에 로그아웃 요청
            authRepository.logout(accessToken, refreshToken)

            // 로컬 토큰 삭제
            tokenManager.clearTokens()

            _uiState.update {
                it.copy(
                    isLoggedIn = false,
                    userEmail = "",
                    messages = "로그아웃되었습니다\n모든 토큰이 삭제되었습니다",
                    isLoading = false
                )
            }
            Log.d("LoginViewModel", "로그아웃 성공")
        }
    }

    /**
     * 메시지 업데이트
     */
    fun updateMessages(message: String) {
        _uiState.update { it.copy(messages = message) }
    }
}
