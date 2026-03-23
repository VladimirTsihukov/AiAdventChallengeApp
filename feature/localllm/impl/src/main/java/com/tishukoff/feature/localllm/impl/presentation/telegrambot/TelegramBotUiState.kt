package com.tishukoff.feature.localllm.impl.presentation.telegrambot

import com.tishukoff.feature.localllm.impl.domain.model.LocalLlmMessage
import com.tishukoff.feature.localllm.impl.domain.model.TelegramChatInfo

/**
 * UI-состояние экрана Telegram-бота.
 */
data class TelegramBotUiState(
    val chats: List<TelegramChatInfo> = emptyList(),
    val selectedChatId: Long? = null,
    val messages: List<LocalLlmMessage> = emptyList(),
    val modelName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)
