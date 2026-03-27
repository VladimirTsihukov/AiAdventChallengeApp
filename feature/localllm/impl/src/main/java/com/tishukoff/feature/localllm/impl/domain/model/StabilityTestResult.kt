package com.tishukoff.feature.localllm.impl.domain.model

/**
 * Результат теста стабильности сервера.
 *
 * @param totalRequests общее количество запросов
 * @param successCount количество успешных запросов
 * @param failureCount количество неудачных запросов
 * @param avgLatencyMs среднее время отклика
 * @param minLatencyMs минимальное время отклика
 * @param maxLatencyMs максимальное время отклика
 * @param errors список ошибок
 */
data class StabilityTestResult(
    val totalRequests: Int,
    val successCount: Int,
    val failureCount: Int,
    val avgLatencyMs: Long,
    val minLatencyMs: Long,
    val maxLatencyMs: Long,
    val errors: List<String> = emptyList(),
) {
    val successRate: Float
        get() = if (totalRequests > 0) successCount.toFloat() / totalRequests * 100f else 0f
}
