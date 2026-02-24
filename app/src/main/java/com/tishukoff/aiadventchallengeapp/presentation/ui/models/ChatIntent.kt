package com.tishukoff.aiadventchallengeapp.presentation.ui.models

sealed interface ChatIntent {
    data class UpdateInput(val text: String) : ChatIntent
    data object SendMessage : ChatIntent
    data object ClearHistory : ChatIntent
}
