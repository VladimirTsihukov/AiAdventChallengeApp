package com.tishukoff.feature.rag.impl.domain.model

/**
 * Конфигурация расширенного поиска с реранкингом.
 */
data class RagSearchConfig(
    val initialTopK: Int = 10,
    val finalTopK: Int = 3,
    val similarityThreshold: Float = 0.3f,
    val useReranking: Boolean = true,
    val useQueryRewrite: Boolean = true,
)
