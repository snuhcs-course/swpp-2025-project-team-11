package com.fiveis.xend.ui.inbox

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.fiveis.xend.data.model.EmailItem
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.repository.ContactBookRepository
import com.fiveis.xend.data.repository.InboxRepository
import com.fiveis.xend.ui.base.BaseMailListUiState
import com.fiveis.xend.ui.base.BaseMailListViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Inbox 화면 UI 상태
 */
data class InboxUiState(
    override val emails: List<EmailItem> = emptyList(),
    override val isLoading: Boolean = false,
    override val error: String? = null,
    override val loadMoreNextPageToken: String? = null,
    override val isRefreshing: Boolean = false,
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
) : BaseMailListUiState {
    override fun copyWith(
        emails: List<EmailItem>,
        isLoading: Boolean,
        error: String?,
        loadMoreNextPageToken: String?,
        isRefreshing: Boolean
    ): BaseMailListUiState {
        return copy(
            emails = emails,
            isLoading = isLoading,
            error = error,
            loadMoreNextPageToken = loadMoreNextPageToken,
            isRefreshing = isRefreshing
        )
    }
}

/**
 * Inbox 화면 ViewModel
 */
class InboxViewModel(
    repository: InboxRepository,
    private val contactRepository: ContactBookRepository
) : BaseMailListViewModel<InboxUiState, InboxRepository>(
    repository = repository,
    uiStateFlow = MutableStateFlow(InboxUiState()),
    logTag = "InboxViewModel"
) {

    val uiState: StateFlow<InboxUiState> = uiStateFlow.asStateFlow()

    init {
        loadGroups()
        observeContacts()
    }

    private fun loadGroups() {
        viewModelScope.launch {
            contactRepository.observeGroups().collect { groups ->
                uiStateFlow.update { it.copy(groups = groups) }
            }
        }
    }

    private fun observeContacts() {
        viewModelScope.launch {
            contactRepository.observeContacts().collect { contacts ->
                val contactEmailsSet = contacts.map { it.email.lowercase() }.toSet()
                val contactsByEmailMap = contacts.associate { it.email.lowercase() to it.name }
                Log.d("InboxViewModel", "Contact emails updated: ${contactEmailsSet.size} contacts")
                uiStateFlow.update {
                    it.copy(
                        contactEmails = contactEmailsSet,
                        contactsByEmail = contactsByEmailMap
                    )
                }
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
        uiStateFlow.update {
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
        uiStateFlow.update {
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
        uiStateFlow.update {
            it.copy(addContactSuccess = false)
        }
    }

    /**
     * 임시 저장 성공 배너 표시
     */
    fun showDraftSavedBanner() {
        uiStateFlow.update { it.copy(showDraftSavedBanner = true) }
    }

    /**
     * 임시 저장 성공 배너 닫기
     */
    fun dismissDraftSavedBanner() {
        uiStateFlow.update { it.copy(showDraftSavedBanner = false) }
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
                uiStateFlow.update {
                    it.copy(
                        addContactSuccess = true,
                        addContactError = null,
                        showAddContactDialog = false,
                        selectedEmailForContact = null
                    )
                }
            } catch (e: Exception) {
                Log.e("InboxViewModel", "Failed to add contact", e)
                uiStateFlow.update {
                    it.copy(
                        addContactSuccess = false,
                        addContactError = e.message ?: "연락처 추가 실패"
                    )
                }
            }
        }
    }
}
