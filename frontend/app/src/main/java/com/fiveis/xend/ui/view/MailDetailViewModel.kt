package com.fiveis.xend.ui.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiveis.xend.data.model.MailDetailResponse
import com.fiveis.xend.data.repository.InboxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MailDetailUiState(
    val mail: MailDetailResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class MailDetailViewModel(
    private val repository: InboxRepository,
    private val messageId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(MailDetailUiState())
    val uiState: StateFlow<MailDetailUiState> = _uiState.asStateFlow()

    init {
        loadMail()
    }

    private fun loadMail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val response = repository.getMail(messageId)
                if (response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            mail = response.body(),
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(error = "Failed to load mail", isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
