package com.tishukoff.feature.localllm.impl.domain.repository

import com.tishukoff.feature.localllm.impl.domain.model.LlmConfig
import com.tishukoff.feature.localllm.impl.domain.model.LocalLlmMessage
import com.tishukoff.feature.localllm.impl.domain.model.ModelInfo
import com.tishukoff.feature.localllm.impl.domain.model.ServerStatus

/**
 * Репозиторий для взаимодействия с локальной LLM.
 */
internal interface LocalLlmRepository {

    /**
     * Отправляет историю сообщений и возвращает ответ модели.
     *
     * @param messages история диалога
     * @param config конфигурация параметров LLM
     */
    suspend fun sendMessage(
        messages: List<LocalLlmMessage>,
        config: LlmConfig = LlmConfig.DEFAULT,
    ): String

    /**
     * Проверяет доступность сервера и возвращает его статус.
     */
    suspend fun checkHealth(): ServerStatus

    /**
     * Возвращает список доступных моделей.
     */
    suspend fun listModels(): List<ModelInfo>

    /**
     * Возвращает версию Ollama.
     */
    suspend fun getVersion(): String
}
