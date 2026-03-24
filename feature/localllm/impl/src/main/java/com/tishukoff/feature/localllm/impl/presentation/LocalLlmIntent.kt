package com.tishukoff.feature.localllm.impl.presentation

/**
 * Интенты для экрана чата с локальной LLM.
 */
sealed interface LocalLlmIntent {
    data class UpdateInput(val text: String) : LocalLlmIntent
    data object SendMessage : LocalLlmIntent
    data object ClearHistory : LocalLlmIntent
    data class DismissError(val message: String? = null) : LocalLlmIntent
}
