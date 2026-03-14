package com.tishukoff.feature.mcp.impl.presentation.pipeline

import androidx.compose.runtime.Immutable
import com.tishukoff.feature.mcp.api.PipelineStepInfo

sealed interface PipelineUiState {

    data object NotConnected : PipelineUiState

    data class Configuring(
        val form: PipelineForm = PipelineForm(),
    ) : PipelineUiState

    data class Running(
        val pipelineId: String,
        val steps: List<PipelineStepInfo>,
        val overallStatus: String,
    ) : PipelineUiState

    data class Completed(
        val pipelineId: String,
        val steps: List<PipelineStepInfo>,
        val overallStatus: String,
    ) : PipelineUiState

    data class Error(val message: String) : PipelineUiState
}

@Immutable
data class PipelineForm(
    val query: String = "",
    val filename: String = "pipeline_result.txt",
    val perPage: String = "5",
    val searchEnabled: Boolean = true,
    val summarizeEnabled: Boolean = true,
    val saveToFileEnabled: Boolean = true,
    val telegramEnabled: Boolean = false,
)
