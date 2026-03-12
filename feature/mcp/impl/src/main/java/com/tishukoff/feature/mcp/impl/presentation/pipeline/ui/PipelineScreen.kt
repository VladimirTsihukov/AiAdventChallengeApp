package com.tishukoff.feature.mcp.impl.presentation.pipeline.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tishukoff.feature.mcp.api.PipelineStepInfo
import com.tishukoff.feature.mcp.impl.presentation.pipeline.PipelineForm
import com.tishukoff.feature.mcp.impl.presentation.pipeline.PipelineUiState
import com.tishukoff.feature.mcp.impl.presentation.pipeline.PipelineViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PipelineScreen(
    onBack: () -> Unit,
    viewModel: PipelineViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pipeline") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            when (val currentState = state) {
                is PipelineUiState.NotConnected -> {
                    NotConnectedContent()
                }

                is PipelineUiState.Configuring -> {
                    ConfiguringContent(
                        form = currentState.form,
                        onQueryChange = viewModel::updateQuery,
                        onFilenameChange = viewModel::updateFilename,
                        onPerPageChange = viewModel::updatePerPage,
                        onToggleSearch = viewModel::toggleSearch,
                        onToggleSummarize = viewModel::toggleSummarize,
                        onToggleSaveToFile = viewModel::toggleSaveToFile,
                        onToggleTelegram = viewModel::toggleTelegram,
                        onRun = viewModel::runPipeline,
                    )
                }

                is PipelineUiState.Running -> {
                    StepperContent(
                        steps = currentState.steps,
                        overallStatus = currentState.overallStatus,
                        showCancel = true,
                        onCancel = viewModel::cancelPipeline,
                        onReset = null,
                    )
                }

                is PipelineUiState.Completed -> {
                    StepperContent(
                        steps = currentState.steps,
                        overallStatus = currentState.overallStatus,
                        showCancel = false,
                        onCancel = null,
                        onReset = viewModel::resetToConfiguring,
                    )
                }

                is PipelineUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = currentState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = viewModel::resetToConfiguring) {
                            Text("Back to configuration")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NotConnectedContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Not connected to MCP server",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Connect to the server on MCP Tools screen first",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ConfiguringContent(
    form: PipelineForm,
    onQueryChange: (String) -> Unit,
    onFilenameChange: (String) -> Unit,
    onPerPageChange: (String) -> Unit,
    onToggleSearch: (Boolean) -> Unit,
    onToggleSummarize: (Boolean) -> Unit,
    onToggleSaveToFile: (Boolean) -> Unit,
    onToggleTelegram: (Boolean) -> Unit,
    onRun: () -> Unit,
) {
    Text(
        text = "Pipeline Steps",
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(modifier = Modifier.height(12.dp))

    StepToggleRow(
        stepName = "1. Search",
        description = "Search GitHub repositories",
        enabled = form.searchEnabled,
        onToggle = onToggleSearch,
    )

    StepToggleRow(
        stepName = "2. Summarize",
        description = "Create a summary from search results",
        enabled = form.summarizeEnabled,
        onToggle = onToggleSummarize,
    )

    StepToggleRow(
        stepName = "3. Save to File",
        description = "Save results to a file on the server",
        enabled = form.saveToFileEnabled,
        onToggle = onToggleSaveToFile,
    )

    StepToggleRow(
        stepName = "4. Send to Telegram",
        description = "Send summary to Telegram chat",
        enabled = form.telegramEnabled,
        onToggle = onToggleTelegram,
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Parameters",
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = form.query,
        onValueChange = onQueryChange,
        label = { Text("Search query") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = form.perPage,
            onValueChange = onPerPageChange,
            label = { Text("Results count") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )

        if (form.saveToFileEnabled) {
            OutlinedTextField(
                value = form.filename,
                onValueChange = onFilenameChange,
                label = { Text("Filename") },
                singleLine = true,
                modifier = Modifier.weight(2f),
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    val hasEnabledSteps = form.searchEnabled || form.summarizeEnabled || form.saveToFileEnabled || form.telegramEnabled
    Button(
        onClick = onRun,
        enabled = form.query.isNotBlank() && hasEnabledSteps,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Run Pipeline")
    }
}

@Composable
private fun StepToggleRow(
    stepName: String,
    description: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stepName,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
            )
        }
    }
}

@Composable
private fun StepperContent(
    steps: List<PipelineStepInfo>,
    overallStatus: String,
    showCancel: Boolean,
    onCancel: (() -> Unit)?,
    onReset: (() -> Unit)?,
) {
    val statusColor = when (overallStatus) {
        "COMPLETED" -> MaterialTheme.colorScheme.primary
        "ERROR" -> MaterialTheme.colorScheme.error
        "CANCELLED" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.tertiary
    }

    Text(
        text = "Pipeline: $overallStatus",
        style = MaterialTheme.typography.titleMedium,
        color = statusColor,
    )
    Spacer(modifier = Modifier.height(16.dp))

    steps.forEachIndexed { index, step ->
        StepItem(step = step, isLast = index == steps.lastIndex)
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showCancel && onCancel != null) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            ) {
                Text("Cancel")
            }
        }
        if (onReset != null) {
            Button(
                onClick = onReset,
                modifier = Modifier.weight(1f),
            ) {
                Text("New Pipeline")
            }
        }
    }
}

@Composable
private fun StepItem(
    step: PipelineStepInfo,
    isLast: Boolean,
) {
    val stepLabel = when (step.step) {
        "SEARCH" -> "Search"
        "SUMMARIZE" -> "Summarize"
        "SAVE_TO_FILE" -> "Save to File"
        "SEND_TO_TELEGRAM" -> "Send to Telegram"
        else -> step.step
    }

    val indicatorColor by animateColorAsState(
        targetValue = when (step.status) {
            "COMPLETED" -> Color(0xFF4CAF50)
            "IN_PROGRESS" -> Color(0xFFFF9800)
            "ERROR" -> Color(0xFFF44336)
            "SKIPPED" -> Color(0xFF9E9E9E)
            else -> Color(0xFFBDBDBD)
        },
        label = "stepColor",
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp),
        ) {
            if (step.status == "IN_PROGRESS") {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = indicatorColor,
                )
            } else {
                Canvas(modifier = Modifier.size(20.dp)) {
                    drawCircle(color = indicatorColor)
                }
            }
            if (!isLast) {
                val lineColor = indicatorColor
                Canvas(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp),
                ) {
                    drawLine(
                        color = lineColor,
                        start = Offset(size.width / 2, 0f),
                        end = Offset(size.width / 2, size.height),
                        strokeWidth = 2f,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stepLabel,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = step.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = indicatorColor,
                )
            }

            val errorText = step.error
            if (errorText != null) {
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val resultText = step.result
            if (resultText != null && step.status == "COMPLETED") {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Text(
                        text = resultText.take(500),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }

            if (!isLast) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
