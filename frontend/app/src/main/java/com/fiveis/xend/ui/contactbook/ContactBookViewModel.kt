package com.fiveis.xend.ui.contactbook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.repository.ContactBookRepository
import com.fiveis.xend.data.repository.ContactBookTab
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContactBookUiState(
    val selectedTab: ContactBookTab = ContactBookTab.Groups,
    val groups: List<Group> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ContactBookViewModel(application: Application) : AndroidViewModel(application) {
    private var loadJob: Job? = null

    private val repository: ContactBookRepository = ContactBookRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(ContactBookUiState())
    val uiState: StateFlow<ContactBookUiState> = _uiState.asStateFlow()

    init {
        loadContactInfo(ContactBookTab.Groups)
    }

    fun onTabSelected(tab: ContactBookTab) {
        loadContactInfo(tab)
    }

    private fun loadContactInfo(tab: ContactBookTab) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(selectedTab = tab, isLoading = true, error = null) }
            try {
                if (tab == ContactBookTab.Groups) {
                    val groups = repository.getAllGroups()
                    _uiState.update {
                        it.copy(
                            groups = groups, contacts = emptyList(), isLoading = false, error = null
                        )
                    }
                } else {
                    val contacts = repository.getAllContacts()
                    _uiState.update {
                        it.copy(
                            groups = emptyList(), contacts = contacts, isLoading = false, error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "불러오기 실패") }
            }
        }
    }

    // 그룹/연락처 클릭 처리
    fun onGroupClick(group: Group) {
        // TODO: 그룹 상세 화면으로 이동
    }

    fun onContactClick(contact: Contact) {
        // TODO: 연락처 상세 화면으로 이동
    }

    fun onContactDelete(contactId: Long) {
        try {
            viewModelScope.launch {
                repository.deleteContact(contactId)
                loadContactInfo(ContactBookTab.Contacts)
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message ?: "연락처 삭제 실패") }
        }
    }

    fun onGroupDelete(groupId: Long) {
        try {
            viewModelScope.launch {
                repository.deleteGroup(groupId)
                loadContactInfo(ContactBookTab.Groups)
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message ?: "그룹 삭제 실패") }
        }
    }
}
