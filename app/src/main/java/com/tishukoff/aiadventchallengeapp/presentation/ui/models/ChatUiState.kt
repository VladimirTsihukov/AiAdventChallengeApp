package com.tishukoff.aiadventchallengeapp.presentation.ui.models

import com.tishukoff.feature.agent.api.ChatMessage

data class ChatUiState(
    val input: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
)
