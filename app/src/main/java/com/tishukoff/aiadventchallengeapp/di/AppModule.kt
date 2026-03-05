package com.tishukoff.aiadventchallengeapp.di

import android.content.Context
import com.tishukoff.aiadventchallengeapp.BuildConfig
import com.tishukoff.aiadventchallengeapp.presentation.ChatViewModel
import com.tishukoff.core.database.impl.databaseModule
import com.tishukoff.feature.agent.impl.agentModule
import com.tishukoff.feature.memory.impl.di.memoryModule
import com.tishukoff.feature.profile.impl.di.profileModule
import com.tishukoff.feature.setting.impl.di.settingModule
import com.tishukoff.feature.taskstate.impl.di.taskStateModule
import org.koin.core.qualifier.named
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    includes(databaseModule, memoryModule, agentModule, settingModule, profileModule, taskStateModule)
    single {
        get<Context>().getSharedPreferences("llm_settings", Context.MODE_PRIVATE)
    }
    single(named("profilePrefs")) {
        get<Context>().getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)
    }
    single(named("anthropicApiKey")) { BuildConfig.ANTHROPIC_API_KEY }
    viewModel { ChatViewModel(get(), get(), get()) }
}
