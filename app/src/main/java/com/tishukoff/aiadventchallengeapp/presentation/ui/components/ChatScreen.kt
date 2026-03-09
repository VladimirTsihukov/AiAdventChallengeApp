package com.tishukoff.aiadventchallengeapp.presentation.ui.components

import android.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tishukoff.aiadventchallengeapp.presentation.ui.models.ChatIntent
import com.tishukoff.aiadventchallengeapp.presentation.ui.models.ChatUiState
import com.tishukoff.core.designsystem.AiAdventChallengeAppTheme
import com.tishukoff.feature.agent.api.ChatMessage
import com.tishukoff.feature.agent.api.ContextStrategyType
import com.tishukoff.feature.agent.api.TokenStats
import com.tishukoff.feature.taskstate.api.PauseReason
import com.tishukoff.feature.taskstate.api.TaskStage
import com.tishukoff.feature.taskstate.api.TaskState

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    state: State<ChatUiState>,
    onIntent: (ChatIntent) -> Unit,
) {
    val listState = rememberLazyListState()
    val stateValue = state.value
    // Auto-scroll to the latest message
    LaunchedEffect(stateValue.messages.size) {
        if (stateValue.messages.isNotEmpty()) {
            listState.animateScrollToItem(stateValue.messages.lastIndex)
        }
    }

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        val taskState = stateValue.taskState
        if (taskState.stage != TaskStage.IDLE) {
            TaskStagePanel(
                taskState = taskState,
                isLoading = stateValue.isLoading,
                transitionError = stateValue.transitionError,
                onApprovePlan = { clarification -> onIntent(ChatIntent.ApprovePlan(clarification)) },
                onResume = { clarification -> onIntent(ChatIntent.ResumeTask(clarification)) },
                onPause = { onIntent(ChatIntent.PauseTask) },
                onReset = { onIntent(ChatIntent.ResetTask) },
                onDismissError = { onIntent(ChatIntent.DismissTransitionError) },
            )
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (stateValue.messages.isEmpty() && !stateValue.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ask Claude anything...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(stateValue.messages) { message ->
                MessageBubble(message = message)
            }

            if (stateValue.isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(BubbleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (stateValue.contextStrategyType == ContextStrategyType.BRANCHING) {
            BranchPanel(
                branches = stateValue.branches,
                currentBranchId = stateValue.currentBranchId,
                onCreateCheckpoint = { name ->
                    onIntent(ChatIntent.CreateCheckpoint(name))
                },
                onCreateBranch = { checkpointId, name ->
                    onIntent(ChatIntent.CreateBranch(checkpointId, name))
                },
                onSwitchBranch = { branchId ->
                    onIntent(ChatIntent.SwitchBranch(branchId))
                },
            )
        }

        TokenStatsPanel(
            stats = stateValue.tokenStats,
            compressionStats = stateValue.compressionStats,
            contextStrategyType = stateValue.contextStrategyType,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        // Input field at the bottom
        OutlinedTextField(
            value = stateValue.input,
            onValueChange = { onIntent(ChatIntent.UpdateInput(it)) },
            placeholder = { Text("Message") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            trailingIcon = {
                if (stateValue.isLoading.not()) {
                    IconButton(
                        onClick = { onIntent(ChatIntent.SendMessage) },
                        enabled = stateValue.input.isNotBlank(),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_menu_send),
                            contentDescription = "Send"
                        )
                    }
                }
            }
        )

        if (stateValue.taskState.stage == TaskStage.IDLE) {
            Button(
                onClick = {
                    val text = stateValue.input.trim()
                    if (text.isNotBlank()) {
                        onIntent(ChatIntent.UpdateInput(""))
                        onIntent(ChatIntent.StartTask(text))
                    }
                },
                enabled = stateValue.input.isNotBlank() && !stateValue.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
            ) {
                Text("Task", color = Color.Black)
            }
        }
    }
}

@Composable
private fun TaskStagePanel(
    taskState: TaskState,
    isLoading: Boolean,
    transitionError: String?,
    onApprovePlan: (String?) -> Unit,
    onResume: (String?) -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onDismissError: () -> Unit,
) {
    val stages = listOf(
        TaskStage.PLANNING to "Planning",
        TaskStage.PLAN_APPROVED to "Approved",
        TaskStage.EXECUTION to "Execution",
        TaskStage.VALIDATION to "Validation",
        TaskStage.DONE to "Done",
    )

    val currentIndex = stages.indexOfFirst { it.first == taskState.stage }
    val progress = if (stages.isNotEmpty()) {
        (currentIndex + 1).toFloat() / stages.size
    } else {
        0f
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Task: ${taskState.stage.name}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = onReset) {
                Text("Reset")
            }
        }

        val isDone = taskState.stage == TaskStage.DONE
        val progressColor = if (isDone) {
            Color(0xFF4CAF50)
        } else {
            MaterialTheme.colorScheme.primary
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = progressColor,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            stages.forEach { (stage, label) ->
                val isActive = stage == taskState.stage
                val isPast = stages.indexOfFirst { it.first == stage } < currentIndex
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isActive -> MaterialTheme.colorScheme.primary
                        isPast -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    },
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }

        if (transitionError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = transitionError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismissError) {
                    Text("OK", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        if (taskState.isPaused && !isLoading) {
            Spacer(modifier = Modifier.height(8.dp))

            var clarification by remember { mutableStateOf("") }

            val showClarificationField = taskState.pauseReason == PauseReason.STAGE_COMPLETE &&
                taskState.stage != TaskStage.DONE

            if (showClarificationField) {
                OutlinedTextField(
                    value = clarification,
                    onValueChange = { clarification = it },
                    placeholder = { Text("Уточнение (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                if (taskState.stage == TaskStage.PLANNING) {
                    Button(
                        onClick = {
                            val text = clarification.trim().ifEmpty { null }
                            onApprovePlan(text)
                            clarification = ""
                        },
                    ) {
                        Text("Утвердить план")
                    }
                } else if (taskState.stage != TaskStage.DONE) {
                    Button(
                        onClick = {
                            val text = clarification.trim().ifEmpty { null }
                            onResume(text)
                            clarification = ""
                        },
                    ) {
                        Text("Продолжить")
                    }
                }
            }
        }

        if (!taskState.isPaused && taskState.stage !in listOf(TaskStage.DONE, TaskStage.PLAN_APPROVED)) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                OutlinedButton(onClick = onPause) {
                    Text("Пауза")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatScreenEmptyPreview() {
    AiAdventChallengeAppTheme(dynamicColor = false) {
        val state = remember {
            mutableStateOf(ChatUiState())
        }
        ChatScreen(state = state, onIntent = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatScreenWithMessagesPreview() {
    AiAdventChallengeAppTheme(dynamicColor = false) {
        val state = remember {
            mutableStateOf(
                ChatUiState(
                    messages = listOf(
                        ChatMessage(text = "Привет! Как работает Kotlin?", isUser = true),
                        ChatMessage(
                            text = "Kotlin — это современный язык программирования, разработанный JetBrains.",
                            isUser = false,
                            metadataText = "claude-sonnet-4-5 | in: 12 out: 24 | 1.2s | \$0.0004",
                        ),
                    ),
                    input = "Расскажи подробнее",
                    tokenStats = TokenStats(
                        totalInputTokens = 1250,
                        totalOutputTokens = 3400,
                        totalCostUsd = 0.0548,
                        requestCount = 5,
                        contextWindow = 200_000,
                        lastRequestInputTokens = 800,
                        lastRequestOutputTokens = 920,
                        lastRequestCostUsd = 0.0162,
                    )
                )
            )
        }
        ChatScreen(state = state, onIntent = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatScreenLoadingPreview() {
    AiAdventChallengeAppTheme(dynamicColor = false) {
        val state = remember {
            mutableStateOf(
                ChatUiState(
                    messages = listOf(
                        ChatMessage(text = "Что такое корутины?", isUser = true),
                    ),
                    isLoading = true,
                )
            )
        }
        ChatScreen(state = state, onIntent = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun TaskStagePanelPreview() {
    AiAdventChallengeAppTheme(dynamicColor = false) {
        TaskStagePanel(
            taskState = TaskState(
                stage = TaskStage.PLANNING,
                isPaused = true,
                pauseReason = PauseReason.STAGE_COMPLETE,
                taskDescription = "Написать REST API",
                planningResult = "1. Создать модели\n2. Настроить роутинг",
            ),
            isLoading = false,
            transitionError = null,
            onApprovePlan = {},
            onResume = {},
            onPause = {},
            onReset = {},
            onDismissError = {},
        )
    }
}
