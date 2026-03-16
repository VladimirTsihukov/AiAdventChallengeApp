package com.tishukoff.feature.rag.impl.data.repository

import com.tishukoff.feature.rag.impl.data.chunking.ChunkResult
import com.tishukoff.feature.rag.impl.data.chunking.FixedSizeChunker
import com.tishukoff.feature.rag.impl.data.chunking.StructuralChunker
import com.tishukoff.feature.rag.impl.data.local.EmbeddingConverter
import com.tishukoff.feature.rag.impl.data.local.RagChunkDao
import com.tishukoff.feature.rag.impl.data.local.RagChunkEntity
import com.tishukoff.feature.rag.impl.data.remote.OllamaEmbeddingClient
import com.tishukoff.feature.rag.impl.domain.model.ChunkMetadata
import com.tishukoff.feature.rag.impl.domain.model.ChunkingStrategy
import com.tishukoff.feature.rag.impl.domain.model.DocumentChunk
import com.tishukoff.feature.rag.impl.domain.model.SearchResult
import com.tishukoff.feature.rag.impl.domain.repository.RagRepository

internal class RagRepositoryImpl(
    private val dao: RagChunkDao,
    private val embeddingClient: OllamaEmbeddingClient,
    private val fixedSizeChunker: FixedSizeChunker,
    private val structuralChunker: StructuralChunker,
) : RagRepository {

    override suspend fun indexDocument(
        fileName: String,
        content: String,
        strategy: ChunkingStrategy,
    ): List<DocumentChunk> {
        val title = extractDocumentTitle(content, fileName)
        val chunkResults = when (strategy) {
            ChunkingStrategy.FIXED_SIZE -> fixedSizeChunker.chunk(content, fileName)
            ChunkingStrategy.STRUCTURAL -> structuralChunker.chunk(content, fileName)
        }

        val entities = chunkResults.map { chunk ->
            val embedding = embeddingClient.getEmbedding(chunk.text)
            RagChunkEntity(
                text = chunk.text,
                embedding = EmbeddingConverter.toByteArray(embedding),
                source = fileName,
                title = title,
                section = chunk.section,
                chunkIndex = chunk.chunkIndex,
                strategy = strategy.name,
            )
        }

        dao.insertAll(entities)

        return entities.map { it.toDomain(strategy) }
    }

    override suspend fun search(
        query: String,
        strategy: ChunkingStrategy,
        topK: Int,
    ): List<SearchResult> {
        val queryEmbedding = embeddingClient.getEmbedding(query)
        val allChunks = dao.getChunksByStrategy(strategy.name)

        return allChunks
            .map { entity ->
                val chunkEmbedding = EmbeddingConverter.toFloatArray(entity.embedding)
                val score = CosineSimilarity.compute(queryEmbedding, chunkEmbedding)
                SearchResult(
                    chunk = entity.toDomain(strategy),
                    score = score,
                )
            }
            .sortedByDescending { it.score }
            .take(topK)
    }

    override suspend fun getIndexedChunkCount(strategy: ChunkingStrategy): Int {
        return dao.getChunkCount(strategy.name)
    }

    override suspend fun clearIndex(strategy: ChunkingStrategy) {
        dao.deleteByStrategy(strategy.name)
    }

    override suspend fun isIndexed(strategy: ChunkingStrategy): Boolean {
        return dao.getChunkCount(strategy.name) > 0
    }

    private fun extractDocumentTitle(content: String, fallback: String): String {
        val firstLine = content.lineSequence().firstOrNull()?.trim() ?: return fallback
        return if (firstLine.startsWith("#")) {
            firstLine.removePrefix("#").trim()
        } else {
            fallback.removeSuffix(".md").removeSuffix(".txt")
        }
    }

    private fun RagChunkEntity.toDomain(strategy: ChunkingStrategy) = DocumentChunk(
        id = id,
        text = text,
        embedding = EmbeddingConverter.toFloatArray(embedding),
        metadata = ChunkMetadata(
            source = source,
            title = title,
            section = section,
            chunkIndex = chunkIndex,
            strategy = strategy,
        ),
    )
}
