package com.tishukoff.aiadventchallengeapp.presentation.ui.models

import com.tishukoff.aiadventchallengeapp.data.LlmSettings

data class ChatUiState(
    val input: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val settings: LlmSettings = LlmSettings()
)
