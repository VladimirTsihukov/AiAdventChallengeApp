package com.tishukoff.aiadventchallengeapp.presentation.ui.models

import com.tishukoff.feature.agent.api.ChatMessage
import com.tishukoff.feature.agent.api.LlmSettings

data class ChatUiState(
    val input: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val settings: LlmSettings = LlmSettings()
)
