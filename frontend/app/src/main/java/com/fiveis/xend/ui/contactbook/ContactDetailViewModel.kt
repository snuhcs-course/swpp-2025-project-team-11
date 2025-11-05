package com.fiveis.xend.ui.contactbook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.repository.ContactBookRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContactDetailUiState(
    val isLoading: Boolean = false,
    val contact: Contact? = null,
    val error: String? = null
)

class ContactDetailViewModel(
    app: Application,
    private val repo: ContactBookRepository = ContactBookRepository(app.applicationContext)
) : AndroidViewModel(app) {

    private val ui = MutableStateFlow(ContactDetailUiState())
    val uiState: StateFlow<ContactDetailUiState> = ui.asStateFlow()

    private var currentId: Long? = null
    private var job: Job? = null

    fun load(id: Long, force: Boolean = false) {
        if (!force && currentId == id) return
        currentId = id

        ui.update { it.copy(isLoading = true, error = null) }

        // 1) DB Flow
        job?.cancel()
        job = viewModelScope.launch {
            repo.observeContact(id).collectLatest { c ->
                ui.update { it.copy(contact = c) }
            }
        }

        // 2) 서버 → DB 동기화
        viewModelScope.launch {
            runCatching { repo.refreshContact(id) }
                .onSuccess { ui.update { it.copy(isLoading = false) } }
                .onFailure { e -> ui.update { it.copy(isLoading = false, error = e.message ?: "동기화 실패") } }
        }
    }

    fun refresh() {
        val id = currentId ?: return
        ui.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { repo.refreshContact(id) }
                .onSuccess { ui.update { it.copy(isLoading = false) } }
                .onFailure { e -> ui.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    override fun onCleared() {
        job?.cancel()
        super.onCleared()
    }
}
