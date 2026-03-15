package com.tishukoff.feature.mcp.api

/**
 * Describes a connected MCP server and its available tools.
 */
data class McpServerInfo(
    val id: String,
    val url: String,
    val name: String,
    val version: String,
    val tools: List<McpTool>,
)
