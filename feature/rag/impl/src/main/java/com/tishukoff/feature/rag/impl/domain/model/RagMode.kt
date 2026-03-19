package com.tishukoff.feature.rag.impl.domain.model

/**
 * Режим работы RAG-агента.
 */
enum class RagMode(val displayName: String) {
    FIXED_SIZE("Fixed Size"),
    STRUCTURAL("Structural"),
    NO_RAG("No RAG"),
    FIXED_RERANKED("Fixed + Rerank"),
    STRUCTURAL_RERANKED("Structural + Rerank"),
}

/**
 * Преобразование [RagMode] в [ChunkingStrategy] для режимов, использующих RAG.
 * Возвращает `null` для [RagMode.NO_RAG].
 */
fun RagMode.toChunkingStrategy(): ChunkingStrategy? = when (this) {
    RagMode.FIXED_SIZE -> ChunkingStrategy.FIXED_SIZE
    RagMode.STRUCTURAL -> ChunkingStrategy.STRUCTURAL
    RagMode.NO_RAG -> null
    RagMode.FIXED_RERANKED -> ChunkingStrategy.FIXED_SIZE
    RagMode.STRUCTURAL_RERANKED -> ChunkingStrategy.STRUCTURAL
}

/**
 * Возвращает `true`, если режим использует реранкинг.
 */
val RagMode.isReranked: Boolean
    get() = this == RagMode.FIXED_RERANKED || this == RagMode.STRUCTURAL_RERANKED
