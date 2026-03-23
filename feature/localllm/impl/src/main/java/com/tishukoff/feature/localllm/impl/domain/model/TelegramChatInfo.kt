package com.tishukoff.feature.localllm.impl.domain.model

/**
 * Информация об активном чате Telegram-бота.
 */
data class TelegramChatInfo(
    val chatId: Long,
    val messageCount: Int,
    val lastMessage: String,
)
