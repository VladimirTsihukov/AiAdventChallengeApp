package com.tishukoff.feature.mcp.impl.presentation.scheduler.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tishukoff.feature.mcp.api.McpTool
import com.tishukoff.feature.mcp.impl.presentation.scheduler.CreateTaskForm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskDialog(
    form: CreateTaskForm,
    availableTools: List<McpTool>,
    isCreating: Boolean,
    onNameChange: (String) -> Unit,
    onToolNameChange: (String) -> Unit,
    onToolArgumentChange: (String, String) -> Unit,
    onIntervalChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "Create Scheduled Task",
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = form.name,
                onValueChange = onNameChange,
                label = { Text("Task name") },
                placeholder = { Text("e.g. Track Kotlin repo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            ToolDropdown(
                selectedTool = form.toolName,
                tools = availableTools,
                onToolSelected = onToolNameChange,
            )

            Spacer(modifier = Modifier.height(12.dp))

            val selectedTool = availableTools.find { it.name == form.toolName }
            if (selectedTool != null) {
                Text(
                    text = "Tool arguments",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(modifier = Modifier.height(4.dp))

                val paramNames = parseParamNames(selectedTool.inputSchemaJson)
                paramNames.forEach { param ->
                    OutlinedTextField(
                        value = form.toolArguments[param].orEmpty(),
                        onValueChange = { onToolArgumentChange(param, it) },
                        label = { Text(param) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = form.intervalMinutes,
                    onValueChange = onIntervalChange,
                    label = { Text("Interval (min)") },
                    placeholder = { Text("60") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    supportingText = { Text(formatMinutesHint(form.intervalMinutes)) },
                )

                OutlinedTextField(
                    value = form.durationMinutes,
                    onValueChange = onDurationChange,
                    label = { Text("Duration (min)") },
                    placeholder = { Text("1440") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    supportingText = { Text(formatMinutesHint(form.durationMinutes)) },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onCreate,
                    enabled = !isCreating
                            && form.name.isNotBlank()
                            && form.toolName.isNotBlank()
                            && form.intervalMinutes.toIntOrNull() != null
                            && form.durationMinutes.toIntOrNull() != null,
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    Text("Create")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolDropdown(
    selectedTool: String,
    tools: List<McpTool>,
    onToolSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val schedulerTools = listOf(
        "get_repository",
        "get_repository_summary",
        "search_repositories",
        "get_file_content"
    )
    val filteredTools = tools.filter { it.name in schedulerTools }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedTool.ifBlank { "Select tool" },
            onValueChange = {},
            readOnly = true,
            label = { Text("Tool") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            filteredTools.forEach { tool ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(tool.name, style = MaterialTheme.typography.bodyMedium)
                            if (tool.description.isNotBlank()) {
                                Text(
                                    tool.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    onClick = {
                        onToolSelected(tool.name)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun parseParamNames(inputSchemaJson: String): List<String> {
    val regex = """"(\w+)"\s*:\s*\{""".toRegex()
    return regex.findAll(inputSchemaJson)
        .map { it.groupValues[1] }
        .filter { it != "type" && it != "properties" }
        .toList()
}

private fun formatMinutesHint(input: String): String {
    val minutes = input.toIntOrNull() ?: return ""
    return when {
        minutes < 60 -> "${minutes}m"
        minutes < 1440 -> "${minutes / 60}h ${minutes % 60}m"
        else -> "${minutes / 1440}d ${(minutes % 1440) / 60}h"
    }
}
