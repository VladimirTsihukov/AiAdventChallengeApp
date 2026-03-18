package com.tishukoff.feature.rag.impl.domain.usecase

import com.tishukoff.feature.rag.impl.domain.repository.LlmClient

/**
 * Переформулирует пользовательский запрос для улучшения качества поиска.
 */
class RewriteQueryUseCase(
    private val llmClient: LlmClient,
) {

    suspend operator fun invoke(query: String): Result<String> = runCatching {
        val prompt = buildString {
            append("Перепиши следующий пользовательский запрос так, чтобы он лучше подходил ")
            append("для семантического поиска по документам. ")
            append("Сделай запрос более конкретным и добавь ключевые слова-синонимы. ")
            append("Верни ТОЛЬКО переписанный запрос, без пояснений.\n\n")
            append("Оригинальный запрос: $query")
        }
        llmClient.complete(prompt)
    }
}
