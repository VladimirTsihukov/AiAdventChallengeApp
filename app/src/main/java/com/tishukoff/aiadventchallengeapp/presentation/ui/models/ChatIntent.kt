package com.tishukoff.aiadventchallengeapp.presentation.ui.models

import com.tishukoff.aiadventchallengeapp.data.LlmSettings

sealed interface ChatIntent {
    data class UpdateInput(val text: String) : ChatIntent
    data object SendMessage : ChatIntent
    data class SaveSettings(val settings: LlmSettings) : ChatIntent
}
