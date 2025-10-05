package com.fiveis.xend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

data class MailComposeUiState(
    val isStreaming: Boolean = false,
    val subject: String = "",
    val bodyRendered: String = "",
    val error: String? = null
)

class MailComposeViewModel(
    private val api: MailComposeSseClient
) : ViewModel() {

    private val _ui = MutableStateFlow(MailComposeUiState())
    val ui: StateFlow<MailComposeUiState> = _ui

    private val bodyBuffer = StringBuilder()
    private var throttleJob: Job? = null

    fun startStreaming(payload: JSONObject) {
        bodyBuffer.clear()
        _ui.value = MailComposeUiState(isStreaming = true)

        // 80ms 스로틀로 화면 반영, 깜빡임 방지
        throttleJob?.cancel()
        throttleJob = viewModelScope.launch {
            while (isActive) {
                delay(80)
                val text = bodyBuffer.toString()
                _ui.update { it.copy(bodyRendered = text) }
            }
        }

        api.start(
            payload = payload,
            onSubject = { title ->
                _ui.update { it.copy(subject = title.ifBlank { it.subject }) }
            },
            onBodyDelta = { _, text ->
                bodyBuffer.append(text)
            },
            onDone = {
                throttleJob?.cancel()
                _ui.update { it.copy(isStreaming = false, bodyRendered = bodyBuffer.toString()) }
            },
            onError = { msg ->
                throttleJob?.cancel()
                _ui.update { it.copy(isStreaming = false, error = msg) }
            }
        )
    }

    fun stopStreaming() {
        api.stop()
        throttleJob?.cancel()
        _ui.update { it.copy(isStreaming = false) }
    }
}
