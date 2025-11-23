package com.fiveis.xend.ui.sent

import android.content.SharedPreferences
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
    private val repository: SentRepository,
    private val prefs: SharedPreferences
) : ViewModel() {

    companion object {
        private const val PREF_SENT_NEXT_PAGE_TOKEN = "sent_next_page_token"
    }

    private val _uiState = MutableStateFlow(SentUiState())
    val uiState: StateFlow<SentUiState> = _uiState.asStateFlow()

    init {
        Log.d("SentViewModel", "Initializing SentViewModel")
        // SharedPreferences에서 저장된 토큰 복원
        restorePageToken()
        loadCachedEmails()
        // 백그라운드에서 사일런트 동기화 (UI 로딩 표시 없이)
        silentRefreshEmails()
    }

    /**
     * SharedPreferences에서 저장된 페이지 토큰 복원
     */
    private fun restorePageToken() {
        val savedToken = prefs.getString(PREF_SENT_NEXT_PAGE_TOKEN, null)
        Log.d("SentViewModel", "Restored page token from prefs: $savedToken")
        if (savedToken != null) {
            _uiState.update { it.copy(loadMoreNextPageToken = savedToken) }
        }
    }

    /**
     * 페이지 토큰을 SharedPreferences에 저장
     */
    private fun savePageToken(token: String?) {
        Log.d("SentViewModel", "Saving page token to prefs: $token")
        prefs.edit().apply {
            if (token != null) {
                putString(PREF_SENT_NEXT_PAGE_TOKEN, token)
            } else {
                remove(PREF_SENT_NEXT_PAGE_TOKEN)
            }
            apply()
        }
    }

    /**
     * 토큰 에러 시 초기화 (만료된 토큰 등)
     */
    private fun clearPageToken() {
        Log.d("SentViewModel", "Clearing page token due to error")
        savePageToken(null)
        _uiState.update { it.copy(loadMoreNextPageToken = null) }
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
                        // 저장된 토큰이 없고, 새 토큰이 있을 때만 설정
                        // (이미 토큰이 있으면 유지 - loadMore로 받은 토큰이 더 정확함)
                        val newLoadMoreToken = if (currentState.loadMoreNextPageToken == null && nextToken != null) {
                            Log.d("SentViewModel", "No existing token - setting loadMoreNextPageToken: $nextToken")
                            // 새 토큰을 SharedPreferences에 저장
                            savePageToken(nextToken)
                            nextToken
                        } else {
                            Log.d(
                                "SentViewModel",
                                "Keeping existing loadMoreNextPageToken: ${currentState.loadMoreNextPageToken}"
                            )
                            // 기존 토큰 유지 (이미 SharedPreferences에 저장되어 있음)
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
                    // 새 토큰을 SharedPreferences에 저장
                    savePageToken(newLoadMoreToken)
                    _uiState.update {
                        it.copy(
                            loadMoreNextPageToken = newLoadMoreToken,
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    val errorCode = response.code()
                    Log.e("SentViewModel", "loadMoreEmails failed with code: $errorCode")
                    // 400, 401, 404 등의 에러는 토큰 문제일 가능성이 높으므로 초기화
                    if (errorCode in listOf(400, 401, 404)) {
                        Log.w("SentViewModel", "Clearing invalid page token due to error code: $errorCode")
                        clearPageToken()
                    }
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
