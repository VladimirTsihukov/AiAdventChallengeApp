package com.tishukoff.feature.setting.impl.presentation

import androidx.lifecycle.ViewModel
import com.tishukoff.feature.agent.api.Agent
import com.tishukoff.feature.agent.api.LlmSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingViewModel(
    private val agent: Agent,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingUiState(settings = agent.settings))
    val uiState: StateFlow<SettingUiState> = _uiState.asStateFlow()

    fun saveSettings(settings: LlmSettings) {
        agent.updateSettings(settings)
        _uiState.value = _uiState.value.copy(settings = settings)
    }
}
