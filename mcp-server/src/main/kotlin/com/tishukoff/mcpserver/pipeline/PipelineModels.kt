package com.tishukoff.mcpserver.pipeline

import kotlinx.serialization.Serializable

enum class PipelineStepType {
    SEARCH,
    SUMMARIZE,
    SAVE_TO_FILE,
    SEND_TO_TELEGRAM,
}

enum class PipelineStepStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    SKIPPED,
    ERROR,
}

enum class PipelineStatus {
    RUNNING,
    COMPLETED,
    CANCELLED,
    ERROR,
}

@Serializable
data class PipelineStepResult(
    val step: String,
    val status: String,
    val result: String? = null,
    val error: String? = null,
)

@Serializable
data class PipelineConfig(
    val searchEnabled: Boolean = true,
    val summarizeEnabled: Boolean = true,
    val saveToFileEnabled: Boolean = true,
    val sendToTelegramEnabled: Boolean = false,
    val query: String = "",
    val filename: String = "pipeline_result.txt",
    val perPage: Int = 5,
)

@Serializable
data class PipelineInfo(
    val id: String,
    val config: PipelineConfig,
    val status: String,
    val steps: List<PipelineStepResult>,
    val createdAt: Long,
    val completedAt: Long? = null,
)
