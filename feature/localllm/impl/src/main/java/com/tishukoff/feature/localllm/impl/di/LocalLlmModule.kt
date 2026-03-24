package com.tishukoff.feature.localllm.impl.di

import com.tishukoff.feature.localllm.impl.data.remote.LocalLlmRepositoryImpl
import com.tishukoff.feature.localllm.impl.data.remote.OllamaGenerateClient
import com.tishukoff.feature.localllm.impl.domain.repository.LocalLlmRepository
import com.tishukoff.feature.localllm.impl.domain.usecase.SendMessageUseCase
import com.tishukoff.feature.localllm.impl.presentation.LocalLlmViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val localLlmModule = module {
    single { OllamaGenerateClient() }
    single<LocalLlmRepository> { LocalLlmRepositoryImpl(get()) }
    factory { SendMessageUseCase(get()) }
    viewModel { LocalLlmViewModel(get()) }
}
