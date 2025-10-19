package com.fiveis.xend.ui.inbox

import android.util.Log
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
        Log.d("InboxViewModel", "Initializing InboxViewModel")
        loadCachedEmails()
        refreshEmails()
    }

    private fun loadCachedEmails() {
        Log.d("InboxViewModel", "Starting to collect cached emails")
        viewModelScope.launch {
            repository.getCachedEmails().collect { cachedEmails ->
                Log.d("InboxViewModel", "Received ${cachedEmails.size} cached emails from DB")
                _uiState.update { it.copy(emails = cachedEmails) }
            }
        }
    }

    fun refreshEmails() {
        Log.d("InboxViewModel", "refreshEmails called")
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            try {
                val result = repository.refreshEmails()
                if (result.isFailure) {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e("InboxViewModel", "refreshEmails failed: $errorMessage")
                    _uiState.update {
                        it.copy(
                            error = errorMessage,
                            isRefreshing = false
                        )
                    }
                } else {
                    val nextToken = result.getOrNull()
                    Log.d("InboxViewModel", "refreshEmails succeeded, nextPageToken: $nextToken")
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            error = null,
                            nextPageToken = nextToken
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("InboxViewModel", "Exception during refreshEmails", e)
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
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.isRefreshing) {
            Log.d("InboxViewModel", "loadMoreEmails skipped: already loading or refreshing")
            return
        }

        val token = currentState.nextPageToken
        if (token == null) {
            Log.d("InboxViewModel", "loadMoreEmails skipped: no next page token")
            return
        }

        Log.d("InboxViewModel", "loadMoreEmails called with token: $token")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = repository.getMails(pageToken = token)
                if (response.isSuccessful) {
                    val newEmails = response.body()?.messages ?: emptyList()
                    Log.d("InboxViewModel", "Received ${newEmails.size} more emails")
                    if (newEmails.isNotEmpty()) {
                        repository.saveEmailsToCache(newEmails)
                        Log.d("InboxViewModel", "Saved ${newEmails.size} emails to cache")
                    }
                    _uiState.update {
                        it.copy(
                            nextPageToken = response.body()?.nextPageToken,
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    Log.e("InboxViewModel", "loadMoreEmails failed with code: ${response.code()}")
                    _uiState.update { it.copy(error = "Failed to load more emails", isLoading = false) }
                }
            } catch (e: Exception) {
                Log.e("InboxViewModel", "Exception during loadMoreEmails", e)
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
