package com.tishukoff.feature.rag.impl.presentation

import com.tishukoff.feature.rag.impl.domain.model.BenchmarkResult
import com.tishukoff.feature.rag.impl.domain.model.RagMode

internal data class RagUiState(
    val fixedSizeMessages: List<RagChatMessage> = emptyList(),
    val structuralMessages: List<RagChatMessage> = emptyList(),
    val noRagMessages: List<RagChatMessage> = emptyList(),
    val fixedRerankedMessages: List<RagChatMessage> = emptyList(),
    val structuralRerankedMessages: List<RagChatMessage> = emptyList(),
    val input: String = "",
    val isLoading: Boolean = false,
    val isIndexing: Boolean = false,
    val indexingProgress: String = "",
    val currentMode: RagMode = RagMode.FIXED_SIZE,
    val fixedSizeChunkCount: Int = 0,
    val structuralChunkCount: Int = 0,
    val error: String? = null,
    val isBenchmarkRunning: Boolean = false,
    val benchmarkProgress: String = "",
    val benchmarkResult: BenchmarkResult? = null,
    val similarityThreshold: Float = 0.3f,
    val initialTopK: Int = 10,
    val finalTopK: Int = 3,
) {
    val messages: List<RagChatMessage>
        get() = when (currentMode) {
            RagMode.FIXED_SIZE -> fixedSizeMessages
            RagMode.STRUCTURAL -> structuralMessages
            RagMode.NO_RAG -> noRagMessages
            RagMode.FIXED_RERANKED -> fixedRerankedMessages
            RagMode.STRUCTURAL_RERANKED -> structuralRerankedMessages
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
