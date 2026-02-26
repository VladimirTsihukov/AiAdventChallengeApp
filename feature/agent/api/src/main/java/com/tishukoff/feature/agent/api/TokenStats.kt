package com.tishukoff.feature.agent.api

/**
 * Accumulated token statistics for the current dialog session.
 */
data class TokenStats(
    val totalInputTokens: Int = 0,
    val totalOutputTokens: Int = 0,
    val totalCostUsd: Double = 0.0,
    val requestCount: Int = 0,
    val contextWindow: Int = 200_000,
    val lastRequestInputTokens: Int = 0,
    val lastRequestOutputTokens: Int = 0,
    val lastRequestCostUsd: Double = 0.0,
) {
    val totalTokens: Int get() = totalInputTokens + totalOutputTokens

    val contextUsagePercent: Float
        get() = if (contextWindow > 0) {
            (lastRequestInputTokens.toFloat() / contextWindow) * 100f
        } else 0f

    val isNearLimit: Boolean get() = contextUsagePercent > 80f

    val isOverLimit: Boolean get() = contextUsagePercent > 95f

    val averageInputPerRequest: Int
        get() = if (requestCount > 0) totalInputTokens / requestCount else 0

    val averageOutputPerRequest: Int
        get() = if (requestCount > 0) totalOutputTokens / requestCount else 0
}
