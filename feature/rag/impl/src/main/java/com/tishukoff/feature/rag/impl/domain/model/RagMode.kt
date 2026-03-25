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
    MEMORY_RAG("RAG + Memory"),
    LOCAL_FIXED("Local Fixed"),
    LOCAL_STRUCTURAL("Local Structural"),
    LOCAL_NO_RAG("Local No RAG"),
    COMPARISON("Compare"),
}

/**
 * Преобразование [RagMode] в [ChunkingStrategy] для режимов, использующих RAG.
 * Возвращает `null` для режимов без RAG.
 */
fun RagMode.toChunkingStrategy(): ChunkingStrategy? = when (this) {
    RagMode.FIXED_SIZE -> ChunkingStrategy.FIXED_SIZE
    RagMode.STRUCTURAL -> ChunkingStrategy.STRUCTURAL
    RagMode.NO_RAG -> null
    RagMode.FIXED_RERANKED -> ChunkingStrategy.FIXED_SIZE
    RagMode.STRUCTURAL_RERANKED -> ChunkingStrategy.STRUCTURAL
    RagMode.MEMORY_RAG -> ChunkingStrategy.STRUCTURAL
    RagMode.LOCAL_FIXED -> ChunkingStrategy.FIXED_SIZE
    RagMode.LOCAL_STRUCTURAL -> ChunkingStrategy.STRUCTURAL
    RagMode.LOCAL_NO_RAG -> null
    RagMode.COMPARISON -> ChunkingStrategy.STRUCTURAL
}

/**
 * Возвращает `true`, если режим использует реранкинг.
 */
val RagMode.isReranked: Boolean
    get() = this == RagMode.FIXED_RERANKED || this == RagMode.STRUCTURAL_RERANKED

/**
 * Возвращает `true`, если режим использует локальную LLM.
 */
val RagMode.isLocal: Boolean
    get() = this == RagMode.LOCAL_FIXED ||
        this == RagMode.LOCAL_STRUCTURAL ||
        this == RagMode.LOCAL_NO_RAG

/**
 * Возвращает `true`, если режим сравнения cloud vs local.
 */
val RagMode.isComparison: Boolean
    get() = this == RagMode.COMPARISON
