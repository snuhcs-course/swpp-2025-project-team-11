package com.fiveis.xend.ui.contactbook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.repository.ContactBookRepository
import com.fiveis.xend.data.repository.ContactBookTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ContactBookUiState(
    val selectedTab: ContactBookTab = ContactBookTab.Groups,
    val groups: List<Group> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ContactBookViewModel(application: Application) : AndroidViewModel(application) {
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
        _uiState.update {
            if (tab == ContactBookTab.Groups) {
                it.copy(
                    selectedTab = tab,
                    groups = repository.getDummyGroups(),
                    contacts = emptyList(),
                    isLoading = false,
                    error = null
                )
            } else {
                it.copy(
                    selectedTab = tab,
                    groups = emptyList(),
                    contacts = repository.getDummyContacts(),
                    isLoading = false,
                    error = null
                )
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
}
