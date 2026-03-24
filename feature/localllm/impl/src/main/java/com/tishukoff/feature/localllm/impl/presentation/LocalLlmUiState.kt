package com.tishukoff.feature.localllm.impl.presentation

import com.tishukoff.feature.localllm.impl.domain.model.LocalLlmMessage

/**
 * UI-состояние экрана чата с локальной LLM.
 */
data class LocalLlmUiState(
    val input: String = "",
    val messages: List<LocalLlmMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val modelName: String = "phi3:mini",
)
