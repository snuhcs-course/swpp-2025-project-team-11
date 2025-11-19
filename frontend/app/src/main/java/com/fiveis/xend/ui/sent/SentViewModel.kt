package com.fiveis.xend.ui.sent

import com.fiveis.xend.data.model.EmailItem
import com.fiveis.xend.data.repository.SentRepository
import com.fiveis.xend.ui.base.BaseMailListUiState
import com.fiveis.xend.ui.base.BaseMailListViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sent 화면 UI 상태
 */
data class SentUiState(
    override val emails: List<EmailItem> = emptyList(),
    override val isLoading: Boolean = false,
    override val error: String? = null,
    override val loadMoreNextPageToken: String? = null,
    override val isRefreshing: Boolean = false
) : BaseMailListUiState {
    override fun copyWith(
        emails: List<EmailItem>,
        isLoading: Boolean,
        error: String?,
        loadMoreNextPageToken: String?,
        isRefreshing: Boolean
    ): BaseMailListUiState {
        return copy(
            emails = emails,
            isLoading = isLoading,
            error = error,
            loadMoreNextPageToken = loadMoreNextPageToken,
            isRefreshing = isRefreshing
        )
    }
}

/**
 * Sent 화면 ViewModel
 */
class SentViewModel(
    repository: SentRepository
) : BaseMailListViewModel<SentUiState, SentRepository>(
    repository = repository,
    uiStateFlow = MutableStateFlow(SentUiState()),
    logTag = "SentViewModel"
) {

    val uiState: StateFlow<SentUiState> = uiStateFlow.asStateFlow()

    /**
     * 이메일 클릭 처리
     */
    fun onEmailClick(email: EmailItem) {
        // TODO: 이메일 상세 화면으로 이동
    }
}
