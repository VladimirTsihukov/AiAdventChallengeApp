package com.tishukoff.aiadventchallengeapp.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tishukoff.feature.agent.api.ClaudeModel
import com.tishukoff.feature.agent.api.LlmSettings
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDrawer(
    settings: LlmSettings,
    onSave: (LlmSettings) -> Unit
) {
    var selectedModel by remember(settings) { mutableStateOf(settings.model) }
    var maxTokens by remember(settings) { mutableStateOf(settings.maxTokens.toString()) }
    var temperature by remember(settings) { mutableFloatStateOf(settings.temperature) }
    var stopSequences by remember(settings) {
        mutableStateOf(settings.stopSequences.joinToString(", "))
    }
    var systemPrompt by remember(settings) { mutableStateOf(settings.systemPrompt) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    ModalDrawerSheet {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "LLM Settings",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            ExposedDropdownMenuBox(
                expanded = modelDropdownExpanded,
                onExpandedChange = { modelDropdownExpanded = it }
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
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = modelDropdownExpanded,
                    onDismissRequest = { modelDropdownExpanded = false }
                ) {
                    ClaudeModel.entries.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model.displayName) },
                            onClick = {
                                selectedModel = model
                                modelDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = maxTokens,
                onValueChange = { maxTokens = it.filter { c -> c.isDigit() } },
                label = { Text("max_tokens") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "temperature: ${(temperature * 10).roundToInt() / 10f}")
            Slider(
                value = temperature,
                onValueChange = { temperature = it },
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth()
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
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = stopSequences,
                onValueChange = { stopSequences = it },
                label = { Text("stop_sequences (comma-separated)") },
                modifier = Modifier.fillMaxWidth()
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
                            systemPrompt = systemPrompt
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
