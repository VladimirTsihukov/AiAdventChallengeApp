package com.tishukoff.aiadventchallengeapp.di

import android.content.Context
import com.tishukoff.aiadventchallengeapp.BuildConfig
import com.tishukoff.aiadventchallengeapp.presentation.ChatViewModel
import com.tishukoff.core.database.impl.databaseModule
import com.tishukoff.feature.agent.impl.agentModule
import com.tishukoff.feature.setting.impl.di.settingModule
import org.koin.core.qualifier.named
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    includes(databaseModule, agentModule, settingModule)
    single {
        get<Context>().getSharedPreferences("llm_settings", Context.MODE_PRIVATE)
    }
    single(named("anthropicApiKey")) { BuildConfig.ANTHROPIC_API_KEY }
    viewModel { ChatViewModel(get()) }
}
