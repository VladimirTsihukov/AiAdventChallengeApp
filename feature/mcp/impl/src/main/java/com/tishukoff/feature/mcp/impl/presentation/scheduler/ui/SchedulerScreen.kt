package com.tishukoff.feature.mcp.impl.presentation.scheduler.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tishukoff.feature.mcp.api.ScheduledTaskInfo
import com.tishukoff.feature.mcp.api.TaskResultInfo
import com.tishukoff.feature.mcp.impl.presentation.scheduler.SchedulerUiState
import com.tishukoff.feature.mcp.impl.presentation.scheduler.SchedulerViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulerScreen(
    onBack: () -> Unit,
    viewModel: SchedulerViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val form by viewModel.form.collectAsState()
    val availableTools by viewModel.availableTools.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scheduled Tasks") },
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
        floatingActionButton = {
            if (state is SchedulerUiState.Content) {
                FloatingActionButton(
                    onClick = { viewModel.showCreateDialog() },
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create task")
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            when (val currentState = state) {
                is SchedulerUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Loading tasks...")
                    }
                }

                is SchedulerUiState.NotConnected -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
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

                is SchedulerUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = currentState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = { viewModel.loadTasks() }) {
                            Text("Retry")
                        }
                    }
                }

                is SchedulerUiState.Content -> {
                    if (currentState.tasks.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "No scheduled tasks yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap + to create one",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            item { Spacer(modifier = Modifier.height(4.dp)) }

                            items(currentState.tasks, key = { it.id }) { task ->
                                TaskCard(
                                    task = task,
                                    isExpanded = currentState.expandedTaskId == task.id,
                                    results = if (currentState.expandedTaskId == task.id) {
                                        currentState.taskResults
                                    } else emptyList(),
                                    isLoadingResults = currentState.expandedTaskId == task.id
                                            && currentState.isLoadingResults,
                                    onToggle = { viewModel.toggleTask(task.id, it) },
                                    onDelete = { viewModel.deleteTask(task.id) },
                                    onExpand = { viewModel.expandTask(task.id) },
                                )
                            }

                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }

                    if (currentState.showCreateDialog) {
                        CreateTaskDialog(
                            form = form,
                            availableTools = availableTools,
                            isCreating = currentState.isCreating,
                            onNameChange = { viewModel.updateFormName(it) },
                            onToolNameChange = { viewModel.updateFormToolName(it) },
                            onToolArgumentChange = { k, v -> viewModel.updateFormToolArgument(k, v) },
                            onIntervalChange = { viewModel.updateFormInterval(it) },
                            onDurationChange = { viewModel.updateFormDuration(it) },
                            onCreate = { viewModel.createTask() },
                            onDismiss = { viewModel.hideCreateDialog() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: ScheduledTaskInfo,
    isExpanded: Boolean,
    results: List<TaskResultInfo>,
    isLoadingResults: Boolean,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onExpand: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (!task.isActive) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Tool: ${task.toolName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Switch(
                    checked = task.isActive,
                    onCheckedChange = onToggle,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Every ${formatMinutes(task.intervalMinutes)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "Duration: ${formatMinutes(task.durationMinutes)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            val lastRun = task.lastRunAt
            if (lastRun != null) {
                Text(
                    text = "Last run: ${formatTimestamp(lastRun)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val now = System.currentTimeMillis()
            if (task.isActive && task.expiresAt > now) {
                val remainingMinutes = ((task.expiresAt - now) / 60_000).toInt()
                Text(
                    text = "Expires in: ${formatMinutes(remainingMinutes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else if (!task.isActive) {
                Text(
                    text = "Inactive",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onExpand) {
                    Icon(
                        imageVector = if (isExpanded) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete task",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Text(
                        text = "Execution Results",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (task.toolArguments.isNotEmpty()) {
                        Text(
                            text = "Arguments: ${task.toolArguments}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    when {
                        isLoadingResults -> {
                            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                        }

                        results.isEmpty() -> {
                            Text(
                                text = "No results yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        else -> {
                            results.forEach { result ->
                                ResultItem(result)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultItem(result: TaskResultInfo) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (result.isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = formatTimestamp(result.executedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = result.result.take(500),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                maxLines = 10,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun formatMinutes(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    minutes < 1440 -> "${minutes / 60}h ${minutes % 60}m"
    else -> "${minutes / 1440}d ${(minutes % 1440) / 60}h"
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
