package com.tishukoff.feature.mcp.api

/**
 * Information about a single tool call made by the agent during a conversation turn.
 */
data class ToolCallInfo(
    val toolName: String,
    val serverName: String,
    val arguments: Map<String, String>,
    val result: String,
)
