package com.tishukoff.feature.rag.impl.presentation

import com.tishukoff.feature.rag.impl.domain.model.ChunkingStrategy

internal data class RagUiState(
    val fixedSizeMessages: List<RagChatMessage> = emptyList(),
    val structuralMessages: List<RagChatMessage> = emptyList(),
    val input: String = "",
    val isLoading: Boolean = false,
    val isIndexing: Boolean = false,
    val indexingProgress: String = "",
    val currentStrategy: ChunkingStrategy = ChunkingStrategy.FIXED_SIZE,
    val fixedSizeChunkCount: Int = 0,
    val structuralChunkCount: Int = 0,
    val error: String? = null,
) {
    val messages: List<RagChatMessage>
        get() = when (currentStrategy) {
            ChunkingStrategy.FIXED_SIZE -> fixedSizeMessages
            ChunkingStrategy.STRUCTURAL -> structuralMessages
        }
}

internal data class RagChatMessage(
    val text: String,
    val isUser: Boolean,
    val sources: List<SourceInfo> = emptyList(),
)

internal data class SourceInfo(
    val fileName: String,
    val section: String,
    val score: Float,
    val chunkPreview: String,
)
