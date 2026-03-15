package com.tishukoff.feature.mcp.impl.presentation.orchestration

import com.tishukoff.feature.agent.api.ChatMessage
import com.tishukoff.feature.mcp.api.McpServerInfo

data class OrchestrationUiState(
    val servers: List<McpServerInfo> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isLoading: Boolean = false,
    val isConnecting: Boolean = false,
    val connectionError: String? = null,
    val serverUrls: List<ServerEntry> = DEFAULT_SERVERS,
    val hostAddress: String = "",
) {
    companion object {
        val DEFAULT_SERVERS = listOf(
            ServerEntry("GitHub MCP", port = 3000),
            ServerEntry("Weather MCP", port = 3001),
        )
    }
}

data class ServerEntry(
    val label: String,
    val port: Int,
    val isConnected: Boolean = false,
) {
    fun buildUrl(host: String): String = "http://$host:$port/"
}
