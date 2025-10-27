package com.fiveis.xend.ui.contactbook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.repository.ContactBookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    fun load(id: Long, force: Boolean = false) {
        if (!force && ui.value.group?.id == id) return
        ui.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val g = repo.getGroup(id)
                ui.update { it.copy(isLoading = false, group = g, error = null) }
            } catch (e: Exception) {
                ui.update { it.copy(isLoading = false, error = e.message ?: "불러오기 실패") }
            }
        }
    }
}
