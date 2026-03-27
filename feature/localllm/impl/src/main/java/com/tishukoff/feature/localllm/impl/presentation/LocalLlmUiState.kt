package com.tishukoff.feature.localllm.impl.presentation

import com.tishukoff.feature.localllm.impl.domain.model.BenchmarkComparison
import com.tishukoff.feature.localllm.impl.domain.model.LlmConfig
import com.tishukoff.feature.localllm.impl.domain.model.LocalLlmMessage

/**
 * UI-состояние экрана чата с локальной LLM.
 */
data class LocalLlmUiState(
    val input: String = "",
    val messages: List<LocalLlmMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val modelName: String = LlmConfig.DEFAULT_MODEL,
    val config: LlmConfig = LlmConfig.DEFAULT,
    val isSettingsExpanded: Boolean = false,
    val isBenchmarkRunning: Boolean = false,
    val benchmarkProgress: String = "",
    val benchmarkResult: BenchmarkComparison? = null,
)
