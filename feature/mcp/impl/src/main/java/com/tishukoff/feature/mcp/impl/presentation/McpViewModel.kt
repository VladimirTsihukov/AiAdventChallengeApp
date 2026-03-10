package com.tishukoff.feature.mcp.impl.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tishukoff.feature.mcp.api.McpTool
import com.tishukoff.feature.mcp.impl.data.McpClientWrapper
import com.tishukoff.feature.mcp.impl.data.McpPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class McpViewModel(
    private val mcpClientWrapper: McpClientWrapper,
    private val mcpPreferences: McpPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow<McpUiState>(McpUiState.Idle)
    val uiState: StateFlow<McpUiState> = _uiState.asStateFlow()

    private val _serverUrl = MutableStateFlow(mcpPreferences.getSavedUrl() ?: DEFAULT_SERVER_URL)
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    fun updateServerUrl(url: String) {
        _serverUrl.value = url
    }

    fun connect() {
        viewModelScope.launch {
            _uiState.value = McpUiState.Connecting
            mcpPreferences.saveUrl(_serverUrl.value)
            try {
                val result = mcpClientWrapper.connectAndListTools(_serverUrl.value)
                _uiState.value = McpUiState.Connected(
                    serverName = result.serverName,
                    serverVersion = result.serverVersion,
                    tools = result.tools,
                )
            } catch (e: Exception) {
                Log.d("Logger_2","error: ${e.message}")
                _uiState.value = McpUiState.Error(
                    message = e.message ?: "Unknown error",
                )
            }
        }
    }

    fun selectTool(tool: McpTool) {
        _uiState.update { state ->
            if (state is McpUiState.Connected) {
                state.copy(
                    selectedTool = tool,
                    toolArguments = emptyMap(),
                    toolResult = null,
                )
            } else state
        }
    }

    fun updateToolArgument(key: String, value: String) {
        _uiState.update { state ->
            if (state is McpUiState.Connected) {
                state.copy(toolArguments = state.toolArguments + (key to value))
            } else state
        }
    }

    fun callTool() {
        val state = _uiState.value
        if (state !is McpUiState.Connected || state.selectedTool == null) return

        viewModelScope.launch {
            _uiState.update { (it as McpUiState.Connected).copy(isCallingTool = true, toolResult = null) }
            try {
                val arguments: Map<String, Any?> = state.toolArguments
                val result = mcpClientWrapper.callTool(state.selectedTool.name, arguments)
                _uiState.update { (it as McpUiState.Connected).copy(toolResult = result, isCallingTool = false) }
            } catch (e: Exception) {
                Log.d("Logger_2", "callTool error: ${e.message}")
                _uiState.update {
                    (it as McpUiState.Connected).copy(
                        toolResult = "Error: ${e.message}",
                        isCallingTool = false,
                    )
                }
            }
        }
    }

    fun clearToolResult() {
        _uiState.update { state ->
            if (state is McpUiState.Connected) {
                state.copy(selectedTool = null, toolResult = null, toolArguments = emptyMap())
            } else state
        }
    }

    fun disconnect() {
        mcpClientWrapper.disconnect()
        _uiState.value = McpUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        mcpClientWrapper.disconnect()
    }

    private companion object {
        const val DEFAULT_SERVER_URL = "https://mcp.deepwiki.com/mcp"
    }
}
