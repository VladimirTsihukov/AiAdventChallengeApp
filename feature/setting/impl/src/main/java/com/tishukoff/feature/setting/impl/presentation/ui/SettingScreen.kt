package com.tishukoff.feature.setting.impl.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tishukoff.feature.agent.api.ClaudeModel
import com.tishukoff.feature.agent.api.CompressionSettings
import com.tishukoff.feature.agent.api.LlmSettings
import com.tishukoff.feature.setting.impl.presentation.SettingViewModel
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    onBack: () -> Unit,
    viewModel: SettingViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LLM Settings") },
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
        SettingContent(
            modifier = Modifier.padding(innerPadding),
            settings = state.settings,
            onSave = { settings ->
                viewModel.saveSettings(settings)
                onBack()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingContent(
    modifier: Modifier = Modifier,
    settings: LlmSettings,
    onSave: (LlmSettings) -> Unit,
) {
    var selectedModel by remember(settings) { mutableStateOf(settings.model) }
    var maxTokens by remember(settings) { mutableStateOf(settings.maxTokens.toString()) }
    var temperature by remember(settings) { mutableFloatStateOf(settings.temperature) }
    var stopSequences by remember(settings) {
        mutableStateOf(settings.stopSequences.joinToString(", "))
    }
    var systemPrompt by remember(settings) { mutableStateOf(settings.systemPrompt) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    var compressionEnabled by remember(settings) { mutableStateOf(settings.compression.enabled) }
    var recentMessagesToKeep by remember(settings) {
        mutableIntStateOf(settings.compression.recentMessagesToKeep)
    }
    var summarizationBatchSize by remember(settings) {
        mutableIntStateOf(settings.compression.summarizationBatchSize)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        ExposedDropdownMenuBox(
            expanded = modelDropdownExpanded,
            onExpandedChange = { modelDropdownExpanded = it },
        ) {
            OutlinedTextField(
                value = selectedModel.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Model") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelDropdownExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = modelDropdownExpanded,
                onDismissRequest = { modelDropdownExpanded = false },
            ) {
                ClaudeModel.entries.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model.displayName) },
                        onClick = {
                            selectedModel = model
                            modelDropdownExpanded = false
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = maxTokens,
            onValueChange = { maxTokens = it.filter { c -> c.isDigit() } },
            label = { Text("max_tokens") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "temperature: ${(temperature * 10).roundToInt() / 10f}")
        Slider(
            value = temperature,
            onValueChange = { temperature = it },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = systemPrompt,
            onValueChange = { systemPrompt = it },
            label = { Text("system prompt") },
            trailingIcon = {
                if (systemPrompt.isNotEmpty()) {
                    IconButton(onClick = { systemPrompt = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            maxLines = 5,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = stopSequences,
            onValueChange = { stopSequences = it },
            label = { Text("stop_sequences (comma-separated)") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Context Compression",
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Enable compression",
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = compressionEnabled,
                onCheckedChange = { compressionEnabled = it },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(text = "Recent messages to keep: $recentMessagesToKeep")
        Slider(
            value = recentMessagesToKeep.toFloat(),
            onValueChange = { recentMessagesToKeep = it.roundToInt() },
            valueRange = 2f..20f,
            steps = 17,
            enabled = compressionEnabled,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Batch size for summarization: $summarizationBatchSize")
        Slider(
            value = summarizationBatchSize.toFloat(),
            onValueChange = { summarizationBatchSize = it.roundToInt() },
            valueRange = 5f..20f,
            steps = 14,
            enabled = compressionEnabled,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val tokens = maxTokens.toIntOrNull() ?: 1024
                val stops = stopSequences.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                onSave(
                    LlmSettings(
                        model = selectedModel,
                        maxTokens = tokens,
                        temperature = temperature,
                        stopSequences = stops,
                        systemPrompt = systemPrompt,
                        compression = CompressionSettings(
                            enabled = compressionEnabled,
                            recentMessagesToKeep = recentMessagesToKeep,
                            summarizationBatchSize = summarizationBatchSize,
                        ),
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingContentPreview() {
    MaterialTheme {
        SettingContent(
            settings = LlmSettings(),
            onSave = {},
        )
    }
}
