package com.tishukoff.feature.mcp.impl.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.tishukoff.feature.mcp.api.McpTool
import com.tishukoff.feature.mcp.impl.presentation.McpUiState
import com.tishukoff.feature.mcp.impl.presentation.McpViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McpScreen(
    onBack: () -> Unit,
    onNavigateToScheduler: () -> Unit = {},
    onNavigateToPipeline: () -> Unit = {},
    viewModel: McpViewModel = koinViewModel(),
) {
    val state = viewModel.uiState.collectAsState().value
    val serverUrl = viewModel.serverUrl.collectAsState().value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MCP Tools") },
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
                .padding(16.dp)
                .imePadding(),
        ) {
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { viewModel.updateServerUrl(it) },
                label = { Text("MCP Server URL") },
                singleLine = true,
                trailingIcon = {
                    if (serverUrl.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateServerUrl("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { viewModel.connect() },
                    enabled = state !is McpUiState.Connecting,
                ) {
                    Text("Connect")
                }

                if (state is McpUiState.Connected) {
                    OutlinedButton(onClick = { viewModel.disconnect() }) {
                        Text("Disconnect")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (state) {
                is McpUiState.Idle -> {
                    Text(
                        text = "Enter MCP server URL and tap Connect",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                is McpUiState.Connecting -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Connecting to MCP server...")
                    }
                }

                is McpUiState.Connected -> {
                    ConnectedContent(
                        state = state,
                        onToolSelected = { viewModel.selectTool(it) },
                        onArgumentChanged = { key, value -> viewModel.updateToolArgument(key, value) },
                        onCallTool = { viewModel.callTool() },
                        onClearResult = { viewModel.clearToolResult() },
                        onNavigateToScheduler = onNavigateToScheduler,
                        onNavigateToPipeline = onNavigateToPipeline,
                    )
                }

                is McpUiState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectedContent(
    state: McpUiState.Connected,
    onToolSelected: (McpTool) -> Unit,
    onArgumentChanged: (String, String) -> Unit,
    onCallTool: () -> Unit,
    onClearResult: () -> Unit,
    onNavigateToScheduler: () -> Unit,
    onNavigateToPipeline: () -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = "Server: ${state.serverName} v${state.serverVersion}",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Available tools: ${state.tools.size}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onNavigateToScheduler,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Scheduled Tasks")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onNavigateToPipeline,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Pipeline")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(state.tools, key = { it.name }) { tool ->
            val isSelected = state.selectedTool?.name == tool.name
            ToolCard(
                tool = tool,
                isSelected = isSelected,
                onClick = { onToolSelected(tool) },
            )
        }

        if (state.selectedTool != null) {
            item {
                ToolCallSection(
                    tool = state.selectedTool,
                    arguments = state.toolArguments,
                    result = state.toolResult,
                    isCalling = state.isCallingTool,
                    onArgumentChanged = onArgumentChanged,
                    onCallTool = onCallTool,
                    onClearResult = onClearResult,
                )
            }
        }
    }
}

@Composable
private fun ToolCard(
    tool: McpTool,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = if (isSelected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            )
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Text(
                text = tool.name,
                style = MaterialTheme.typography.titleMedium,
            )
            if (tool.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ToolCallSection(
    tool: McpTool,
    arguments: Map<String, String>,
    result: String?,
    isCalling: Boolean,
    onArgumentChanged: (String, String) -> Unit,
    onCallTool: () -> Unit,
    onClearResult: () -> Unit,
) {
    Column {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Call: ${tool.name}",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))

        val paramNames = parseParamNames(tool.inputSchemaJson)
        paramNames.forEach { param ->
            OutlinedTextField(
                value = arguments[param].orEmpty(),
                onValueChange = { onArgumentChanged(param, it) },
                label = { Text(param) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onCallTool,
                enabled = !isCalling,
            ) {
                if (isCalling) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text("Call Tool")
            }

            if (result != null) {
                OutlinedButton(onClick = onClearResult) {
                    Text("Clear")
                }
            }
        }

        if (result != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Result:",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = result,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
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
