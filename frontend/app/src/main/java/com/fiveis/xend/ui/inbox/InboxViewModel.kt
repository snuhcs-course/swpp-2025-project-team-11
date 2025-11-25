package com.fiveis.xend.ui.inbox

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiveis.xend.data.model.EmailItem
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.repository.ContactBookRepository
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
    val loadMoreNextPageToken: String? = null,
    val isRefreshing: Boolean = false,
    val showAddContactDialog: Boolean = false,
    val selectedEmailForContact: EmailItem? = null,
    val groups: List<Group> = emptyList(),
    val addContactSuccess: Boolean = false,
    val addContactError: String? = null,
    // 연락처에 있는 이메일 주소들
    val contactEmails: Set<String> = emptySet(),
    // 이메일 주소를 key로 하는 연락처 맵 (이름 표시용)
    val contactsByEmail: Map<String, String> = emptyMap(),
    // 임시 저장 성공 배너 표시 여부
    val showDraftSavedBanner: Boolean = false
)

/**
 * Inbox 화면 ViewModel
 */
class InboxViewModel(
    private val repository: InboxRepository,
    private val contactRepository: ContactBookRepository,
    private val prefs: SharedPreferences
) : ViewModel() {

    companion object {
        private const val PREF_INBOX_NEXT_PAGE_TOKEN = "inbox_next_page_token"
    }

    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    init {
        Log.d("InboxViewModel", "Initializing InboxViewModel")
        // SharedPreferences에서 저장된 토큰 복원
        restorePageToken()
        loadCachedEmails()
        loadGroups()
        observeContacts()
        refreshContactsFromServer()
        // 백그라운드에서 사일런트 동기화 (UI 로딩 표시 없이)
        silentRefreshEmails()
    }

    /**
     * SharedPreferences에서 저장된 페이지 토큰 복원
     */
    private fun restorePageToken() {
        val savedToken = prefs.getString(PREF_INBOX_NEXT_PAGE_TOKEN, null)
        Log.d("InboxViewModel", "Restored page token from prefs: $savedToken")
        if (savedToken != null) {
            _uiState.update { it.copy(loadMoreNextPageToken = savedToken) }
        }
    }

    private fun refreshContactsFromServer() {
        viewModelScope.launch {
            try {
                contactRepository.refreshGroups()
                contactRepository.refreshContacts()
            } catch (e: Exception) {
                Log.e("InboxViewModel", "Failed to refresh contacts when Inbox starts", e)
            }
        }
    }

    /**
     * 페이지 토큰을 SharedPreferences에 저장
     */
    private fun savePageToken(token: String?) {
        Log.d("InboxViewModel", "Saving page token to prefs: $token")
        prefs.edit().apply {
            if (token != null) {
                putString(PREF_INBOX_NEXT_PAGE_TOKEN, token)
            } else {
                remove(PREF_INBOX_NEXT_PAGE_TOKEN)
            }
            apply()
        }
    }

    /**
     * 토큰 에러 시 초기화 (만료된 토큰 등)
     */
    private fun clearPageToken() {
        Log.d("InboxViewModel", "Clearing page token due to error")
        savePageToken(null)
        _uiState.update { it.copy(loadMoreNextPageToken = null) }
    }

    private fun loadGroups() {
        viewModelScope.launch {
            contactRepository.observeGroups().collect { groups ->
                _uiState.update { it.copy(groups = groups) }
            }
        }
    }

    private fun observeContacts() {
        viewModelScope.launch {
            contactRepository.observeContacts().collect { contacts ->
                val contactEmailsSet = contacts.map { it.email.lowercase() }.toSet()
                val contactsByEmailMap = contacts.associate { it.email.lowercase() to it.name }
                Log.d("InboxViewModel", "Contact emails updated: ${contactEmailsSet.size} contacts")
                _uiState.update {
                    it.copy(
                        contactEmails = contactEmailsSet,
                        contactsByEmail = contactsByEmailMap
                    )
                }
            }
        }
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

    /**
     * 사용자가 Pull-to-Refresh할 때 호출 (UI 로딩 표시 있음)
     */
    fun refreshEmails() {
        Log.d("InboxViewModel", "refreshEmails called (with UI loading)")
        _uiState.update { it.copy(isRefreshing = true) }
        performRefresh(showLoading = true)
    }

    /**
     * 백그라운드 사일런트 동기화 (UI 로딩 표시 없음)
     */
    private fun silentRefreshEmails() {
        Log.d("InboxViewModel", "silentRefreshEmails called (background sync)")
        performRefresh(showLoading = false)
    }

    private fun performRefresh(showLoading: Boolean) {
        viewModelScope.launch {
            try {
                val result = repository.refreshEmails()
                if (result.isFailure) {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e("InboxViewModel", "refreshEmails failed: $errorMessage")
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
                    Log.d("InboxViewModel", "refreshEmails succeeded, nextPageToken: $nextToken")

                    _uiState.update { currentState ->
                        // 저장된 토큰이 없고, 새 토큰이 있을 때만 설정
                        // (이미 토큰이 있으면 유지 - loadMore로 받은 토큰이 더 정확함)
                        val newLoadMoreToken = if (currentState.loadMoreNextPageToken == null && nextToken != null) {
                            Log.d("InboxViewModel", "No existing token - setting loadMoreNextPageToken: $nextToken")
                            // 새 토큰을 SharedPreferences에 저장
                            savePageToken(nextToken)
                            nextToken
                        } else {
                            Log.d(
                                "InboxViewModel",
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
                Log.e("InboxViewModel", "Exception during refreshEmails", e)
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
            "InboxViewModel",
            "loadMoreEmails called - isLoading=${currentState.isLoading}, isRefreshing=${currentState.isRefreshing}, " +
                "loadMoreNextPageToken=${currentState.loadMoreNextPageToken}"
        )

        if (currentState.isLoading) {
            Log.d("InboxViewModel", "loadMoreEmails skipped: already loading")
            return
        }

        val token = currentState.loadMoreNextPageToken
        if (token == null) {
            Log.d("InboxViewModel", "loadMoreEmails skipped: no next page token")
            return
        }

        Log.d("InboxViewModel", "loadMoreEmails proceeding with token: $token")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = repository.getMails(pageToken = token)
                if (response.isSuccessful) {
                    val newEmails = response.body()?.messages ?: emptyList()
                    Log.d("InboxViewModel", "Received ${newEmails.size} more emails")

                    // Check for duplicates
                    val existingIds = currentState.emails.map { it.id }.toSet()
                    val actuallyNewEmails = newEmails.filter { it.id !in existingIds }
                    Log.d(
                        "InboxViewModel",
                        "Actually new emails: ${actuallyNewEmails.size} (duplicates: " +
                            "${newEmails.size - actuallyNewEmails.size})"
                    )

                    if (newEmails.isNotEmpty()) {
                        repository.saveEmailsToCache(newEmails)
                        Log.d("InboxViewModel", "Saved ${newEmails.size} emails to cache")
                    }

                    val newLoadMoreToken = response.body()?.nextPageToken
                    Log.d("InboxViewModel", "Updated loadMoreNextPageToken: $newLoadMoreToken")
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
                    Log.e("InboxViewModel", "loadMoreEmails failed with code: $errorCode")
                    // 400, 401, 404 등의 에러는 토큰 문제일 가능성이 높으므로 초기화
                    if (errorCode in listOf(400, 401, 404)) {
                        Log.w("InboxViewModel", "Clearing invalid page token due to error code: $errorCode")
                        clearPageToken()
                    }
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

    /**
     * 연락처 추가 다이얼로그 표시
     */
    fun showAddContactDialog(email: EmailItem) {
        _uiState.update {
            it.copy(
                showAddContactDialog = true,
                selectedEmailForContact = email,
                addContactSuccess = false,
                addContactError = null
            )
        }
    }

    /**
     * 연락처 추가 다이얼로그 닫기
     */
    fun dismissAddContactDialog() {
        _uiState.update {
            it.copy(
                showAddContactDialog = false,
                selectedEmailForContact = null,
                addContactSuccess = false,
                addContactError = null
            )
        }
    }

    /**
     * 성공 배너 닫기
     */
    fun dismissSuccessBanner() {
        _uiState.update {
            it.copy(addContactSuccess = false)
        }
    }

    /**
     * 임시 저장 성공 배너 표시
     */
    fun showDraftSavedBanner() {
        _uiState.update { it.copy(showDraftSavedBanner = true) }
    }

    /**
     * 임시 저장 성공 배너 닫기
     */
    fun dismissDraftSavedBanner() {
        _uiState.update { it.copy(showDraftSavedBanner = false) }
    }

    /**
     * 연락처 추가
     */
    fun addContact(
        name: String,
        email: String,
        senderRole: String?,
        recipientRole: String,
        personalPrompt: String?,
        groupId: Long?
    ) {
        viewModelScope.launch {
            try {
                Log.d("InboxViewModel", "Adding contact: name=$name, email=$email")
                contactRepository.addContact(
                    name = name,
                    email = email,
                    groupId = groupId,
                    senderRole = senderRole,
                    recipientRole = recipientRole,
                    personalPrompt = personalPrompt
                )
                Log.d("InboxViewModel", "Contact added successfully")
                _uiState.update {
                    it.copy(
                        addContactSuccess = true,
                        addContactError = null,
                        showAddContactDialog = false,
                        selectedEmailForContact = null
                    )
                }
            } catch (e: Exception) {
                Log.e("InboxViewModel", "Failed to add contact", e)
                _uiState.update {
                    it.copy(
                        addContactSuccess = false,
                        addContactError = e.message ?: "연락처 추가 실패"
                    )
                }
            }
        }
    }
}
