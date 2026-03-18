package com.tishukoff.feature.rag.impl.domain.usecase

import com.tishukoff.feature.rag.impl.domain.model.SearchResult
import com.tishukoff.feature.rag.impl.domain.repository.LlmClient

/**
 * Реранкинг результатов поиска с помощью LLM.
 * LLM оценивает релевантность каждого чанка запросу по шкале 0-10.
 */
class RerankChunksUseCase(
    private val llmClient: LlmClient,
) {

    suspend operator fun invoke(
        query: String,
        results: List<SearchResult>,
        topK: Int,
    ): Result<List<SearchResult>> = runCatching {
        if (results.isEmpty()) return@runCatching emptyList()

        val prompt = buildString {
            append("Оцени релевантность каждого текстового фрагмента запросу пользователя ")
            append("по шкале от 0 до 10 (10 = максимально релевантен).\n")
            append("Верни ТОЛЬКО числа через запятую в том же порядке, без пояснений.\n\n")
            append("Запрос: $query\n\n")
            results.forEachIndexed { index, result ->
                append("Фрагмент ${index + 1}:\n${result.chunk.text.take(500)}\n\n")
            }
            append("Оценки (числа через запятую):")
        }

        val response = llmClient.complete(prompt)
        val scores = parseScores(response, results.size)

        results
            .zip(scores)
            .sortedByDescending { it.second }
            .take(topK)
            .map { (result, rerankScore) ->
                result.copy(score = rerankScore / 10f)
            }
    }

    private fun parseScores(response: String, expectedCount: Int): List<Float> {
        val parsed = response
            .replace(Regex("[^0-9.,]"), "")
            .split(",")
            .mapNotNull { it.trim().toFloatOrNull() }

        if (parsed.size >= expectedCount) return parsed.take(expectedCount)

        return parsed + List(expectedCount - parsed.size) { 5f }
    }
}
