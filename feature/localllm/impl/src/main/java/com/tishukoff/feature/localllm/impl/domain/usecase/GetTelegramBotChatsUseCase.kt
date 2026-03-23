package com.tishukoff.feature.localllm.impl.domain.usecase

import com.tishukoff.feature.localllm.impl.domain.model.LocalLlmMessage
import com.tishukoff.feature.localllm.impl.domain.model.TelegramChatInfo
import com.tishukoff.feature.localllm.impl.domain.repository.TelegramBotRepository

/**
 * Получает список чатов и сообщения из Telegram-бота.
 */
internal class GetTelegramBotChatsUseCase(
    private val repository: TelegramBotRepository,
) {

    suspend fun getChats(): Result<List<TelegramChatInfo>> =
        runCatching { repository.getChats() }

    suspend fun getMessages(chatId: Long): Result<List<LocalLlmMessage>> =
        runCatching { repository.getMessages(chatId) }

    suspend fun getModelName(): Result<String> =
        runCatching { repository.getModelName() }

    suspend fun clearHistory(chatId: Long): Result<Unit> =
        runCatching { repository.clearHistory(chatId) }
}
