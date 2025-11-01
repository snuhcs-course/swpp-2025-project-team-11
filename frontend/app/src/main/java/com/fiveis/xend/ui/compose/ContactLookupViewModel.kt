package com.fiveis.xend.ui.compose

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.repository.ContactBookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private fun normalizeEmail(s: String) = s.trim().lowercase()

class ContactLookupViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ContactBookRepository(app.applicationContext)

    private val _byEmail = MutableStateFlow<Map<String, Contact>>(emptyMap())
    val byEmail: StateFlow<Map<String, Contact>> = _byEmail.asStateFlow()

    init {
        // 연락처 DB observe
        viewModelScope.launch {
            repo.observeContacts().collectLatest { list ->
                _byEmail.value = list.associateBy { normalizeEmail(it.email) }
            }
        }
    }

    fun lookup(email: String): Contact? = _byEmail.value[normalizeEmail(email)]
}
