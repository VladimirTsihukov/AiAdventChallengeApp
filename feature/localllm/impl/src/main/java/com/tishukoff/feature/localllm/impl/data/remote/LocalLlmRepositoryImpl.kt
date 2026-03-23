package com.tishukoff.feature.localllm.impl.data.remote

import com.tishukoff.feature.localllm.impl.domain.model.LocalLlmMessage
import com.tishukoff.feature.localllm.impl.domain.repository.LocalLlmRepository

internal class LocalLlmRepositoryImpl(
    private val ollamaClient: OllamaGenerateClient,
) : LocalLlmRepository {

    override suspend fun sendMessage(messages: List<LocalLlmMessage>): String {
        val ollamaMessages = messages.map { msg ->
            val role = when (msg.role) {
                LocalLlmMessage.Role.USER -> "user"
                LocalLlmMessage.Role.ASSISTANT -> "assistant"
            }
            role to msg.text
        }
        return ollamaClient.chat(ollamaMessages)
    }
}
