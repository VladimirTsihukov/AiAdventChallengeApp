package com.tishukoff.feature.localllm.impl.data.remote

import com.tishukoff.feature.localllm.impl.domain.model.LlmConfig
import com.tishukoff.feature.localllm.impl.domain.model.LocalLlmMessage
import com.tishukoff.feature.localllm.impl.domain.model.ModelInfo
import com.tishukoff.feature.localllm.impl.domain.model.ServerStatus
import com.tishukoff.feature.localllm.impl.domain.repository.LocalLlmRepository

internal class LocalLlmRepositoryImpl(
    private val ollamaClient: OllamaGenerateClient,
) : LocalLlmRepository {

    override suspend fun sendMessage(
        messages: List<LocalLlmMessage>,
        config: LlmConfig,
    ): String {
        val ollamaMessages = messages.map { msg ->
            val role = when (msg.role) {
                LocalLlmMessage.Role.USER -> "user"
                LocalLlmMessage.Role.ASSISTANT -> "assistant"
            }
            role to msg.text
        }
        return ollamaClient.chat(ollamaMessages, config)
    }

    override suspend fun checkHealth(): ServerStatus {
        val responseTime = ollamaClient.ping()
        if (responseTime < 0) {
            return ServerStatus(isOnline = false)
        }

        val version = try {
            ollamaClient.getVersion()
        } catch (_: Exception) {
            ""
        }

        val models = try {
            ollamaClient.listModels()
        } catch (_: Exception) {
            emptyList()
        }

        return ServerStatus(
            isOnline = true,
            version = version,
            models = models,
            responseTimeMs = responseTime,
        )
    }

    override suspend fun listModels(): List<ModelInfo> = ollamaClient.listModels()

    override suspend fun getVersion(): String = ollamaClient.getVersion()
}
