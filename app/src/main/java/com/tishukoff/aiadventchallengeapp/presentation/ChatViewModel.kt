package com.tishukoff.aiadventchallengeapp.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tishukoff.core.database.api.ChatStorage
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
    private val agent: Agent,
    private val chatStorage: ChatStorage,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        agent.conversationHistory
            .onEach { messages ->
                _uiState.value = _uiState.value.copy(messages = messages)
            }
            .launchIn(viewModelScope)

        chatStorage.getAllChats()
            .onEach { chats ->
                _uiState.value = _uiState.value.copy(chats = chats)
            }
            .launchIn(viewModelScope)

        agent.currentChatId
            .onEach { chatId ->
                _uiState.value = _uiState.value.copy(currentChatId = chatId)
            }
            .launchIn(viewModelScope)

        agent.tokenStats
            .onEach { stats ->
                _uiState.value = _uiState.value.copy(tokenStats = stats)
            }
            .launchIn(viewModelScope)

        agent.compressionStats
            .onEach { stats ->
                _uiState.value = _uiState.value.copy(compressionStats = stats)
            }
            .launchIn(viewModelScope)
    }

    fun handleIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.UpdateInput -> {
                _uiState.value = _uiState.value.copy(input = intent.text)
            }
            is ChatIntent.SendMessage -> sendMessage()
            is ChatIntent.ClearHistory -> clearHistory()
            is ChatIntent.NewChat -> newChat()
            is ChatIntent.SelectChat -> selectChat(intent.chatId)
            is ChatIntent.DeleteChat -> deleteChat(intent.chatId)
        }
    }

    private fun clearHistory() {
        viewModelScope.launch {
            agent.clearHistory()
        }
    }

    private fun newChat() {
        agent.startNewChat()
    }

    private fun selectChat(chatId: Long) {
        viewModelScope.launch {
            agent.selectChat(chatId)
        }
    }

    private fun deleteChat(chatId: Long) {
        viewModelScope.launch {
            val isCurrentChat = _uiState.value.currentChatId == chatId
            chatStorage.deleteChat(chatId)
            if (isCurrentChat) {
                agent.startNewChat()
            }
        }
    }

    private fun sendMessage() {
        val message = _uiState.value.input.trim()
        if (message.isBlank()) return

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            input = "",
        )

        viewModelScope.launch {
            agent.addUserMessage(message)
            agent.processRequest()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}
