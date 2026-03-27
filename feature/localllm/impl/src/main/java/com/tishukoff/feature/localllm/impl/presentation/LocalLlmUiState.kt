package com.tishukoff.feature.localllm.impl.presentation

import com.tishukoff.feature.localllm.impl.data.remote.OllamaGenerateClient
import com.tishukoff.feature.localllm.impl.domain.model.BenchmarkComparison
import com.tishukoff.feature.localllm.impl.domain.model.BenchmarkEntry
import com.tishukoff.feature.localllm.impl.domain.model.LlmConfig
import com.tishukoff.feature.localllm.impl.domain.model.LocalLlmMessage
import com.tishukoff.feature.localllm.impl.domain.model.RateLimitConfig
import com.tishukoff.feature.localllm.impl.domain.model.ServerStatus
import com.tishukoff.feature.localllm.impl.domain.model.StabilityTestResult

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
    val benchmarkCurrentQuestion: String = "",
    val benchmarkCurrentConfig: String = "",
    val benchmarkQuestionIndex: Int = 0,
    val benchmarkTotalQuestions: Int = 0,
    val benchmarkLiveEntries: List<BenchmarkEntry> = emptyList(),
    val benchmarkResult: BenchmarkComparison? = null,
    val isServerStatusExpanded: Boolean = false,
    val serverStatus: ServerStatus? = null,
    val isCheckingHealth: Boolean = false,
    val stabilityTestResult: StabilityTestResult? = null,
    val isStabilityTestRunning: Boolean = false,
    val serverIp: String = OllamaGenerateClient.DEFAULT_BASE_URL,
    val rateLimitConfig: RateLimitConfig = RateLimitConfig.DEFAULT,
    val requestsInLastMinute: Int = 0,
)
