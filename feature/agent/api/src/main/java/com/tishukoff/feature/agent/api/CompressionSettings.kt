package com.tishukoff.feature.agent.api

data class CompressionSettings(
    val enabled: Boolean = false,
    val recentMessagesToKeep: Int = 10,
    val summarizationBatchSize: Int = 10,
)
