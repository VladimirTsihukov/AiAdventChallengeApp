package com.tishukoff.feature.localllm.impl.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tishukoff.feature.localllm.impl.domain.model.BenchmarkComparison
import com.tishukoff.feature.localllm.impl.domain.model.BenchmarkEntry
import com.tishukoff.feature.localllm.impl.domain.model.LlmConfig
import com.tishukoff.feature.localllm.impl.domain.model.LocalLlmMessage
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

            state.benchmarkResult?.let { result ->
                BenchmarkResultPanel(
                    result = result,
                    onDismiss = { viewModel.handleIntent(LocalLlmIntent.DismissBenchmark) },
                )
            }

            if (state.isBenchmarkRunning) {
                BenchmarkProgressPanel(progress = state.benchmarkProgress)
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
private fun BenchmarkProgressPanel(progress: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = progress,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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
