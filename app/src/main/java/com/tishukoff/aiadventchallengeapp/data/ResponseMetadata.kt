package com.tishukoff.aiadventchallengeapp.data

data class ResponseMetadata(
    val modelId: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val responseTimeMs: Long,
    val costUsd: Double
)
