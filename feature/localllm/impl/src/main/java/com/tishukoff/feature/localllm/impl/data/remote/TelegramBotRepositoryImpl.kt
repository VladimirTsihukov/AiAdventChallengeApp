package com.tishukoff.feature.localllm.impl.data.remote

import com.tishukoff.feature.localllm.impl.domain.model.LocalLlmMessage
import com.tishukoff.feature.localllm.impl.domain.model.TelegramChatInfo
import com.tishukoff.feature.localllm.impl.domain.repository.TelegramBotRepository

internal class TelegramBotRepositoryImpl(
    private val apiClient: TelegramBotApiClient,
) : TelegramBotRepository {

    override suspend fun getChats(): List<TelegramChatInfo> {
        return apiClient.getChats().map { dto ->
            TelegramChatInfo(
                chatId = dto.chatId,
                messageCount = dto.messageCount,
                lastMessage = dto.lastMessage,
            )
        }
    }

    override suspend fun getMessages(chatId: Long): List<LocalLlmMessage> {
        return apiClient.getMessages(chatId).map { dto ->
            LocalLlmMessage(
                text = dto.content,
                role = when (dto.role) {
                    "user" -> LocalLlmMessage.Role.USER
                    else -> LocalLlmMessage.Role.ASSISTANT
                },
            )
        }
    }

    override suspend fun getModelName(): String {
        return apiClient.getStatus().model
    }

    override suspend fun clearHistory(chatId: Long) {
        apiClient.clearHistory(chatId)
    }
}
