package com.tishukoff.feature.rag.impl.domain.model

data class DocumentChunk(
    val id: Long = 0,
    val text: String,
    val embedding: FloatArray,
    val metadata: ChunkMetadata,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DocumentChunk) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

data class ChunkMetadata(
    val source: String,
    val title: String,
    val section: String,
    val chunkIndex: Int,
    val strategy: ChunkingStrategy,
)
