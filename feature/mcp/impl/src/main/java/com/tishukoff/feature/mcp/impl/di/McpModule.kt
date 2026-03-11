package com.tishukoff.feature.mcp.impl.di

import com.tishukoff.feature.mcp.impl.data.McpClientWrapper
import com.tishukoff.feature.mcp.impl.data.McpPreferences
import com.tishukoff.feature.mcp.impl.presentation.McpViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val mcpModule = module {
    single { McpClientWrapper() }
    single { McpPreferences(get()) }
    viewModel { McpViewModel(get(), get()) }
}
