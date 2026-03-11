package com.tishukoff.feature.mcp.impl.presentation.scheduler

import androidx.compose.runtime.Immutable
import com.tishukoff.feature.mcp.api.ScheduledTaskInfo
import com.tishukoff.feature.mcp.api.TaskResultInfo

sealed interface SchedulerUiState {

    data object NotConnected : SchedulerUiState

    data object Loading : SchedulerUiState

    data class Content(
        val tasks: List<ScheduledTaskInfo> = emptyList(),
        val showCreateDialog: Boolean = false,
        val expandedTaskId: String? = null,
        val taskResults: List<TaskResultInfo> = emptyList(),
        val isLoadingResults: Boolean = false,
        val isCreating: Boolean = false,
        val error: String? = null,
    ) : SchedulerUiState

    data class Error(val message: String) : SchedulerUiState
}

@Immutable
data class CreateTaskForm(
    val name: String = "",
    val toolName: String = "",
    val toolArguments: Map<String, String> = emptyMap(),
    val intervalMinutes: String = "60",
    val durationMinutes: String = "1440",
)
