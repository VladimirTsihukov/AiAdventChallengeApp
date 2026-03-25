package com.tishukoff.feature.localllm.impl.presentation.telegrambot

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tishukoff.feature.localllm.impl.domain.usecase.GetTelegramBotChatsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TelegramBotViewModel internal constructor(
    private val getTelegramBotChatsUseCase: GetTelegramBotChatsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TelegramBotUiState())
    val uiState: StateFlow<TelegramBotUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        handleIntent(TelegramBotIntent.LoadChats)
        startPolling()
    }

    fun handleIntent(intent: TelegramBotIntent) {
        when (intent) {
            is TelegramBotIntent.LoadChats -> {
                _uiState.update { it.copy(selectedChatId = null, messages = emptyList()) }
                loadChats()
            }
            is TelegramBotIntent.SelectChat -> selectChat(intent.chatId)
            is TelegramBotIntent.RefreshMessages -> refreshMessages()
            is TelegramBotIntent.ClearHistory -> clearHistory()
            is TelegramBotIntent.DismissError -> {
                _uiState.update { it.copy(error = null) }
            }
        }
    }

    private fun startPolling() {
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(POLL_INTERVAL_MS)
                pollSilently()
            }
        }
    }

    private suspend fun pollSilently() {
        val chatId = _uiState.value.selectedChatId
        if (chatId != null) {
            getTelegramBotChatsUseCase.getMessages(chatId).onSuccess { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        } else {
            getTelegramBotChatsUseCase.getChats().onSuccess { chats ->
                _uiState.update { it.copy(chats = chats) }
            }
        }
    }

    private fun loadChats() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            getTelegramBotChatsUseCase.getChats().fold(
                onSuccess = { chats ->
                    _uiState.update { it.copy(chats = chats, isLoading = false) }
                    if (chats.size == 1 && _uiState.value.selectedChatId == null) {
                        selectChat(chats.first().chatId)
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to load chats", error)
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "Failed to load chats")
                    }
                },
            )
            getTelegramBotChatsUseCase.getModelName().onSuccess { model ->
                _uiState.update { it.copy(modelName = model) }
            }
        }
    }

    private fun selectChat(chatId: Long) {
        _uiState.update { it.copy(selectedChatId = chatId, isLoading = true) }
        viewModelScope.launch {
            getTelegramBotChatsUseCase.getMessages(chatId).fold(
                onSuccess = { messages ->
                    _uiState.update { it.copy(messages = messages, isLoading = false) }
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to load messages for chat $chatId", error)
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "Failed to load messages")
                    }
                },
            )
        }
    }

    private fun clearHistory() {
        val chatId = _uiState.value.selectedChatId ?: return
        viewModelScope.launch {
            getTelegramBotChatsUseCase.clearHistory(chatId).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(selectedChatId = null, messages = emptyList())
                    }
                    loadChats()
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to clear history for chat $chatId", error)
                    _uiState.update {
                        it.copy(error = error.message ?: "Failed to clear history")
                    }
                },
            )
        }
    }

    private fun refreshMessages() {
        val chatId = _uiState.value.selectedChatId
        if (chatId != null) {
            selectChat(chatId)
        } else {
            loadChats()
        }
    }

    private companion object {
        const val TAG = "TelegramBotVM"
        const val POLL_INTERVAL_MS = 3000L
    }
}
