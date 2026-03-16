package com.tishukoff.feature.mcp.impl.presentation.orchestration

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tishukoff.feature.agent.api.Agent
import com.tishukoff.feature.mcp.impl.data.McpClientWrapper
import com.tishukoff.feature.mcp.impl.data.McpPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class OrchestrationViewModel(
    private val mcpClientWrapper: McpClientWrapper,
    private val agent: Agent,
    private val mcpPreferences: McpPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OrchestrationUiState(
            hostAddress = mcpPreferences.getSavedHost().orEmpty(),
        )
    )
    val uiState: StateFlow<OrchestrationUiState> = _uiState.asStateFlow()

    init {
        mcpClientWrapper.connectedServers
            .onEach { servers ->
                val connectedUrls = servers.map { it.url }.toSet()
                val host = _uiState.value.hostAddress
                _uiState.value = _uiState.value.copy(
                    servers = servers,
                    serverUrls = _uiState.value.serverUrls.map { entry ->
                        entry.copy(isConnected = entry.buildUrl(host) in connectedUrls)
                    },
                )
            }
            .launchIn(viewModelScope)

        agent.conversationHistory
            .onEach { messages ->
                _uiState.value = _uiState.value.copy(messages = messages)
            }
            .launchIn(viewModelScope)
    }

    fun updateHostAddress(host: String) {
        _uiState.value = _uiState.value.copy(hostAddress = host)
    }

    fun updateInput(text: String) {
        _uiState.value = _uiState.value.copy(input = text)
    }

    fun connectToServer(entry: ServerEntry) {
        val host = _uiState.value.hostAddress.trim()
        if (host.isBlank()) return
        val url = entry.buildUrl(host)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConnecting = true, connectionError = null)
            try {
                mcpClientWrapper.connectToServer(url)
                mcpPreferences.saveHost(host)
            } catch (e: Exception) {
                Log.e("Orchestration", "Connect error to $url", e)
                _uiState.value = _uiState.value.copy(
                    connectionError = "${entry.label}: ${e.message}",
                )
            } finally {
                _uiState.value = _uiState.value.copy(isConnecting = false)
            }
        }
    }

    fun disconnectFromServer(entry: ServerEntry) {
        val host = _uiState.value.hostAddress.trim()
        mcpClientWrapper.disconnectServer(entry.buildUrl(host))
    }

    fun sendMessage() {
        val text = _uiState.value.input.trim()
        if (text.isBlank()) return

        _uiState.value = _uiState.value.copy(input = "", isLoading = true)

        viewModelScope.launch {
            agent.addUserMessage(text)
            agent.processRequest()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            agent.clearHistory()
            agent.startNewChat()
        }
    }

    fun connectAll() {
        val host = _uiState.value.hostAddress.trim()
        if (host.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConnecting = true, connectionError = null)
            mcpPreferences.saveHost(host)
            val errors = mutableListOf<String>()
            for (entry in _uiState.value.serverUrls) {
                if (!entry.isConnected) {
                    val url = entry.buildUrl(host)
                    try {
                        mcpClientWrapper.connectToServer(url)
                    } catch (e: Exception) {
                        Log.e("Orchestration", "ConnectAll error for ${entry.label} ($url)", e)
                        errors.add("${entry.label}: ${e.message}")
                    }
                }
            }
            _uiState.value = _uiState.value.copy(
                isConnecting = false,
                connectionError = errors.takeIf { it.isNotEmpty() }?.joinToString("\n"),
            )
        }
    }

    fun disconnectAll() {
        mcpClientWrapper.disconnectAll()
    }
}
