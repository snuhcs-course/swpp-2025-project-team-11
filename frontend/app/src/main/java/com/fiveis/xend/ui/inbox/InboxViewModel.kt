package com.fiveis.xend.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiveis.xend.data.model.EmailItem
import com.fiveis.xend.data.repository.InboxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Inbox 화면 UI 상태
 */
data class InboxUiState(
    val emails: List<EmailItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val nextPageToken: String? = null
)

/**
 * Inbox 화면 ViewModel
 */
class InboxViewModel(
    private val repository: InboxRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    init {
        loadEmails()
    }

    /**
     * 이메일 목록 로드
     */
    fun loadEmails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, emails = emptyList()) } // 기존 목록을 비웁니다.
            try {
                val response = repository.getMails()
                if (response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            emails = response.body()?.messages ?: emptyList(),
                            nextPageToken = response.body()?.nextPageToken,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(error = "Failed to load emails", isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun loadMoreEmails() {
        // 이미 로딩 중이거나 다음 페이지 토큰이 없으면 실행하지 않습니다.
        if (_uiState.value.isLoading || _uiState.value.nextPageToken == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val response = repository.getMails(pageToken = _uiState.value.nextPageToken)
                if (response.isSuccessful) {
                    val newEmails = response.body()?.messages ?: emptyList()
                    _uiState.update {
                        it.copy(
                            emails = it.emails + newEmails,
                            nextPageToken = response.body()?.nextPageToken,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(error = "Failed to load more emails", isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    /**
     * 이메일 클릭 처리
     */
    fun onEmailClick(email: EmailItem) {
        // TODO: 이메일 상세 화면으로 이동
    }
}
