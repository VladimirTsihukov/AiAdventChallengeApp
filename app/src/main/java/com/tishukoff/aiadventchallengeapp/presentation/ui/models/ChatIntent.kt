package com.tishukoff.aiadventchallengeapp.presentation.ui.models

sealed interface ChatIntent {
    data class UpdateInput(val text: String) : ChatIntent
    data object SendMessage : ChatIntent
    data object ClearHistory : ChatIntent
    data object NewChat : ChatIntent
    data class SelectChat(val chatId: Long) : ChatIntent
    data class DeleteChat(val chatId: Long) : ChatIntent
}
