package com.tishukoff.feature.setting.impl.presentation

import com.tishukoff.feature.agent.api.LlmSettings

data class SettingUiState(
    val settings: LlmSettings = LlmSettings(),
)
