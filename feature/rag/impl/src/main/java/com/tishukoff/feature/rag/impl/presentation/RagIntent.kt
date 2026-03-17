package com.tishukoff.feature.rag.impl.presentation

import com.tishukoff.feature.rag.impl.domain.model.ChunkingStrategy

internal sealed interface RagIntent {
    data class UpdateInput(val text: String) : RagIntent
    data object SendMessage : RagIntent
    data object IndexDocuments : RagIntent
    data class SwitchStrategy(val strategy: ChunkingStrategy) : RagIntent
    data object DismissError : RagIntent
}
