package com.tishukoff.feature.mcp.impl.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tishukoff.feature.mcp.impl.data.McpClientWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class McpViewModel(
    private val mcpClientWrapper: McpClientWrapper,
) : ViewModel() {

    private val _uiState = MutableStateFlow<McpUiState>(McpUiState.Idle)
    val uiState: StateFlow<McpUiState> = _uiState.asStateFlow()

    private val _serverUrl = MutableStateFlow(DEFAULT_SERVER_URL)
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    fun updateServerUrl(url: String) {
        _serverUrl.value = url
    }

    fun connect() {
        viewModelScope.launch {
            _uiState.value = McpUiState.Connecting
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
