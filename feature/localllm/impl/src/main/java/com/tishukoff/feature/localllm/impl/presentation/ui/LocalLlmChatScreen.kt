package com.tishukoff.feature.localllm.impl.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tishukoff.feature.localllm.impl.data.remote.OllamaGenerateClient
import com.tishukoff.feature.localllm.impl.domain.model.BenchmarkComparison
import com.tishukoff.feature.localllm.impl.domain.model.BenchmarkEntry
import com.tishukoff.feature.localllm.impl.domain.model.LlmConfig
import com.tishukoff.feature.localllm.impl.domain.model.LocalLlmMessage
import com.tishukoff.feature.localllm.impl.domain.model.ServerStatus
import com.tishukoff.feature.localllm.impl.domain.model.StabilityTestResult
import com.tishukoff.feature.localllm.impl.presentation.LocalLlmIntent
import com.tishukoff.feature.localllm.impl.presentation.LocalLlmUiState
import com.tishukoff.feature.localllm.impl.presentation.LocalLlmViewModel
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalLlmChatScreen(
    onBack: () -> Unit,
    viewModel: LocalLlmViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Local LLM")
                        Text(
                            text = state.config.model,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.handleIntent(LocalLlmIntent.ToggleSettings) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                        )
                    }
                    if (state.messages.isNotEmpty() && !state.isLoading) {
                        IconButton(
                            onClick = { viewModel.handleIntent(LocalLlmIntent.ClearHistory) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear history",
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            AnimatedVisibility(visible = state.isSettingsExpanded) {
                SettingsPanel(state = state, onIntent = viewModel::handleIntent)
            }

            ServerStatusPanel(state = state, onIntent = viewModel::handleIntent)

            if (state.isBenchmarkRunning || state.benchmarkLiveEntries.isNotEmpty()) {
                BenchmarkLivePanel(state = state)
            }

            state.benchmarkResult?.let { result ->
                BenchmarkResultPanel(
                    result = result,
                    onDismiss = { viewModel.handleIntent(LocalLlmIntent.DismissBenchmark) },
                )
            }

            val listState = rememberLazyListState()

            LaunchedEffect(state.messages.size) {
                if (state.messages.isNotEmpty()) {
                    listState.animateScrollToItem(state.messages.size - 1)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                items(
                    items = state.messages,
                    key = { "${it.role}_${state.messages.indexOf(it)}" },
                ) { message ->
                    MessageBubble(message = message)
                }

                if (state.isLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Generating...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            state.error?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(8.dp),
                    action = {
                        TextButton(
                            onClick = { viewModel.handleIntent(LocalLlmIntent.DismissError()) }
                        ) {
                            Text("OK")
                        }
                    },
                ) {
                    Text(error)
                }
            }

            BottomInputBar(
                input = state.input,
                isLoading = state.isLoading || state.isBenchmarkRunning,
                onInputChange = { viewModel.handleIntent(LocalLlmIntent.UpdateInput(it)) },
                onSend = { viewModel.handleIntent(LocalLlmIntent.SendMessage) },
            )
        }
    }
}

@Composable
private fun SettingsPanel(
    state: LocalLlmUiState,
    onIntent: (LocalLlmIntent) -> Unit,
) {
    val config = state.config

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Настройки LLM",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = config == LlmConfig.DEFAULT,
                    onClick = { onIntent(LocalLlmIntent.ApplyPreset(LlmConfig.DEFAULT)) },
                    label = { Text("По умолчанию") },
                )
                FilterChip(
                    selected = config == LlmConfig.OPTIMIZED,
                    onClick = { onIntent(LocalLlmIntent.ApplyPreset(LlmConfig.OPTIMIZED)) },
                    label = { Text("Оптимизированный") },
                )
            }

            HorizontalDivider()

            SliderSetting(
                label = "Temperature",
                value = config.temperature,
                valueRange = 0f..1.5f,
                valueLabel = String.format("%.2f", config.temperature),
                onValueChange = { onIntent(LocalLlmIntent.UpdateTemperature(it)) },
            )

            SliderSetting(
                label = "Max Tokens (num_predict)",
                value = config.maxTokens.toFloat(),
                valueRange = 128f..4096f,
                valueLabel = "${config.maxTokens}",
                onValueChange = { onIntent(LocalLlmIntent.UpdateMaxTokens(it.roundToInt())) },
            )

            SliderSetting(
                label = "Context Window (num_ctx)",
                value = config.contextWindow.toFloat(),
                valueRange = 512f..8192f,
                valueLabel = "${config.contextWindow}",
                onValueChange = { onIntent(LocalLlmIntent.UpdateContextWindow(it.roundToInt())) },
            )

            SliderSetting(
                label = "Repeat Penalty",
                value = config.repeatPenalty,
                valueRange = 1.0f..2.0f,
                valueLabel = String.format("%.2f", config.repeatPenalty),
                onValueChange = { onIntent(LocalLlmIntent.UpdateRepeatPenalty(it)) },
            )

            SliderSetting(
                label = "Top P",
                value = config.topP,
                valueRange = 0.1f..1.0f,
                valueLabel = String.format("%.2f", config.topP),
                onValueChange = { onIntent(LocalLlmIntent.UpdateTopP(it)) },
            )

            SliderSetting(
                label = "Top K",
                value = config.topK.toFloat(),
                valueRange = 1f..100f,
                valueLabel = "${config.topK}",
                onValueChange = { onIntent(LocalLlmIntent.UpdateTopK(it.roundToInt())) },
            )

            Text(
                text = "System Prompt",
                style = MaterialTheme.typography.labelMedium,
            )
            OutlinedTextField(
                value = config.systemPrompt,
                onValueChange = { onIntent(LocalLlmIntent.UpdateSystemPrompt(it)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                placeholder = { Text("Системный промпт (опционально)") },
            )

            HorizontalDivider()

            Button(
                onClick = { onIntent(LocalLlmIntent.RunBenchmark) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isBenchmarkRunning && !state.isLoading,
            ) {
                Text("Запустить бенчмарк (сравнение)")
            }
        }
    }
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
        )
    }
}

@Composable
private fun BenchmarkLivePanel(state: LocalLlmUiState) {
    val liveListState = rememberLazyListState()

    LaunchedEffect(state.benchmarkLiveEntries.size, state.benchmarkCurrentQuestion) {
        val totalItems = state.benchmarkLiveEntries.size +
            (if (state.benchmarkCurrentQuestion.isNotEmpty()) 1 else 0)
        if (totalItems > 0) {
            liveListState.animateScrollToItem(totalItems - 1)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Бенчмарк",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (state.isBenchmarkRunning && state.benchmarkTotalQuestions > 0) {
                    Text(
                        text = "[${state.benchmarkCurrentConfig}] " +
                            "${state.benchmarkQuestionIndex}/${state.benchmarkTotalQuestions}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            if (state.isBenchmarkRunning) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                )
            }

            LazyColumn(
                state = liveListState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(
                    items = state.benchmarkLiveEntries,
                    key = { "${it.configLabel}_${it.question}" },
                ) { entry ->
                    BenchmarkLiveEntry(entry = entry)
                }

                if (state.benchmarkCurrentQuestion.isNotEmpty() && state.isBenchmarkRunning) {
                    item(key = "current_question") {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "[${state.benchmarkCurrentConfig}]",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = state.benchmarkCurrentQuestion,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BenchmarkLiveEntry(entry: BenchmarkEntry) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "[${entry.configLabel}]",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (entry.configLabel == "По умолчанию") {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
                Text(
                    text = "${entry.durationMs} мс",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = entry.question,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = entry.answer.take(150) + if (entry.answer.length > 150) "..." else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BenchmarkResultPanel(
    result: BenchmarkComparison,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Результаты бенчмарка",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            HorizontalDivider()

            ComparisonRow(
                label = "Среднее время ответа",
                defaultValue = "${result.defaultAvgMs} мс",
                optimizedValue = "${result.optimizedAvgMs} мс",
            )
            ComparisonRow(
                label = "Средняя длина ответа",
                defaultValue = "${result.defaultAvgLength} символов",
                optimizedValue = "${result.optimizedAvgLength} символов",
            )

            val speedup = result.speedupPercent
            val speedLabel = if (speedup > 0) "быстрее на $speedup%" else "медленнее на ${-speedup}%"
            Text(
                text = "Оптимизированный: $speedLabel",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (speedup > 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )

            HorizontalDivider()

            Text(
                text = "Детали по вопросам:",
                style = MaterialTheme.typography.labelLarge,
            )

            result.defaultResults.zip(result.optimizedResults).forEach { (default, optimized) ->
                QuestionComparisonCard(default = default, optimized = optimized)
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Закрыть результаты")
            }
        }
    }
}

@Composable
private fun ComparisonRow(
    label: String,
    defaultValue: String,
    optimizedValue: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Default: $defaultValue",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Optimized: $optimizedValue",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun QuestionComparisonCard(
    default: BenchmarkEntry,
    optimized: BenchmarkEntry,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = default.question,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Default (${default.durationMs} мс):",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = default.answer.take(200) + if (default.answer.length > 200) "..." else "",
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Optimized (${optimized.durationMs} мс):",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = optimized.answer.take(200) + if (optimized.answer.length > 200) "..." else "",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun MessageBubble(message: LocalLlmMessage) {
    val isUser = message.role == LocalLlmMessage.Role.USER
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Card(
            modifier = Modifier.widthIn(max = screenWidth * 0.85f),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp,
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
            ),
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
            )
        }
    }
}

@Composable
private fun BottomInputBar(
    input: String,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Message...") },
            maxLines = 4,
            enabled = !isLoading,
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onSend,
            enabled = input.isNotBlank() && !isLoading,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
            )
        }
    }
}

@Composable
private fun ServerStatusPanel(
    state: LocalLlmUiState,
    onIntent: (LocalLlmIntent) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onIntent(LocalLlmIntent.ToggleServerStatus) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ServerStatusIndicator(status = state.serverStatus)
                    Text(
                        text = "Server Status",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    state.serverStatus?.let { status ->
                        if (status.isOnline) {
                            Text(
                                text = "${status.responseTimeMs} ms",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Icon(
                    imageVector = if (state.isServerStatusExpanded) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = "Toggle",
                )
            }

            AnimatedVisibility(visible = state.isServerStatusExpanded) {
                ServerStatusDetails(state = state, onIntent = onIntent)
            }
        }
    }
}

@Composable
private fun ServerStatusIndicator(status: ServerStatus?) {
    val color by animateColorAsState(
        targetValue = when {
            status == null -> MaterialTheme.colorScheme.outline
            status.isOnline -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.error
        },
        label = "status_color",
    )

    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun ServerStatusDetails(
    state: LocalLlmUiState,
    onIntent: (LocalLlmIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalDivider()

        Text(
            text = "Server URL",
            style = MaterialTheme.typography.labelMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.serverIp,
                onValueChange = { onIntent(LocalLlmIntent.UpdateServerIp(it)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text(OllamaGenerateClient.DEFAULT_BASE_URL) },
                textStyle = MaterialTheme.typography.bodySmall,
            )
            Button(
                onClick = { onIntent(LocalLlmIntent.ApplyServerIp) },
            ) {
                Text("OK")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { onIntent(LocalLlmIntent.CheckHealth) },
                enabled = !state.isCheckingHealth,
                modifier = Modifier.weight(1f),
            ) {
                if (state.isCheckingHealth) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Health Check")
            }
            Button(
                onClick = { onIntent(LocalLlmIntent.RunStabilityTest) },
                enabled = !state.isStabilityTestRunning && !state.isCheckingHealth,
                modifier = Modifier.weight(1f),
            ) {
                if (state.isStabilityTestRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Stability Test")
            }
        }

        state.serverStatus?.let { status ->
            ServerInfoCard(status = status)
        }

        RateLimitInfoCard(state = state)

        state.stabilityTestResult?.let { result ->
            StabilityTestResultCard(result = result)
        }
    }
}

@Composable
private fun ServerInfoCard(status: ServerStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Информация о сервере",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Статус", style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (status.isOnline) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.Warning
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (status.isOnline) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (status.isOnline) "Online" else "Offline",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (status.isOnline) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
            }

            if (status.version.isNotBlank()) {
                InfoRow(label = "Версия Ollama", value = status.version)
            }

            InfoRow(label = "Время отклика", value = "${status.responseTimeMs} ms")

            if (status.models.isNotEmpty()) {
                Text(
                    text = "Доступные модели:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                status.models.forEach { model ->
                    val sizeStr = when {
                        model.size > 1_000_000_000 ->
                            String.format("%.1f GB", model.size / 1_000_000_000.0)
                        model.size > 1_000_000 ->
                            String.format("%.1f MB", model.size / 1_000_000.0)
                        else -> "${model.size} B"
                    }
                    InfoRow(label = "  ${model.name}", value = sizeStr)
                }
            }
        }
    }
}

@Composable
private fun RateLimitInfoCard(state: LocalLlmUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Ограничения (Rate Limit)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            InfoRow(
                label = "Макс. запросов/мин",
                value = "${OllamaGenerateClient.MAX_REQUESTS_PER_MINUTE}",
            )
            InfoRow(
                label = "Макс. параллельных",
                value = "${OllamaGenerateClient.MAX_CONCURRENT_REQUESTS}",
            )
            InfoRow(
                label = "Макс. контекст",
                value = "${state.rateLimitConfig.maxContextTokens} токенов",
            )
            InfoRow(
                label = "Запросов за мин.",
                value = "${state.requestsInLastMinute}/${OllamaGenerateClient.MAX_REQUESTS_PER_MINUTE}",
            )
        }
    }
}

@Composable
private fun StabilityTestResultCard(result: StabilityTestResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (result.failureCount == 0) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Результат теста стабильности",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )

            InfoRow(label = "Всего запросов", value = "${result.totalRequests}")
            InfoRow(label = "Успешных", value = "${result.successCount}")
            InfoRow(label = "Неудачных", value = "${result.failureCount}")
            InfoRow(
                label = "Успешность",
                value = String.format("%.0f%%", result.successRate),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            InfoRow(label = "Среднее время", value = "${result.avgLatencyMs} ms")
            InfoRow(label = "Мин. время", value = "${result.minLatencyMs} ms")
            InfoRow(label = "Макс. время", value = "${result.maxLatencyMs} ms")

            if (result.errors.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = "Ошибки:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                result.errors.distinct().forEach { error ->
                    Text(
                        text = "• $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
        )
    }
}
