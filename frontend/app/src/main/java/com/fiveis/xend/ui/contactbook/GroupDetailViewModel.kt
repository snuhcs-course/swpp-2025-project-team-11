package com.fiveis.xend.ui.contactbook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.repository.ContactBookRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupDetailUiState(
    val isLoading: Boolean = false,
    val group: Group? = null,
    val error: String? = null
)

class GroupDetailViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ContactBookRepository(app.applicationContext)

    private val ui = MutableStateFlow(GroupDetailUiState())
    val uiState: StateFlow<GroupDetailUiState> = ui.asStateFlow()

    private var observingId: Long? = null
    private var observeJob: Job? = null

    /**
     * 화면 진입 시/탭 재선택 시 호출.
     * - DB Flow를 구독해서 즉시 캐시 표시
     * - 동시에 서버에서 새로고침 → Room 갱신 → UI 자동 갱신
     */
    fun load(id: Long, force: Boolean = false) {
        if (!force && observingId == id) return
        observingId = id

        ui.update { it.copy(isLoading = true, error = null) }

        // 1) DB Flow 구독
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repo.observeGroup(id).collectLatest { group ->
                ui.update { it.copy(group = group) } // null일 수 있으니 UI에서 처리
            }
        }

        // 2) 서버 → DB 동기화 트리거
        viewModelScope.launch {
            try {
                repo.refreshGroupAndMembers(id)
                ui.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                ui.update { it.copy(isLoading = false, error = e.message ?: "동기화 실패") }
            }
        }
    }

    /** 풀투리프레시/새로고침 버튼 등에 연결 */
    fun refresh() {
        val id = observingId ?: return
        ui.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                repo.refreshGroupAndMembers(id)
                ui.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                ui.update { it.copy(isLoading = false, error = e.message ?: "동기화 실패") }
            }
        }
    }

    override fun onCleared() {
        observeJob?.cancel()
        super.onCleared()
    }
}
