package com.tishukoff.feature.agent.api

enum class ClaudeModel(
    val apiId: String,
    val displayName: String,
    val inputPricePerMillion: Double,
    val outputPricePerMillion: Double
) {
    HAIKU("claude-haiku-4-5-20251001", "Haiku 4.5 (fast)", 1.00, 5.00),
    SONNET("claude-sonnet-4-5-20250929", "Sonnet 4.5 (balanced)", 3.00, 15.00),
    OPUS("claude-opus-4-20250514", "Opus 4 (strong)", 15.00, 75.00)
}
