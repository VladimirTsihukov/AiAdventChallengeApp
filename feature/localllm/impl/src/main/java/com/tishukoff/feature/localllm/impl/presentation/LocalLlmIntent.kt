package com.tishukoff.feature.localllm.impl.presentation

import com.tishukoff.feature.localllm.impl.domain.model.LlmConfig

/**
 * Интенты для экрана чата с локальной LLM.
 */
sealed interface LocalLlmIntent {
    data class UpdateInput(val text: String) : LocalLlmIntent
    data object SendMessage : LocalLlmIntent
    data object ClearHistory : LocalLlmIntent
    data class DismissError(val message: String? = null) : LocalLlmIntent

    data object ToggleSettings : LocalLlmIntent
    data class UpdateTemperature(val value: Float) : LocalLlmIntent
    data class UpdateMaxTokens(val value: Int) : LocalLlmIntent
    data class UpdateContextWindow(val value: Int) : LocalLlmIntent
    data class UpdateRepeatPenalty(val value: Float) : LocalLlmIntent
    data class UpdateTopP(val value: Float) : LocalLlmIntent
    data class UpdateTopK(val value: Int) : LocalLlmIntent
    data class UpdateSystemPrompt(val value: String) : LocalLlmIntent
    data class ApplyPreset(val config: LlmConfig) : LocalLlmIntent

    data object RunBenchmark : LocalLlmIntent
    data object DismissBenchmark : LocalLlmIntent
}
