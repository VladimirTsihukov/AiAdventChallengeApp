package com.tishukoff.feature.localllm.impl.domain.model

/**
 * Конфигурация параметров локальной LLM.
 *
 * @param model название модели Ollama
 * @param temperature температура генерации (0.0 — детерминированно, 1.0+ — креативно)
 * @param maxTokens максимальное количество токенов в ответе (num_predict)
 * @param contextWindow размер контекстного окна в токенах (num_ctx)
 * @param systemPrompt системный промпт для задания роли/стиля ответов
 * @param repeatPenalty штраф за повторения (1.0 — нет штрафа, >1.0 — снижение повторов)
 * @param topP nucleus sampling порог
 * @param topK количество лучших токенов для выборки
 */
data class LlmConfig(
    val model: String = DEFAULT_MODEL,
    val temperature: Float = DEFAULT_TEMPERATURE,
    val maxTokens: Int = DEFAULT_MAX_TOKENS,
    val contextWindow: Int = DEFAULT_CONTEXT_WINDOW,
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val repeatPenalty: Float = DEFAULT_REPEAT_PENALTY,
    val topP: Float = DEFAULT_TOP_P,
    val topK: Int = DEFAULT_TOP_K,
) {
    companion object {
        const val DEFAULT_MODEL = "phi3:mini"
        const val DEFAULT_TEMPERATURE = 0.7f
        const val DEFAULT_MAX_TOKENS = 2048
        const val DEFAULT_CONTEXT_WINDOW = 4096
        const val DEFAULT_SYSTEM_PROMPT = ""
        const val DEFAULT_REPEAT_PENALTY = 1.0f
        const val DEFAULT_TOP_P = 0.9f
        const val DEFAULT_TOP_K = 40

        val DEFAULT = LlmConfig()

        val OPTIMIZED = LlmConfig(
            temperature = 0.3f,
            maxTokens = 1024,
            contextWindow = 2048,
            repeatPenalty = 1.2f,
            topP = 0.85f,
            topK = 30,
            systemPrompt = "Ты — краткий и точный ассистент для ответов на вопросы на русском языке. " +
                "Правила:\n" +
                "1. Отвечай по существу, без воды и вступлений.\n" +
                "2. Используй структурированные ответы: списки, пункты.\n" +
                "3. Если не знаешь ответа — скажи прямо.\n" +
                "4. Давай конкретные факты, а не общие рассуждения.",
        )
    }
}
