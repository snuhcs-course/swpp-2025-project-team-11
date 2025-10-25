package com.fiveis.xend.ui.contactbook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.repository.ContactBookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddContactUiState(
    val isLoading: Boolean = false,
    val lastSuccessMsg: String? = null,
    val error: String? = null
)

class AddContactViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ContactBookRepository = ContactBookRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(AddContactUiState())
    val uiState: StateFlow<AddContactUiState> = _uiState.asStateFlow()

    fun addContact(name: String, email: String, relationshipRole: String, personalPrompt: String?, group: Group?) {
        if (name.isBlank()) {
            _uiState.value = AddContactUiState(isLoading = false, error = "이름을 입력해 주세요.")
            return
        }
        if (email.isBlank()) {
            _uiState.value = AddContactUiState(isLoading = false, error = "이메일을 입력해 주세요.")
            return
        }

        _uiState.value = AddContactUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val res = repository.addContact(name, email, group?.id, relationshipRole, personalPrompt)
                _uiState.value = AddContactUiState(
                    isLoading = false,
                    lastSuccessMsg = "추가 성공(연락처 ID: ${res.id})",
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = AddContactUiState(
                    isLoading = false,
                    error = e.message ?: "알 수 없는 오류"
                )
            }
        }
    }
}
