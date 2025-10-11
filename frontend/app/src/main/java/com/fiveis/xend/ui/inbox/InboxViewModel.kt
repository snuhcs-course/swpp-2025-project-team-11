package com.fiveis.xend.ui.inbox

import androidx.lifecycle.ViewModel
import com.fiveis.xend.data.model.EmailItem
import com.fiveis.xend.data.repository.InboxRepository
import com.fiveis.xend.data.repository.InboxTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Inbox 화면 UI 상태
 */
data class InboxUiState(
    val selectedTab: InboxTab = InboxTab.All,
    val emails: List<EmailItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Inbox 화면 ViewModel
 */
class InboxViewModel(
    private val repository: InboxRepository = InboxRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    init {
        loadEmails(InboxTab.All)
    }

    /**
     * 탭 선택 변경
     */
    fun onTabSelected(tab: InboxTab) {
        loadEmails(tab)
    }

    /**
     * 이메일 목록 로드
     */
    private fun loadEmails(tab: InboxTab) {
        _uiState.update {
            it.copy(
                selectedTab = tab,
                emails = repository.getEmails(tab),
                isLoading = false,
                error = null
            )
        }
    }

    /**
     * 이메일 클릭 처리
     */
    fun onEmailClick(email: EmailItem) {
        // TODO: 이메일 상세 화면으로 이동
    }
}
