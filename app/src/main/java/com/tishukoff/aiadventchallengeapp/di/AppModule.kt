package com.tishukoff.aiadventchallengeapp.di

import android.content.Context
import com.tishukoff.aiadventchallengeapp.presentation.ChatViewModel
import com.tishukoff.aiadventchallengeapp.presentation.CompareViewModel
import com.tishukoff.aiadventchallengeapp.data.ClaudeRepository
import com.tishukoff.aiadventchallengeapp.data.SettingsRepository
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        get<Context>().getSharedPreferences("llm_settings", Context.MODE_PRIVATE)
    }
    single { SettingsRepository(get()) }
    single { ClaudeRepository() }
    viewModel { ChatViewModel(get(), get()) }
    viewModel { CompareViewModel(get(), get()) }
}
