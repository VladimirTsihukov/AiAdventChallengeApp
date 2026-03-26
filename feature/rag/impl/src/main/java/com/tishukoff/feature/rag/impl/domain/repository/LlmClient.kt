package com.tishukoff.feature.rag.impl.domain.repository

/**
 * Клиент для вызова LLM из domain-слоя.
 */
interface LlmClient {

    /**
     * Отправляет промпт и возвращает текстовый ответ.
     */
    suspend fun complete(prompt: String): String

    /**
     * Отправляет список сообщений (role, content) и возвращает текстовый ответ.
     * Поддерживает multi-turn диалог с системным промптом.
     */
    suspend fun chat(
        messages: List<Pair<String, String>>,
        systemPrompt: String = "",
    ): String {
        val combined = buildString {
            if (systemPrompt.isNotBlank()) {
                appendLine("System: $systemPrompt")
                appendLine()
            }
            messages.forEach { (role, content) ->
                appendLine("$role: $content")
            }
        }
        return complete(combined)
    }
}
