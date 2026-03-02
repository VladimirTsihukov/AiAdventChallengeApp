package com.tishukoff.feature.memory.impl.di

import com.tishukoff.feature.memory.api.MemoryManager
import com.tishukoff.feature.memory.impl.MemoryManagerImpl
import com.tishukoff.feature.memory.impl.presentation.MemoryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val memoryModule = module {
    single<MemoryManager> {
        MemoryManagerImpl(
            apiKey = get(named("anthropicApiKey")),
            workingMemoryStorage = get(),
            longTermMemoryStorage = get(),
        )
    }
    viewModel { MemoryViewModel(get(), get(), get()) }
}
