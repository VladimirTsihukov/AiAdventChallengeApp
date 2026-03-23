package com.tishukoff.feature.localllm.impl.domain.repository

import com.tishukoff.feature.localllm.impl.domain.model.LocalLlmMessage

/**
 * Репозиторий для взаимодействия с локальной LLM.
 */
internal interface LocalLlmRepository {

    /**
     * Отправляет историю сообщений и возвращает ответ модели.
     */
    suspend fun sendMessage(messages: List<LocalLlmMessage>): String
}
