package com.tishukoff.feature.localllm.impl.domain.repository

import com.tishukoff.feature.localllm.impl.domain.model.LocalLlmMessage
import com.tishukoff.feature.localllm.impl.domain.model.TelegramChatInfo

/**
 * Репозиторий для получения данных Telegram-бота.
 */
internal interface TelegramBotRepository {

    /**
     * Возвращает список активных чатов бота.
     */
    suspend fun getChats(): List<TelegramChatInfo>

    /**
     * Возвращает историю сообщений конкретного чата.
     */
    suspend fun getMessages(chatId: Long): List<LocalLlmMessage>

    /**
     * Возвращает название модели, используемой ботом.
     */
    suspend fun getModelName(): String

    /**
     * Удаляет историю переписки конкретного чата.
     */
    suspend fun clearHistory(chatId: Long)
}
