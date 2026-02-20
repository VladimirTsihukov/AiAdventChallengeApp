package com.tishukoff.aiadventchallengeapp.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import android.content.ClipData
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.tishukoff.aiadventchallengeapp.data.ClaudeModel
import com.tishukoff.aiadventchallengeapp.presentation.CompareViewModel
import com.tishukoff.aiadventchallengeapp.presentation.ui.models.CompareIntent
import com.tishukoff.aiadventchallengeapp.presentation.ui.models.CompareResult

@Composable
fun CompareScreen(
    modifier: Modifier = Modifier,
    viewModel: CompareViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    val allSucceeded = state.results.size == ClaudeModel.entries.size &&
            state.results.values.all { it is CompareResult.Success }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.prompt,
            onValueChange = { viewModel.handleIntent(CompareIntent.UpdatePrompt(it)) },
            placeholder = { Text("Enter prompt to compare...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            minLines = 2,
            maxLines = 5
        )

        Button(
            onClick = { viewModel.handleIntent(CompareIntent.Compare) },
            enabled = state.prompt.isNotBlank() && !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Text("Compare All Models")
        }

        if (allSucceeded) {
            OutlinedButton(
                onClick = {
                    val text = buildCopyText(state.prompt, state.results)
                    scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("compare", text))) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Copy All Results")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(ClaudeModel.entries) { model ->
                val result = state.results[model]
                ModelResultCard(model = model, result = result)
            }
        }
    }
}

private fun buildCopyText(
    prompt: String,
    results: Map<ClaudeModel, CompareResult>
): String = buildString {
    appendLine("Prompt: $prompt")
    appendLine()
    for (model in ClaudeModel.entries) {
        val result = results[model] as? CompareResult.Success ?: continue
        appendLine("=== ${model.displayName} ===")
        appendLine(result.text)
        appendLine("[${result.metadataText}]")
        appendLine()
    }
}

@Composable
private fun ModelResultCard(
    model: ClaudeModel,
    result: CompareResult?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = model.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            when (result) {
                null -> {
                    Text(
                        text = "Waiting for prompt...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is CompareResult.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.CenterHorizontally),
                        strokeWidth = 2.dp
                    )
                }
                is CompareResult.Success -> {
                    Text(
                        text = result.text,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = result.metadataText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is CompareResult.Error -> {
                    Text(
                        text = "Error: ${result.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
