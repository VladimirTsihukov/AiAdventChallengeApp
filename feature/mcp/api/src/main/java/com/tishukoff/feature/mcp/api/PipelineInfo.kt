package com.tishukoff.feature.mcp.api

/**
 * Represents a pipeline and its step statuses returned from the MCP server.
 */
data class PipelineStepInfo(
    val step: String,
    val status: String,
    val result: String? = null,
    val error: String? = null,
)

data class PipelineStatusInfo(
    val id: String,
    val status: String,
    val steps: List<PipelineStepInfo>,
    val createdAt: Long,
    val completedAt: Long? = null,
)
