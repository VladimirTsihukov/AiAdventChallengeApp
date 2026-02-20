package com.tishukoff.aiadventchallengeapp.presentation.ui.models

import com.tishukoff.aiadventchallengeapp.data.ClaudeModel

data class CompareUiState(
    val prompt: String = "",
    val isLoading: Boolean = false,
    val results: Map<ClaudeModel, CompareResult> = emptyMap()
)

sealed interface CompareResult {
    data object Loading : CompareResult
    data class Success(val text: String, val metadataText: String) : CompareResult
    data class Error(val message: String) : CompareResult
}
