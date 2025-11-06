package com.fiveis.xend.ui.inbox

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
    val contactsByEmail: Map<String, String> = emptyMap()
)

/**
 * Inbox 화면 ViewModel
 */
class InboxViewModel(
    private val repository: InboxRepository,
    private val contactRepository: ContactBookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    init {
        Log.d("InboxViewModel", "Initializing InboxViewModel")
        loadCachedEmails()
        loadGroups()
        observeContacts()
        // 백그라운드에서 사일런트 동기화 (UI 로딩 표시 없이)
        silentRefreshEmails()
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
                        // DB가 비어있을 때만 loadMoreNextPageToken 설정
                        // (DB에 메일이 있으면 refresh 토큰은 이미 저장된 메일을 가리키므로 버림)
                        val newLoadMoreToken = if (currentState.emails.isEmpty()) {
                            Log.d("InboxViewModel", "DB empty - setting loadMoreNextPageToken: $nextToken")
                            nextToken
                        } else {
                            Log.d(
                                "InboxViewModel",
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
                    _uiState.update {
                        it.copy(
                            loadMoreNextPageToken = newLoadMoreToken,
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
