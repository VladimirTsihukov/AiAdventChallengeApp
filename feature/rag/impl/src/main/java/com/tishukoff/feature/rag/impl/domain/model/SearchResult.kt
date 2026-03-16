package com.tishukoff.feature.rag.impl.domain.model

data class SearchResult(
    val chunk: DocumentChunk,
    val score: Float,
)
