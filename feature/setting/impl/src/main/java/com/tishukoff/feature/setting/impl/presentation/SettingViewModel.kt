package com.tishukoff.feature.setting.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tishukoff.feature.agent.api.Agent
import com.tishukoff.feature.agent.api.LlmSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SettingViewModel(
    private val agent: Agent,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingUiState())
    val uiState: StateFlow<SettingUiState> = _uiState.asStateFlow()

    init {
        agent.settings
            .onEach { settings ->
                _uiState.value = _uiState.value.copy(settings = settings)
            }
            .launchIn(viewModelScope)
    }

    fun saveSettings(settings: LlmSettings) {
        agent.updateSettings(settings)
    }
}
