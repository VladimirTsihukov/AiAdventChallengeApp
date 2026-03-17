package com.tishukoff.feature.rag.impl.domain.usecase

import com.tishukoff.feature.rag.impl.domain.model.ChunkingStrategy
import com.tishukoff.feature.rag.impl.domain.model.SearchResult
import com.tishukoff.feature.rag.impl.domain.repository.RagRepository

class SearchDocumentsUseCase(
    private val repository: RagRepository,
) {

    suspend operator fun invoke(
        query: String,
        strategy: ChunkingStrategy,
        topK: Int = 5,
    ): Result<List<SearchResult>> = runCatching {
        repository.search(query, strategy, topK)
    }
}
