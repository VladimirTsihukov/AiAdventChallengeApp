package com.tishukoff.aiadventchallengeapp.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tishukoff.feature.agent.api.Agent
import com.tishukoff.aiadventchallengeapp.presentation.ui.models.ChatIntent
import com.tishukoff.aiadventchallengeapp.presentation.ui.models.ChatUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val agent: Agent
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(settings = agent.settings))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun handleIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.UpdateInput -> {
                _uiState.value = _uiState.value.copy(input = intent.text)
            }
            is ChatIntent.SendMessage -> sendMessage()
            is ChatIntent.SaveSettings -> {
                agent.updateSettings(intent.settings)
                _uiState.value = _uiState.value.copy(settings = intent.settings)
            }
        }
    }

    private fun sendMessage() {
        val message = _uiState.value.input.trim()
        if (message.isBlank()) return

        agent.addUserMessage(message)
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            input = "",
            messages = agent.conversationHistory
        )

        viewModelScope.launch {
            agent.processRequest()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                messages = agent.conversationHistory
            )
        }
    }
}
