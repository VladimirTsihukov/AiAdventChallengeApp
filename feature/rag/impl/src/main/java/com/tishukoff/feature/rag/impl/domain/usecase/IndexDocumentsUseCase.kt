package com.tishukoff.feature.rag.impl.domain.usecase

import com.tishukoff.feature.rag.impl.domain.model.ChunkingStrategy
import com.tishukoff.feature.rag.impl.domain.model.RagDocument
import com.tishukoff.feature.rag.impl.domain.repository.RagRepository

class IndexDocumentsUseCase(
    private val repository: RagRepository,
) {

    suspend operator fun invoke(
        documents: List<RagDocument>,
        strategy: ChunkingStrategy,
    ): Result<Int> = runCatching {
        repository.clearIndex(strategy)
        var totalChunks = 0
        for (doc in documents) {
            val chunks = repository.indexDocument(doc.fileName, doc.content, strategy)
            totalChunks += chunks.size
        }
        totalChunks
    }
}
