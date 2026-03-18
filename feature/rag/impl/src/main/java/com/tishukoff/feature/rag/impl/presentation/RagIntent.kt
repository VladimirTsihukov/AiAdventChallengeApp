package com.tishukoff.feature.rag.impl.presentation

import com.tishukoff.feature.rag.impl.domain.model.RagMode

internal sealed interface RagIntent {
    data class UpdateInput(val text: String) : RagIntent
    data object SendMessage : RagIntent
    data object IndexDocuments : RagIntent
    data class SwitchMode(val mode: RagMode) : RagIntent
    data object DismissError : RagIntent
    data object RunBenchmark : RagIntent
    data object DismissBenchmarkResult : RagIntent
}
