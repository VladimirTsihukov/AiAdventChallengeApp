package com.tishukoff.feature.invariant.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tishukoff.feature.invariant.api.InvariantCategory
import com.tishukoff.feature.invariant.api.InvariantProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class InvariantViewModel(
    private val invariantProvider: InvariantProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvariantUiState())
    val uiState: StateFlow<InvariantUiState> = _uiState.asStateFlow()

    init {
        invariantProvider.invariants
            .onEach { list ->
                _uiState.value = _uiState.value.copy(invariants = list)
            }
            .launchIn(viewModelScope)
    }

    fun selectCategory(category: InvariantCategory) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun addInvariant(category: InvariantCategory, text: String) {
        invariantProvider.addInvariant(category, text)
    }

    fun removeInvariant(id: String) {
        invariantProvider.removeInvariant(id)
    }

    fun toggleEnabled(id: String, enabled: Boolean) {
        invariantProvider.setEnabled(id, enabled)
    }
}
