package com.tishukoff.feature.mcp.impl.presentation

import com.tishukoff.feature.mcp.api.McpTool

sealed interface McpUiState {
    data object Idle : McpUiState

    data object Connecting : McpUiState

    data class Connected(
        val serverName: String,
        val serverVersion: String,
        val tools: List<McpTool>,
        val selectedTool: McpTool? = null,
        val toolArguments: Map<String, String> = emptyMap(),
        val toolResult: String? = null,
        val isCallingTool: Boolean = false,
    ) : McpUiState

    data class Error(val message: String) : McpUiState
}
