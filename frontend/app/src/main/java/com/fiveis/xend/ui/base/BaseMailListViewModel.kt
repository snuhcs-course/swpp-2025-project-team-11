package com.fiveis.xend.ui.base

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiveis.xend.data.model.EmailItem
import com.fiveis.xend.data.repository.BaseMailRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base UI state interface for mail list screens
 */
interface BaseMailListUiState {
    val emails: List<EmailItem>
    val isLoading: Boolean
    val error: String?
    val loadMoreNextPageToken: String?
    val isRefreshing: Boolean

    fun copyWith(
        emails: List<EmailItem> = this.emails,
        isLoading: Boolean = this.isLoading,
        error: String? = this.error,
        loadMoreNextPageToken: String? = this.loadMoreNextPageToken,
        isRefreshing: Boolean = this.isRefreshing
    ): BaseMailListUiState
}

/**
 * Base ViewModel for mail list screens (Inbox and Sent)
 * Provides common functionality for loading, refreshing, and paginating emails
 *
 * @param repository The mail repository (InboxRepository or SentRepository)
 * @param uiStateFlow The mutable state flow for UI state
 * @param logTag Tag used for logging
 */
abstract class BaseMailListViewModel<T : BaseMailListUiState, R : BaseMailRepository>(
    protected val repository: R,
    protected val uiStateFlow: MutableStateFlow<T>,
    protected val logTag: String
) : ViewModel() {

    init {
        Log.d(logTag, "Initializing $logTag")
        loadCachedEmails()
        // 백그라운드에서 사일런트 동기화 (UI 로딩 표시 없이)
        silentRefreshEmails()
    }

    protected fun loadCachedEmails() {
        Log.d(logTag, "Starting to collect cached emails")
        viewModelScope.launch {
            repository.getCachedEmails().collect { cachedEmails ->
                Log.d(logTag, "Received ${cachedEmails.size} cached emails from DB")
                updateUiState { it.copyWith(emails = cachedEmails) }
            }
        }
    }

    /**
     * 사용자가 Pull-to-Refresh할 때 호출 (UI 로딩 표시 있음)
     */
    fun refreshEmails() {
        Log.d(logTag, "refreshEmails called (with UI loading)")
        updateUiState { it.copyWith(isRefreshing = true) }
        performRefresh(showLoading = true)
    }

    /**
     * 백그라운드 사일런트 동기화 (UI 로딩 표시 없음)
     */
    private fun silentRefreshEmails() {
        Log.d(logTag, "silentRefreshEmails called (background sync)")
        performRefresh(showLoading = false)
    }

    private fun performRefresh(showLoading: Boolean) {
        viewModelScope.launch {
            try {
                val result = repository.refreshEmails()
                if (result.isFailure) {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e(logTag, "refreshEmails failed: $errorMessage")
                    if (showLoading) {
                        updateUiState {
                            it.copyWith(
                                error = errorMessage,
                                isRefreshing = false
                            )
                        }
                    }
                } else {
                    val nextToken = result.getOrNull()
                    Log.d(logTag, "refreshEmails succeeded, nextPageToken: $nextToken")

                    updateUiState { currentState ->
                        // DB가 비어있을 때만 loadMoreNextPageToken 설정
                        // (DB에 메일이 있으면 refresh 토큰은 이미 저장된 메일을 가리키므로 버림)
                        val newLoadMoreToken = if (currentState.emails.isEmpty()) {
                            Log.d(logTag, "DB empty - setting loadMoreNextPageToken: $nextToken")
                            nextToken
                        } else {
                            Log.d(
                                logTag,
                                "DB has ${currentState.emails.size} emails - keeping existing loadMoreNextPageToken: " +
                                    "${currentState.loadMoreNextPageToken}"
                            )
                            currentState.loadMoreNextPageToken
                        }

                        currentState.copyWith(
                            isRefreshing = false,
                            error = null,
                            loadMoreNextPageToken = newLoadMoreToken
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(logTag, "Exception during refreshEmails", e)
                if (showLoading) {
                    updateUiState {
                        it.copyWith(
                            error = e.message,
                            isRefreshing = false
                        )
                    }
                }
            }
        }
    }

    fun loadMoreEmails() {
        val currentState = uiStateFlow.value
        Log.d(
            logTag,
            "loadMoreEmails called - isLoading=${currentState.isLoading}, isRefreshing=${currentState.isRefreshing}, " +
                "loadMoreNextPageToken=${currentState.loadMoreNextPageToken}"
        )

        if (currentState.isLoading) {
            Log.d(logTag, "loadMoreEmails skipped: already loading")
            return
        }

        val token = currentState.loadMoreNextPageToken
        if (token == null) {
            Log.d(logTag, "loadMoreEmails skipped: no next page token")
            return
        }

        Log.d(logTag, "loadMoreEmails proceeding with token: $token")
        viewModelScope.launch {
            updateUiState { it.copyWith(isLoading = true, error = null) }
            try {
                val response = repository.getMails(pageToken = token)
                if (response.isSuccessful) {
                    val newEmails = response.body()?.messages ?: emptyList()
                    Log.d(logTag, "Received ${newEmails.size} more emails")

                    // Check for duplicates
                    val existingIds = currentState.emails.map { it.id }.toSet()
                    val actuallyNewEmails = newEmails.filter { it.id !in existingIds }
                    Log.d(
                        logTag,
                        "Actually new emails: ${actuallyNewEmails.size} (duplicates: " +
                            "${newEmails.size - actuallyNewEmails.size})"
                    )

                    if (newEmails.isNotEmpty()) {
                        repository.saveEmailsToCache(newEmails)
                        Log.d(logTag, "Saved ${newEmails.size} emails to cache")
                    }

                    val newLoadMoreToken = response.body()?.nextPageToken
                    Log.d(logTag, "Updated loadMoreNextPageToken: $newLoadMoreToken")
                    updateUiState {
                        it.copyWith(
                            loadMoreNextPageToken = newLoadMoreToken,
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    Log.e(logTag, "loadMoreEmails failed with code: ${response.code()}")
                    updateUiState { it.copyWith(error = "Failed to load more emails", isLoading = false) }
                }
            } catch (e: Exception) {
                Log.e(logTag, "Exception during loadMoreEmails", e)
                updateUiState { it.copyWith(error = e.message, isLoading = false) }
            }
        }
    }

    /**
     * Helper function to update UI state
     */
    @Suppress("UNCHECKED_CAST")
    protected fun updateUiState(update: (T) -> BaseMailListUiState) {
        uiStateFlow.update { currentState ->
            update(currentState) as T
        }
    }
}
