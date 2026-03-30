package com.tishukoff.feature.localllm.impl.domain.model

/**
 * Конфигурация клиентского rate limiting.
 *
 * @param maxRequestsPerMinute максимальное количество запросов в минуту
 * @param maxConcurrentRequests максимальное количество одновременных запросов
 * @param maxContextTokens максимальный размер контекста в токенах
 */
data class RateLimitConfig(
    val maxRequestsPerMinute: Int = DEFAULT_MAX_RPM,
    val maxConcurrentRequests: Int = DEFAULT_MAX_CONCURRENT,
    val maxContextTokens: Int = DEFAULT_MAX_CONTEXT,
) {
    companion object {
        const val DEFAULT_MAX_RPM = 10
        const val DEFAULT_MAX_CONCURRENT = 2
        const val DEFAULT_MAX_CONTEXT = 8192

        val DEFAULT = RateLimitConfig()
    }
}
