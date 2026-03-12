package com.tishukoff.feature.mcp.impl.di

import com.tishukoff.feature.mcp.impl.data.McpClientWrapper
import com.tishukoff.feature.mcp.impl.data.McpPreferences
import com.tishukoff.feature.mcp.impl.data.PipelineRepository
import com.tishukoff.feature.mcp.impl.data.SchedulerRepository
import com.tishukoff.feature.mcp.impl.presentation.McpViewModel
import com.tishukoff.feature.mcp.impl.presentation.pipeline.PipelineViewModel
import com.tishukoff.feature.mcp.impl.presentation.scheduler.SchedulerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val mcpModule = module {
    single { McpClientWrapper() }
    single { McpPreferences(get()) }
    single { SchedulerRepository(get()) }
    single { PipelineRepository(get()) }
    viewModel { McpViewModel(get(), get()) }
    viewModel { SchedulerViewModel(get(), get()) }
    viewModel { PipelineViewModel(get()) }
}
