package com.tishukoff.feature.localllm.impl.domain.usecase

import com.tishukoff.feature.localllm.impl.domain.model.BenchmarkComparison
import com.tishukoff.feature.localllm.impl.domain.model.BenchmarkEntry
import com.tishukoff.feature.localllm.impl.domain.model.LlmConfig
import com.tishukoff.feature.localllm.impl.domain.model.LocalLlmMessage
import com.tishukoff.feature.localllm.impl.domain.repository.LocalLlmRepository

/**
 * Запускает сравнительный бенчмарк: дефолтная конфигурация vs оптимизированная.
 *
 * Каждый вопрос задаётся отдельно (без истории), чтобы замеры были чистыми.
 */
internal class RunBenchmarkUseCase(
    private val repository: LocalLlmRepository,
) {

    suspend operator fun invoke(
        defaultConfig: LlmConfig = LlmConfig.DEFAULT,
        optimizedConfig: LlmConfig = LlmConfig.OPTIMIZED,
    ): Result<BenchmarkComparison> = runCatching {
        val defaultResults = runSuite(defaultConfig, "По умолчанию")
        val optimizedResults = runSuite(optimizedConfig, "Оптимизированный")

        BenchmarkComparison(
            defaultResults = defaultResults,
            optimizedResults = optimizedResults,
        )
    }

    private suspend fun runSuite(config: LlmConfig, label: String): List<BenchmarkEntry> =
        BENCHMARK_QUESTIONS.map { question ->
            val messages = listOf(
                LocalLlmMessage(text = question, role = LocalLlmMessage.Role.USER),
            )

            val startTime = System.currentTimeMillis()
            val answer = repository.sendMessage(messages, config)
            val duration = System.currentTimeMillis() - startTime

            BenchmarkEntry(
                question = question,
                answer = answer,
                durationMs = duration,
                configLabel = label,
            )
        }

    companion object {
        val BENCHMARK_QUESTIONS = listOf(
            "Что такое машинное обучение? Объясни кратко.",
            "Перечисли 3 преимущества Kotlin перед Java.",
            "Как работает сборщик мусора в JVM?",
            "Что такое REST API? Приведи пример.",
            "Объясни разницу между val и var в Kotlin.",
        )
    }
}
