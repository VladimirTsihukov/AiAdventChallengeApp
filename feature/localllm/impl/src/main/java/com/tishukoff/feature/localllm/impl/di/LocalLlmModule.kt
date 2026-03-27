package com.tishukoff.feature.localllm.impl.di

import com.tishukoff.feature.localllm.impl.data.remote.LocalLlmRepositoryImpl
import com.tishukoff.feature.localllm.impl.data.remote.OllamaGenerateClient
import com.tishukoff.feature.localllm.impl.data.remote.TelegramBotApiClient
import com.tishukoff.feature.localllm.impl.data.remote.TelegramBotRepositoryImpl
import com.tishukoff.feature.localllm.impl.domain.repository.LocalLlmRepository
import com.tishukoff.feature.localllm.impl.domain.repository.TelegramBotRepository
import com.tishukoff.feature.localllm.impl.domain.usecase.GetTelegramBotChatsUseCase
import com.tishukoff.feature.localllm.impl.domain.usecase.RunBenchmarkUseCase
import com.tishukoff.feature.localllm.impl.domain.usecase.SendMessageUseCase
import com.tishukoff.feature.localllm.impl.presentation.LocalLlmViewModel
import com.tishukoff.feature.localllm.impl.presentation.telegrambot.TelegramBotViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val localLlmModule = module {
    single { OllamaGenerateClient() }
    single<LocalLlmRepository> { LocalLlmRepositoryImpl(get()) }
    factory { SendMessageUseCase(get()) }
    factory { RunBenchmarkUseCase(get()) }
    viewModel { LocalLlmViewModel(get(), get()) }

    single { TelegramBotApiClient() }
    single<TelegramBotRepository> { TelegramBotRepositoryImpl(get()) }
    factory { GetTelegramBotChatsUseCase(get()) }
    viewModel { TelegramBotViewModel(get()) }
}
