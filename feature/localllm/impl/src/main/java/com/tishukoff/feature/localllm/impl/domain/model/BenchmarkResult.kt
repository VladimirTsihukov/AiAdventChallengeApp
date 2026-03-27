package com.tishukoff.feature.localllm.impl.domain.model

/**
 * Результат одного запроса бенчмарка.
 */
data class BenchmarkEntry(
    val question: String,
    val answer: String,
    val durationMs: Long,
    val configLabel: String,
)

/**
 * Промежуточное событие прогресса бенчмарка.
 */
sealed interface BenchmarkEvent {
    data class AskingQuestion(
        val questionIndex: Int,
        val totalQuestions: Int,
        val question: String,
        val configLabel: String,
    ) : BenchmarkEvent

    data class EntryCompleted(val entry: BenchmarkEntry) : BenchmarkEvent

    data class SuiteCompleted(val configLabel: String) : BenchmarkEvent

    data class Finished(val comparison: BenchmarkComparison) : BenchmarkEvent
}

/**
 * Полный результат сравнительного бенчмарка — "до" и "после" оптимизации.
 */
data class BenchmarkComparison(
    val defaultResults: List<BenchmarkEntry>,
    val optimizedResults: List<BenchmarkEntry>,
) {
    val defaultAvgMs: Long
        get() = defaultResults.map { it.durationMs }.average().toLong()

    val optimizedAvgMs: Long
        get() = optimizedResults.map { it.durationMs }.average().toLong()

    val speedupPercent: Int
        get() = if (defaultAvgMs > 0) {
            ((defaultAvgMs - optimizedAvgMs) * 100 / defaultAvgMs).toInt()
        } else {
            0
        }

    val defaultAvgLength: Int
        get() = defaultResults.map { it.answer.length }.average().toInt()

    val optimizedAvgLength: Int
        get() = optimizedResults.map { it.answer.length }.average().toInt()
}
