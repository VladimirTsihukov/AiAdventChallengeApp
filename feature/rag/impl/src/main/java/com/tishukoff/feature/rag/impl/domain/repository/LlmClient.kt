package com.tishukoff.feature.rag.impl.domain.repository

/**
 * Клиент для вызова LLM из domain-слоя.
 */
interface LlmClient {

    /**
     * Отправляет промпт и возвращает текстовый ответ.
     */
    suspend fun complete(prompt: String): String
}
