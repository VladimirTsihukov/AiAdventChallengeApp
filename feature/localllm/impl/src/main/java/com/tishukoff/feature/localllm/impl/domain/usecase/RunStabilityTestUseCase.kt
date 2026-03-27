package com.tishukoff.feature.localllm.impl.domain.usecase

import com.tishukoff.feature.localllm.impl.domain.model.LlmConfig
import com.tishukoff.feature.localllm.impl.domain.model.LocalLlmMessage
import com.tishukoff.feature.localllm.impl.domain.model.StabilityTestResult
import com.tishukoff.feature.localllm.impl.domain.repository.LocalLlmRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Запускает тест стабильности: отправляет несколько параллельных запросов и собирает метрики.
 */
internal class RunStabilityTestUseCase(
    private val repository: LocalLlmRepository,
) {

    suspend operator fun invoke(
        concurrentRequests: Int = DEFAULT_CONCURRENT,
        config: LlmConfig = LlmConfig.DEFAULT,
    ): Result<StabilityTestResult> = runCatching {
        val testMessage = listOf(
            LocalLlmMessage(text = "Ответь одним словом: да", role = LocalLlmMessage.Role.USER)
        )

        val results = coroutineScope {
            (1..concurrentRequests).map {
                async {
                    val start = System.currentTimeMillis()
                    try {
                        repository.sendMessage(testMessage, config.copy(maxTokens = 32))
                        val duration = System.currentTimeMillis() - start
                        RequestResult(success = true, latencyMs = duration)
                    } catch (e: Exception) {
                        val duration = System.currentTimeMillis() - start
                        RequestResult(
                            success = false,
                            latencyMs = duration,
                            error = e.message ?: "Unknown error",
                        )
                    }
                }
            }.awaitAll()
        }

        val successes = results.filter { it.success }
        val failures = results.filter { !it.success }
        val latencies = results.map { it.latencyMs }

        StabilityTestResult(
            totalRequests = concurrentRequests,
            successCount = successes.size,
            failureCount = failures.size,
            avgLatencyMs = if (latencies.isNotEmpty()) latencies.average().toLong() else 0,
            minLatencyMs = latencies.minOrNull() ?: 0,
            maxLatencyMs = latencies.maxOrNull() ?: 0,
            errors = failures.map { it.error },
        )
    }

    private data class RequestResult(
        val success: Boolean,
        val latencyMs: Long,
        val error: String = "",
    )

    companion object {
        private const val DEFAULT_CONCURRENT = 5
    }
}
