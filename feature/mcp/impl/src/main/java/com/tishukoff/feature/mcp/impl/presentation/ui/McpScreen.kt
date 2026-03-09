package com.tishukoff.feature.mcp.impl.presentation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
    viewModel: McpViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()

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
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { viewModel.updateServerUrl(it) },
                label = { Text("MCP Server URL") },
                singleLine = true,
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

            AnimatedContent(
                targetState = state,
                label = "mcp_state",
            ) { currentState ->
                when (currentState) {
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
                            serverName = currentState.serverName,
                            serverVersion = currentState.serverVersion,
                            tools = currentState.tools,
                        )
                    }

                    is McpUiState.Error -> {
                        Text(
                            text = "Error: ${currentState.message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectedContent(
    serverName: String,
    serverVersion: String,
    tools: List<McpTool>,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = "Server: $serverName v$serverVersion",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Available tools: ${tools.size}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(tools, key = { it.name }) { tool ->
            ToolCard(tool = tool)
        }
    }
}

@Composable
private fun ToolCard(tool: McpTool) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tool.inputSchemaJson,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
