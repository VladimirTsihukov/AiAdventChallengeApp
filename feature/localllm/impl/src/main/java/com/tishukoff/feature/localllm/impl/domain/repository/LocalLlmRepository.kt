package com.tishukoff.feature.localllm.impl.domain.repository

import com.tishukoff.feature.localllm.impl.domain.model.LlmConfig
import com.tishukoff.feature.localllm.impl.domain.model.LocalLlmMessage

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
}
