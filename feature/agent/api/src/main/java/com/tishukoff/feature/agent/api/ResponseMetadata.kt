package com.tishukoff.feature.agent.api

data class ResponseMetadata(
    val modelId: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val responseTimeMs: Long,
    val costUsd: Double
)
