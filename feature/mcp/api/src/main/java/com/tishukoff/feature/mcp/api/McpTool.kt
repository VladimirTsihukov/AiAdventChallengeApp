package com.tishukoff.feature.mcp.api

import androidx.compose.runtime.Immutable

/**
 * Represents a tool available on an MCP server.
 */
@Immutable
data class McpTool(
    val name: String,
    val description: String,
    val inputSchemaJson: String,
)
