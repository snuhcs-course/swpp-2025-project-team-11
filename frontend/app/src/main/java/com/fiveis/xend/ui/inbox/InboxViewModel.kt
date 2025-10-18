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
    val nextPageToken: String? = null,
    val isRefreshing: Boolean = false
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
        loadCachedEmails()
        refreshEmails()
    }

    private fun loadCachedEmails() {
        viewModelScope.launch {
            repository.getCachedEmails().collect { cachedEmails ->
                _uiState.update { it.copy(emails = cachedEmails) }
            }
        }
    }

    fun refreshEmails() {
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            try {
                val result = repository.refreshEmails()
                if (result.isFailure) {
                    _uiState.update {
                        it.copy(
                            error = result.exceptionOrNull()?.message,
                            isRefreshing = false
                        )
                    }
                } else {
                    val initialResponse = repository.getMails()
                    val nextToken = initialResponse.body()?.nextPageToken
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            error = null,
                            nextPageToken = nextToken
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message,
                        isRefreshing = false
                    )
                }
            }
        }
    }

    fun loadMoreEmails() {
        if (_uiState.value.isLoading || _uiState.value.nextPageToken == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val response = repository.getMails(pageToken = _uiState.value.nextPageToken)
                if (response.isSuccessful) {
                    val newEmails = response.body()?.messages ?: emptyList()
                    if (newEmails.isNotEmpty()) {
                        repository.saveEmailsToCache(newEmails)
                    }
                    _uiState.update {
                        it.copy(
                            nextPageToken = response.body()?.nextPageToken,
                            isLoading = false,
                            error = null
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
