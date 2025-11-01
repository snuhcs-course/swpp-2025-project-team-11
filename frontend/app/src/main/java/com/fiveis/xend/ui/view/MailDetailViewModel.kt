package com.fiveis.xend.ui.view

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiveis.xend.data.database.EmailDao
import com.fiveis.xend.data.model.EmailItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MailDetailUiState(
    val mail: EmailItem? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class MailDetailViewModel(
    private val emailDao: EmailDao,
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
                Log.d("MailDetailViewModel", "Loading mail from DB: $messageId")
                val email = emailDao.getEmailById(messageId)
                if (email != null) {
                    Log.d("MailDetailViewModel", "Email loaded from DB successfully")
                    _uiState.update {
                        it.copy(
                            mail = email,
                            isLoading = false
                        )
                    }
                } else {
                    Log.e("MailDetailViewModel", "Email not found in DB")
                    _uiState.update { it.copy(error = "Email not found", isLoading = false) }
                }
            } catch (e: Exception) {
                Log.e("MailDetailViewModel", "Error loading email from DB", e)
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
