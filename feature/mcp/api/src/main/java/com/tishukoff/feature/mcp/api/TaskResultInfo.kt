package com.tishukoff.feature.mcp.api

/**
 * Represents a single execution result of a scheduled task.
 */
data class TaskResultInfo(
    val taskId: String,
    val executedAt: Long,
    val result: String,
    val isError: Boolean,
)
