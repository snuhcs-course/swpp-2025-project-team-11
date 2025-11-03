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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContactBookUiState(
    val selectedTab: ContactBookTab = ContactBookTab.Groups,
    val groups: List<Group> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ContactBookViewModel(
    application: Application,
    private val repository: ContactBookRepository = ContactBookRepository(application.applicationContext)
) : AndroidViewModel(application) {
    private var loadJob: Job? = null

    private val _uiState = MutableStateFlow(ContactBookUiState())
    val uiState: StateFlow<ContactBookUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeGroups().collectLatest { groups ->
                if (_uiState.value.selectedTab == ContactBookTab.Groups) {
                    _uiState.update { it.copy(groups = groups) }
                }
            }
        }
        viewModelScope.launch {
            repository.observeContacts().collectLatest { contacts ->
                if (_uiState.value.selectedTab == ContactBookTab.Contacts) {
                    _uiState.update { it.copy(contacts = contacts) }
                }
            }
        }
        // 초기 동기화(네트워크 → DB)
        refreshAll()
    }

    fun onTabSelected(tab: ContactBookTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
//                when (_uiState.value.selectedTab) {
//                    ContactBookTab.Groups -> repository.refreshGroups()
//                    ContactBookTab.Contacts -> repository.refreshContacts()
//                }
                repository.refreshGroups()
                repository.refreshContacts()
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "동기화 실패") }
            }
        }
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

    fun onContactDelete(contactId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.deleteContact(contactId)
//                loadContactInfo(ContactBookTab.Contacts)
                _uiState.update { it.copy(isLoading = false, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "연락처 삭제 실패") }
            }
        }
    }

    fun onGroupDelete(groupId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.deleteGroup(groupId)
//                loadContactInfo(ContactBookTab.Groups)
                _uiState.update { it.copy(isLoading = false, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "그룹 삭제 실패") }
            }
        }
    }

    class Factory(private val application: Application) : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ContactBookViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ContactBookViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
