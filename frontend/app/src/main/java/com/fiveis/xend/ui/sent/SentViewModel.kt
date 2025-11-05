package com.fiveis.xend.ui.sent

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiveis.xend.data.model.EmailItem
import com.fiveis.xend.data.repository.SentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Sent 화면 UI 상태
 */
data class SentUiState(
    val emails: List<EmailItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val loadMoreNextPageToken: String? = null,
    val isRefreshing: Boolean = false
)

/**
 * Sent 화면 ViewModel
 */
class SentViewModel(
    private val repository: SentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SentUiState())
    val uiState: StateFlow<SentUiState> = _uiState.asStateFlow()

    init {
        Log.d("SentViewModel", "Initializing SentViewModel")
        loadCachedEmails()
        // 백그라운드에서 사일런트 동기화 (UI 로딩 표시 없이)
        silentRefreshEmails()
    }

    private fun loadCachedEmails() {
        Log.d("SentViewModel", "Starting to collect cached emails")
        viewModelScope.launch {
            repository.getCachedEmails().collect { cachedEmails ->
                Log.d("SentViewModel", "Received ${cachedEmails.size} cached emails from DB")
                _uiState.update { it.copy(emails = cachedEmails) }
            }
        }
    }

    /**
     * 사용자가 Pull-to-Refresh할 때 호출 (UI 로딩 표시 있음)
     */
    fun refreshEmails() {
        Log.d("SentViewModel", "refreshEmails called (with UI loading)")
        _uiState.update { it.copy(isRefreshing = true) }
        performRefresh(showLoading = true)
    }

    /**
     * 백그라운드 사일런트 동기화 (UI 로딩 표시 없음)
     */
    private fun silentRefreshEmails() {
        Log.d("SentViewModel", "silentRefreshEmails called (background sync)")
        performRefresh(showLoading = false)
    }

    private fun performRefresh(showLoading: Boolean) {
        viewModelScope.launch {
            try {
                val result = repository.refreshEmails()
                if (result.isFailure) {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e("SentViewModel", "refreshEmails failed: $errorMessage")
                    if (showLoading) {
                        _uiState.update {
                            it.copy(
                                error = errorMessage,
                                isRefreshing = false
                            )
                        }
                    }
                } else {
                    val nextToken = result.getOrNull()
                    Log.d("SentViewModel", "refreshEmails succeeded, nextPageToken: $nextToken")

                    _uiState.update { currentState ->
                        // DB가 비어있을 때만 loadMoreNextPageToken 설정
                        // (DB에 메일이 있으면 refresh 토큰은 이미 저장된 메일을 가리키므로 버림)
                        val newLoadMoreToken = if (currentState.emails.isEmpty()) {
                            Log.d("SentViewModel", "DB empty - setting loadMoreNextPageToken: $nextToken")
                            nextToken
                        } else {
                            Log.d(
                                "SentViewModel",
                                "DB has ${currentState.emails.size} emails - keeping existing loadMoreNextPageToken: " +
                                    "${currentState.loadMoreNextPageToken}"
                            )
                            currentState.loadMoreNextPageToken
                        }

                        currentState.copy(
                            isRefreshing = false,
                            error = null,
                            loadMoreNextPageToken = newLoadMoreToken
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("SentViewModel", "Exception during refreshEmails", e)
                if (showLoading) {
                    _uiState.update {
                        it.copy(
                            error = e.message,
                            isRefreshing = false
                        )
                    }
                }
            }
        }
    }

    fun loadMoreEmails() {
        val currentState = _uiState.value
        Log.d(
            "SentViewModel",
            "loadMoreEmails called - isLoading=${currentState.isLoading}, isRefreshing=${currentState.isRefreshing}, " +
                "loadMoreNextPageToken=${currentState.loadMoreNextPageToken}"
        )

        if (currentState.isLoading) {
            Log.d("SentViewModel", "loadMoreEmails skipped: already loading")
            return
        }

        val token = currentState.loadMoreNextPageToken
        if (token == null) {
            Log.d("SentViewModel", "loadMoreEmails skipped: no next page token")
            return
        }

        Log.d("SentViewModel", "loadMoreEmails proceeding with token: $token")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = repository.getMails(pageToken = token)
                if (response.isSuccessful) {
                    val newEmails = response.body()?.messages ?: emptyList()
                    Log.d("SentViewModel", "Received ${newEmails.size} more emails")

                    // Check for duplicates
                    val existingIds = currentState.emails.map { it.id }.toSet()
                    val actuallyNewEmails = newEmails.filter { it.id !in existingIds }
                    Log.d(
                        "SentViewModel",
                        "Actually new emails: ${actuallyNewEmails.size} (duplicates: " +
                            "${newEmails.size - actuallyNewEmails.size})"
                    )

                    if (newEmails.isNotEmpty()) {
                        repository.saveEmailsToCache(newEmails)
                        Log.d("SentViewModel", "Saved ${newEmails.size} emails to cache")
                    }

                    val newLoadMoreToken = response.body()?.nextPageToken
                    Log.d("SentViewModel", "Updated loadMoreNextPageToken: $newLoadMoreToken")
                    _uiState.update {
                        it.copy(
                            loadMoreNextPageToken = newLoadMoreToken,
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    Log.e("SentViewModel", "loadMoreEmails failed with code: ${response.code()}")
                    _uiState.update { it.copy(error = "Failed to load more emails", isLoading = false) }
                }
            } catch (e: Exception) {
                Log.e("SentViewModel", "Exception during loadMoreEmails", e)
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
