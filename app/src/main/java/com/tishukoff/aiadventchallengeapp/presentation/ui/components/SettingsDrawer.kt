package com.tishukoff.aiadventchallengeapp.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.tishukoff.aiadventchallengeapp.data.LlmSettings
import kotlin.math.roundToInt

@Composable
fun SettingsDrawer(
    settings: LlmSettings,
    onSave: (LlmSettings) -> Unit
) {
    var maxTokens by remember(settings) { mutableStateOf(settings.maxTokens.toString()) }
    var temperature by remember(settings) { mutableFloatStateOf(settings.temperature) }
    var stopSequences by remember(settings) {
        mutableStateOf(settings.stopSequences.joinToString(", "))
    }
    var systemPrompt by remember(settings) { mutableStateOf(settings.systemPrompt) }

    ModalDrawerSheet {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "LLM Settings",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

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
                    onSave(LlmSettings(tokens, temperature, stops, systemPrompt))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
