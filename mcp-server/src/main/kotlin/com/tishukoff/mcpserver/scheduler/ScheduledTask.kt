package com.tishukoff.mcpserver.scheduler

import kotlinx.serialization.Serializable

@Serializable
data class ScheduledTask(
    val id: String,
    val name: String,
    val toolName: String,
    val toolArguments: Map<String, String> = emptyMap(),
    val intervalMinutes: Int,
    val durationMinutes: Int,
    val createdAt: Long,
    val expiresAt: Long,
    val lastRunAt: Long? = null,
    val isActive: Boolean = true,
)

@Serializable
data class TaskExecutionResult(
    val taskId: String,
    val executedAt: Long,
    val result: String,
    val isError: Boolean = false,
)

@Serializable
data class TaskStoreData(
    val tasks: List<ScheduledTask> = emptyList(),
    val results: List<TaskExecutionResult> = emptyList(),
)
