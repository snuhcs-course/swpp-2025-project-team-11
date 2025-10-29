package com.fiveis.xend.ui.view

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fiveis.xend.network.MailReplySseClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ReplyOptionState(
    val id: Int,
    val type: String,
    val title: String,
    val bodyBuffer: StringBuilder = StringBuilder(),
    val bodyRendered: String = "",
    val isComplete: Boolean = false,
    val totalSeq: Int = 0
)

data class ReplyComposeUiState(
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val options: List<ReplyOptionState> = emptyList(),
    val error: String? = null
)

class ReplyComposeViewModel(
    private val api: MailReplySseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReplyComposeUiState())
    val uiState: StateFlow<ReplyComposeUiState> = _uiState

    private var throttleJob: Job? = null

    /**
     * 답장 옵션 추천 시작
     */
    fun startReplyOptions(subject: String, body: String, toEmail: String) {
        _uiState.value = ReplyComposeUiState(isLoading = true, isStreaming = true)

        // 80ms 스로틀로 화면 반영
        throttleJob?.cancel()
        throttleJob = viewModelScope.launch {
            while (isActive) {
                delay(80)
                _uiState.update { state ->
                    state.copy(
                        options = state.options.map { option ->
                            option.copy(
                                bodyRendered = option.bodyBuffer.toString().replace("\n", "<br>")
                            )
                        }
                    )
                }
            }
        }

        api.start(
            subject = subject,
            body = body,
            toEmail = toEmail,
            onReady = {
                _uiState.update { it.copy(isLoading = false) }
            },
            onOptions = { optionInfos ->
                _uiState.update { state ->
                    state.copy(
                        options = optionInfos.map { info ->
                            ReplyOptionState(
                                id = info.id,
                                type = info.type,
                                title = info.title
                            )
                        }
                    )
                }
            },
            onOptionDelta = { optionId, seq, text ->
                _uiState.update { state ->
                    state.copy(
                        options = state.options.map { option ->
                            if (option.id == optionId) {
                                option.bodyBuffer.append(text)
                                option
                            } else {
                                option
                            }
                        }
                    )
                }
            },
            onOptionDone = { optionId, totalSeq ->
                _uiState.update { state ->
                    state.copy(
                        options = state.options.map { option ->
                            if (option.id == optionId) {
                                option.copy(
                                    isComplete = true,
                                    totalSeq = totalSeq,
                                    bodyRendered = option.bodyBuffer.toString().replace("\n", "<br>")
                                )
                            } else {
                                option
                            }
                        }
                    )
                }
            },
            onOptionError = { optionId, message ->
                _uiState.update { state ->
                    state.copy(
                        error = "옵션 $optionId 오류: $message",
                        options = state.options.map { option ->
                            if (option.id == optionId) {
                                option.copy(isComplete = true)
                            } else {
                                option
                            }
                        }
                    )
                }
            },
            onDone = { reason ->
                throttleJob?.cancel()
                _uiState.update { state ->
                    state.copy(
                        isStreaming = false,
                        options = state.options.map { option ->
                            option.copy(
                                bodyRendered = option.bodyBuffer.toString().replace("\n", "<br>")
                            )
                        }
                    )
                }
            },
            onError = { message ->
                throttleJob?.cancel()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isStreaming = false,
                        error = message
                    )
                }
            }
        )
    }

    fun stopStreaming() {
        api.stop()
        throttleJob?.cancel()
        _uiState.update { it.copy(isStreaming = false, isLoading = false) }
    }

    override fun onCleared() {
        super.onCleared()
        api.stop()
        throttleJob?.cancel()
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReplyComposeViewModel::class.java)) {
                val api = MailReplySseClient(
                    context = application,
                    endpointUrl = com.fiveis.xend.BuildConfig.BASE_URL + "api/ai/mail/reply/stream/"
                )
                return ReplyComposeViewModel(api) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
