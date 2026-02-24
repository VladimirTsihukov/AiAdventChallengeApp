package com.tishukoff.aiadventchallengeapp.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tishukoff.feature.agent.api.Agent
import com.tishukoff.aiadventchallengeapp.presentation.ui.models.ChatIntent
import com.tishukoff.aiadventchallengeapp.presentation.ui.models.ChatUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ChatViewModel(
    private val agent: Agent
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        agent.conversationHistory
            .onEach { messages ->
                _uiState.value = _uiState.value.copy(messages = messages)
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            agent.loadHistory()
        }
    }

    fun handleIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.UpdateInput -> {
                _uiState.value = _uiState.value.copy(input = intent.text)
            }
            is ChatIntent.SendMessage -> sendMessage()
            is ChatIntent.ClearHistory -> clearHistory()
        }
    }

    private fun clearHistory() {
        viewModelScope.launch {
            agent.clearHistory()
        }
    }

    private fun sendMessage() {
        val message = _uiState.value.input.trim()
        if (message.isBlank()) return

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            input = ""
        )

        viewModelScope.launch {
            agent.addUserMessage(message)
            agent.processRequest()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}
