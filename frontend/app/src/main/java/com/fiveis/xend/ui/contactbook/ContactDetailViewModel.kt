package com.fiveis.xend.ui.contactbook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.repository.ContactBookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContactDetailUiState(
    val isLoading: Boolean = false,
    val contact: Contact? = null,
    val error: String? = null
)

class ContactDetailViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ContactBookRepository(app.applicationContext)

    private val ui = MutableStateFlow(ContactDetailUiState())
    val uiState: StateFlow<ContactDetailUiState> = ui.asStateFlow()

    fun load(id: Long, force: Boolean = false) {
        if (!force && ui.value.contact?.id == id) return
        ui.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val c = repo.getContact(id)
                ui.update { it.copy(isLoading = false, contact = c, error = null) }
            } catch (e: Exception) {
                ui.update { it.copy(isLoading = false, error = e.message ?: "불러오기 실패") }
            }
        }
    }
}
