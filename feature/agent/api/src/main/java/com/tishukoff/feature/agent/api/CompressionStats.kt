package com.tishukoff.feature.agent.api

data class CompressionStats(
    val isEnabled: Boolean = false,
    val summaryCount: Int = 0,
    val originalTokenEstimate: Int = 0,
    val compressedTokenEstimate: Int = 0,
    val tokensSaved: Int = 0,
    val compressionRatio: Float = 0f,
)
