package com.tishukoff.feature.agent.impl

import com.tishukoff.feature.agent.api.Agent
import org.koin.core.qualifier.named
import org.koin.dsl.module

val agentModule = module {
    single { SettingsRepository(get()) }
    single<Agent> {
        ClaudeAgent(
            apiKey = get(named("anthropicApiKey")),
            settingsRepository = get(),
            chatHistoryStorage = get()
        )
    }
}
