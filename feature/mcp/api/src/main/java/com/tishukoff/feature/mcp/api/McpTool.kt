package com.tishukoff.feature.mcp.api

/**
 * Represents a tool available on an MCP server.
 */
data class McpTool(
    val name: String,
    val description: String,
    val inputSchemaJson: String,
)
