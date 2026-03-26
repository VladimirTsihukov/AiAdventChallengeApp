package com.tishukoff.feature.rag.impl.di

import androidx.room.Room
import com.tishukoff.feature.rag.impl.data.chunking.FixedSizeChunker
import com.tishukoff.feature.rag.impl.data.chunking.StructuralChunker
import com.tishukoff.feature.rag.impl.data.local.RagDatabase
import com.tishukoff.feature.rag.impl.data.remote.AnthropicLlmClient
import com.tishukoff.feature.rag.impl.data.remote.OllamaEmbeddingClient
import com.tishukoff.feature.rag.impl.data.remote.OllamaLlmClient
import com.tishukoff.feature.rag.impl.data.repository.RagRepositoryImpl
import com.tishukoff.feature.rag.impl.domain.repository.LlmClient
import com.tishukoff.feature.rag.impl.domain.repository.RagRepository
import com.tishukoff.feature.rag.impl.domain.usecase.ExtractTaskStateUseCase
import com.tishukoff.feature.rag.impl.domain.usecase.IndexDocumentsUseCase
import com.tishukoff.feature.rag.impl.domain.usecase.RerankChunksUseCase
import com.tishukoff.feature.rag.impl.domain.usecase.RewriteQueryUseCase
import com.tishukoff.feature.rag.impl.domain.usecase.SearchDocumentsUseCase
import com.tishukoff.feature.rag.impl.presentation.RagViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val ragModule = module {
    single {
        Room.databaseBuilder(get(), RagDatabase::class.java, "rag_database")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    single { get<RagDatabase>().ragChunkDao() }
    single { OllamaEmbeddingClient() }
    single { FixedSizeChunker() }
    single { StructuralChunker() }
    single<RagRepository> { RagRepositoryImpl(get(), get(), get(), get()) }
    single<LlmClient>(named("cloud")) { AnthropicLlmClient(get(named("anthropicApiKey"))) }
    single<LlmClient>(named("local")) { OllamaLlmClient() }
    factory { IndexDocumentsUseCase(get()) }
    factory { SearchDocumentsUseCase(get()) }
    factory { RewriteQueryUseCase(get(named("cloud"))) }
    factory { RerankChunksUseCase(get(named("cloud"))) }
    factory { ExtractTaskStateUseCase(get(named("cloud"))) }
    viewModel {
        RagViewModel(
            context = get(),
            indexDocumentsUseCase = get(),
            searchDocumentsUseCase = get(),
            ragRepository = get(),
            cloudLlmClient = get(named("cloud")),
            localLlmClient = get(named("local")),
            rewriteQueryUseCase = get(),
            rerankChunksUseCase = get(),
            extractTaskStateUseCase = get(),
        )
    }
}
