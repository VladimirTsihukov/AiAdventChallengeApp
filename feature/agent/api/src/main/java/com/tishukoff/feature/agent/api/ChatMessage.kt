package com.tishukoff.feature.agent.api

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val metadataText: String? = null,
    val toolCalls: List<ToolCallEntry> = emptyList(),
)

/**
 * Represents a tool call made by the agent.
 */
data class ToolCallEntry(
    val toolName: String,
    val serverName: String,
    val arguments: Map<String, String>,
    val result: String,
)
