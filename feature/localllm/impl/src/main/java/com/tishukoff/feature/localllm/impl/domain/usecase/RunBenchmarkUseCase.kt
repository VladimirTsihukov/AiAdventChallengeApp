package com.tishukoff.feature.localllm.impl.domain.usecase

import com.tishukoff.feature.localllm.impl.domain.model.BenchmarkComparison
import com.tishukoff.feature.localllm.impl.domain.model.BenchmarkEntry
import com.tishukoff.feature.localllm.impl.domain.model.BenchmarkEvent
import com.tishukoff.feature.localllm.impl.domain.model.LlmConfig
import com.tishukoff.feature.localllm.impl.domain.model.LocalLlmMessage
import com.tishukoff.feature.localllm.impl.domain.repository.LocalLlmRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

/**
 * Запускает сравнительный бенчмарк: дефолтная конфигурация vs оптимизированная.
 *
 * Эмитит события прогресса по мере выполнения каждого вопроса.
 */
internal class RunBenchmarkUseCase(
    private val repository: LocalLlmRepository,
) {

    operator fun invoke(
        defaultConfig: LlmConfig = LlmConfig.DEFAULT,
        optimizedConfig: LlmConfig = LlmConfig.OPTIMIZED,
    ): Flow<BenchmarkEvent> = flow {
        val defaultResults = runSuite(defaultConfig, "По умолчанию")
        emit(BenchmarkEvent.SuiteCompleted("По умолчанию"))

        val optimizedResults = runSuite(optimizedConfig, "Оптимизированный")
        emit(BenchmarkEvent.SuiteCompleted("Оптимизированный"))

        emit(
            BenchmarkEvent.Finished(
                BenchmarkComparison(
                    defaultResults = defaultResults,
                    optimizedResults = optimizedResults,
                )
            )
        )
    }

    private suspend fun FlowCollector<BenchmarkEvent>.runSuite(
        config: LlmConfig,
        label: String,
    ): List<BenchmarkEntry> {
        val results = mutableListOf<BenchmarkEntry>()

        BENCHMARK_QUESTIONS.forEachIndexed { index, question ->
            emit(
                BenchmarkEvent.AskingQuestion(
                    questionIndex = index + 1,
                    totalQuestions = BENCHMARK_QUESTIONS.size,
                    question = question,
                    configLabel = label,
                )
            )

            val messages = listOf(
                LocalLlmMessage(text = question, role = LocalLlmMessage.Role.USER),
            )

            val startTime = System.currentTimeMillis()
            val answer = repository.sendMessage(messages, config)
            val duration = System.currentTimeMillis() - startTime

            val entry = BenchmarkEntry(
                question = question,
                answer = answer,
                durationMs = duration,
                configLabel = label,
            )
            results.add(entry)
            emit(BenchmarkEvent.EntryCompleted(entry))
        }

        return results
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
