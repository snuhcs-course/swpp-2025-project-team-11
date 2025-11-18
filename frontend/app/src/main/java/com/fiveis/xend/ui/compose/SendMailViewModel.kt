package com.fiveis.xend.ui.compose

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fiveis.xend.data.repository.MailSendRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SendUiState(
    val isSending: Boolean = false,
    val lastSuccessMsg: String? = null,
    val error: String? = null
)

class SendMailViewModel(application: Application) : AndroidViewModel(application) {
    private val repo: MailSendRepository = MailSendRepository(application.applicationContext)

    private val _ui = MutableStateFlow(SendUiState())
    val ui: StateFlow<SendUiState> = _ui

    fun sendEmail(to: List<String>, subject: String, body: String, attachments: List<Uri> = emptyList()) {
        if (to.isEmpty()) {
            _ui.value = SendUiState(isSending = false, error = "수신자(to)가 비어있습니다.")
            return
        }
        _ui.value = SendUiState(isSending = true)
        viewModelScope.launch {
            try {
                val res = repo.sendEmail(
                    to = to,
                    subject = subject,
                    body = body,
                    attachmentUris = attachments
                )
                _ui.value = SendUiState(
                    isSending = false,
                    lastSuccessMsg = "전송 완료: ${res.id}",
                    error = null
                )
            } catch (e: Exception) {
                _ui.value = SendUiState(
                    isSending = false,
                    error = e.message ?: "알 수 없는 오류"
                )
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SendMailViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SendMailViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
