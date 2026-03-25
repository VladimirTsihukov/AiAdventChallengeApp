package com.tishukoff.feature.localllm.impl.presentation.telegrambot

/**
 * Интенты экрана Telegram-бота.
 */
sealed interface TelegramBotIntent {
    data object LoadChats : TelegramBotIntent
    data class SelectChat(val chatId: Long) : TelegramBotIntent
    data object RefreshMessages : TelegramBotIntent
    data object ClearHistory : TelegramBotIntent
    data object DismissError : TelegramBotIntent
}
