package com.fiveis.xend.ui.contactbook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fiveis.xend.data.model.PromptOption
import com.fiveis.xend.data.repository.ContactBookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddGroupUiState(
    val tonePromptOptions: List<PromptOption> = emptyList(),
    val formatPromptOptions: List<PromptOption> = emptyList(),
    val isLoading: Boolean = false,
    val lastSuccessMsg: String? = null,
    val error: String? = null
)

class AddGroupViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ContactBookRepository = ContactBookRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(AddGroupUiState())
    val uiState: StateFlow<AddGroupUiState> = _uiState.asStateFlow()

    init {
        getAllPromptOptions()
    }

    fun addGroup(
        name: String,
        description: String,
        options: List<PromptOption>,
        members: List<com.fiveis.xend.data.model.Contact> = emptyList()
    ) {
        if (name.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "그룹 이름을 입력해 주세요.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null, lastSuccessMsg = null) }

        viewModelScope.launch {
            try {
                // 1. 그룹 생성
                android.util.Log.d("AddGroupViewModel", "Creating group: $name")
                val res = repository.addGroup(name, description, options)
                android.util.Log.d("AddGroupViewModel", "Group created with ID: ${res.id}")

                // 2. 멤버들의 group_id 업데이트
                if (members.isNotEmpty()) {
                    android.util.Log.d("AddGroupViewModel", "Updating ${members.size} members")
                    members.forEach { contact ->
                        android.util.Log.d("AddGroupViewModel", "Updating contact ${contact.id} to group ${res.id}")
                        repository.updateContactGroup(contact.id, res.id)
                    }
                    android.util.Log.d("AddGroupViewModel", "All members updated")
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        lastSuccessMsg = "추가 성공(그룹 ID: ${res.id}, 멤버 ${members.size}명)",
                        error = null
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("AddGroupViewModel", "Error adding group", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "알 수 없는 오류"
                    )
                }
            }
        }
    }

    fun addPromptOption(
        key: String,
        name: String,
        prompt: String,
        onError: (String) -> Unit = {},
        onSuccess: (PromptOption) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val created = repository.addPromptOption(key, name, prompt)
                _uiState.update { state ->
                    when (key) {
                        "tone" -> state.copy(tonePromptOptions = state.tonePromptOptions + created)
                        "format" -> state.copy(formatPromptOptions = state.formatPromptOptions + created)
                        else -> state
                    }
                }
                onSuccess(created)
            } catch (e: Exception) {
                val msg = e.message ?: "프롬프트 추가 실패"
                _uiState.update { it.copy(error = msg) }
                onError(msg)
            }
        }
    }

    fun getAllPromptOptions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val (tone, format) = repository.getAllPromptOptions()
                _uiState.update {
                    it.copy(
                        tonePromptOptions = tone,
                        formatPromptOptions = format,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "알 수 없는 오류"
                    )
                }
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
