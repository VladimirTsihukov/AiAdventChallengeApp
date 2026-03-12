package com.tishukoff.feature.mcp.api

/**
 * Represents a scheduled task returned from the MCP server.
 */
data class ScheduledTaskInfo(
    val id: String,
    val name: String,
    val toolName: String,
    val toolArguments: Map<String, String>,
    val intervalMinutes: Int,
    val durationMinutes: Int,
    val createdAt: Long,
    val expiresAt: Long,
    val lastRunAt: Long?,
    val isActive: Boolean,
)
