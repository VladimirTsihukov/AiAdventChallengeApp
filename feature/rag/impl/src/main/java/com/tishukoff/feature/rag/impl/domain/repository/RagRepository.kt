package com.tishukoff.feature.rag.impl.domain.repository

import com.tishukoff.feature.rag.impl.domain.model.ChunkingStrategy
import com.tishukoff.feature.rag.impl.domain.model.DocumentChunk
import com.tishukoff.feature.rag.impl.domain.model.SearchResult

interface RagRepository {

    suspend fun indexDocument(
        fileName: String,
        content: String,
        strategy: ChunkingStrategy,
    ): List<DocumentChunk>

    suspend fun search(
        query: String,
        strategy: ChunkingStrategy,
        topK: Int = 5,
    ): List<SearchResult>

    suspend fun getIndexedChunkCount(strategy: ChunkingStrategy): Int

    suspend fun clearIndex(strategy: ChunkingStrategy)

    suspend fun isIndexed(strategy: ChunkingStrategy): Boolean
}
