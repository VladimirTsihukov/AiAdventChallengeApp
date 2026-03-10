package com.tishukoff.feature.mcp.impl.presentation

import com.tishukoff.feature.mcp.api.McpTool

sealed interface McpUiState {
    data object Idle : McpUiState

    data object Connecting : McpUiState

    data class Connected(
        val serverName: String,
        val serverVersion: String,
        val tools: List<McpTool>,
    ) : McpUiState

    data class Error(val message: String) : McpUiState
}
