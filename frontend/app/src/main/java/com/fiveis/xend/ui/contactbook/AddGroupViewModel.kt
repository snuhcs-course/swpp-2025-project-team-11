package com.fiveis.xend.ui.contactbook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fiveis.xend.data.repository.ContactBookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddGroupUiState(
    val isLoading: Boolean = false,
    val lastSuccessMsg: String? = null,
    val error: String? = null
)

class AddGroupViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ContactBookRepository = ContactBookRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(AddGroupUiState())
    val uiState: StateFlow<AddGroupUiState> = _uiState.asStateFlow()

    fun addGroup(name: String, description: String) {
        if (name.isBlank()) {
            _uiState.value = AddGroupUiState(isLoading = false, error = "그룹 이름을 입력해 주세요.")
            return
        }

        _uiState.value = AddGroupUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val res = repository.addGroup(name, description)
                _uiState.value = AddGroupUiState(
                    isLoading = false,
                    lastSuccessMsg = "추가 성공(그룹 ID: ${res.id})",
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = AddGroupUiState(
                    isLoading = false,
                    error = e.message ?: "알 수 없는 오류"
                )
            }
        }
    }

    class Factory(private val application: Application) : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AddGroupViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AddGroupViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
