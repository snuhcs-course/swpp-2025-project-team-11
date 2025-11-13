package com.fiveis.xend.ui.contactbook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.model.PromptOption
import com.fiveis.xend.data.repository.ContactBookRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupDetailUiState(
    val isLoading: Boolean = false,
    val group: Group? = null,
    val error: String? = null,
    val isRenaming: Boolean = false,
    val renameError: String? = null,
    val tonePromptOptions: List<PromptOption> = emptyList(),
    val formatPromptOptions: List<PromptOption> = emptyList(),
    val isPromptSaving: Boolean = false,
    val promptOptionsError: String? = null,
    val contacts: List<Contact> = emptyList()
)

class GroupDetailViewModel(
    app: Application,
    private val repo: ContactBookRepository = ContactBookRepository(app.applicationContext)
) : AndroidViewModel(app) {

    private val ui = MutableStateFlow(GroupDetailUiState())
    val uiState: StateFlow<GroupDetailUiState> = ui.asStateFlow()

    private var observingId: Long? = null
    private var observeJob: Job? = null
    private var promptOptionsJob: Job? = null
    private var contactsJob: Job? = null

    /**
     * - DB Flow로 즉시 캐시 표시
     * - 동시에 서버에서 새로고침 → Room 갱신 → UI 자동 갱신
     */
    fun load(id: Long, force: Boolean = false) {
        if (!force && observingId == id) return
        observingId = id

        ui.update { it.copy(isLoading = true, error = null) }

        ensurePromptOptionsObservation()
        ensureContactsObservation()

        // 1) DB Flow
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repo.observeGroup(id).collectLatest { group ->
                ui.update { it.copy(group = group) }
            }
        }

        // 2) 서버 → DB 동기화
        viewModelScope.launch {
            try {
                repo.refreshGroupAndMembers(id)
                ui.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                ui.update { it.copy(isLoading = false, error = e.message ?: "동기화 실패") }
            }
        }
    }

    /** 풀투리프레시/새로고침 버튼 등에 연결 */
    fun refresh() {
        val id = observingId ?: return
        ui.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                repo.refreshGroupAndMembers(id)
                ui.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                ui.update { it.copy(isLoading = false, error = e.message ?: "동기화 실패") }
            }
        }
    }

    fun removeMemberFromGroup(contactId: Long) {
        viewModelScope.launch {
            try {
                repo.updateContactGroup(contactId, null)
            } catch (e: Exception) {
                ui.update { it.copy(error = e.message ?: "멤버 삭제에 실패했습니다") }
            }
        }
    }

    fun addMembersToGroup(contactIds: List<Long>) {
        val groupId = observingId ?: return
        if (contactIds.isEmpty()) return
        viewModelScope.launch {
            try {
                contactIds.forEach { contactId ->
                    repo.updateContactGroup(contactId, groupId)
                }
            } catch (e: Exception) {
                ui.update { it.copy(error = e.message ?: "멤버 추가에 실패했습니다") }
            }
        }
    }

    fun renameGroup(newName: String, newDescription: String) {
        val id = observingId ?: return
        val trimmedName = newName.trim()
        if (trimmedName.isBlank()) {
            ui.update { it.copy(renameError = "그룹 이름을 입력해 주세요") }
            return
        }
        if (ui.value.isRenaming) return

        val trimmedDescription = newDescription.trim()
        viewModelScope.launch {
            ui.update { it.copy(isRenaming = true, renameError = null) }
            try {
                repo.updateGroup(groupId = id, name = trimmedName, description = trimmedDescription)
                ui.update { it.copy(isRenaming = false, renameError = null) }
            } catch (e: Exception) {
                ui.update {
                    it.copy(
                        isRenaming = false,
                        renameError = e.message ?: "그룹 이름 변경에 실패했습니다"
                    )
                }
            }
        }
    }

    fun clearRenameError() {
        ui.update { it.copy(renameError = null) }
    }

    fun refreshPromptOptions() {
        viewModelScope.launch {
            ui.update { it.copy(promptOptionsError = null) }
            try {
                repo.refreshPromptOptions()
            } catch (e: Exception) {
                ui.update { it.copy(promptOptionsError = e.message ?: "프롬프트 옵션을 불러오지 못했습니다") }
            }
        }
    }

    fun updateGroupPromptOptions(selectedOptionIds: List<Long>) {
        val id = observingId ?: return
        if (ui.value.isPromptSaving) return
        viewModelScope.launch {
            ui.update { it.copy(isPromptSaving = true, promptOptionsError = null) }
            try {
                repo.updateGroup(groupId = id, optionIds = selectedOptionIds)
                ui.update { it.copy(isPromptSaving = false) }
            } catch (e: Exception) {
                ui.update {
                    it.copy(
                        isPromptSaving = false,
                        promptOptionsError = e.message ?: "프롬프트 옵션 저장에 실패했습니다"
                    )
                }
            }
        }
    }

    fun addPromptOption(
        key: String,
        name: String,
        prompt: String,
        onSuccess: (PromptOption) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val created = repo.addPromptOption(key, name, prompt)
                onSuccess(created)
            } catch (e: Exception) {
                val msg = e.message ?: "프롬프트 추가에 실패했습니다"
                ui.update { it.copy(promptOptionsError = msg) }
                onError(msg)
            }
        }
    }

    fun clearPromptOptionsError() {
        ui.update { it.copy(promptOptionsError = null) }
    }

    override fun onCleared() {
        observeJob?.cancel()
        promptOptionsJob?.cancel()
        contactsJob?.cancel()
        super.onCleared()
    }

    private fun ensurePromptOptionsObservation() {
        if (promptOptionsJob != null) return
        promptOptionsJob = viewModelScope.launch {
            repo.observePromptOptions().collectLatest { all ->
                ui.update {
                    it.copy(
                        tonePromptOptions = all.filter { opt -> opt.key.equals("tone", ignoreCase = true) },
                        formatPromptOptions = all.filter { opt -> opt.key.equals("format", ignoreCase = true) }
                    )
                }
            }
        }
        refreshPromptOptions()
    }

    private fun ensureContactsObservation() {
        if (contactsJob != null) return
        contactsJob = viewModelScope.launch {
            repo.observeContacts().collectLatest { contacts ->
                ui.update { it.copy(contacts = contacts) }
            }
        }
    }
}
