package com.fiveis.xend.ui.compose

import androidx.lifecycle.ViewModel
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

class SendMailViewModel(
    private val endpointUrl: String,
    private val accessToken: String?,
    private val repo: MailSendRepository = MailSendRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow(SendUiState())
    val ui: StateFlow<SendUiState> = _ui

    fun sendEmail(to: String, subject: String, body: String) {
        if (to.isBlank()) {
            _ui.value = SendUiState(isSending = false, error = "수신자(to)가 비어있습니다.")
            return
        }
        _ui.value = SendUiState(isSending = true)
        viewModelScope.launch {
            try {
                val res = repo.sendEmail(endpointUrl, to, subject, body, accessToken)
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
}
