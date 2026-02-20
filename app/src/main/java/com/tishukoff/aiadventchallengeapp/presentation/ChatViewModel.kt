package com.tishukoff.aiadventchallengeapp.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tishukoff.aiadventchallengeapp.data.ClaudeRepository
import com.tishukoff.aiadventchallengeapp.data.ResponseMetadata
import com.tishukoff.aiadventchallengeapp.data.SettingsRepository
import com.tishukoff.aiadventchallengeapp.presentation.ui.models.ChatIntent
import com.tishukoff.aiadventchallengeapp.presentation.ui.models.ChatMessage
import com.tishukoff.aiadventchallengeapp.presentation.ui.models.ChatUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class ChatViewModel(
    private val repository: ClaudeRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(settings = settingsRepository.load())
    }

    fun handleIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.UpdateInput -> {
                _uiState.value = _uiState.value.copy(input = intent.text)
            }
            is ChatIntent.SendMessage -> sendMessage()
            is ChatIntent.SaveSettings -> {
                settingsRepository.save(intent.settings)
                _uiState.value = _uiState.value.copy(settings = intent.settings)
            }
        }
    }

    private fun formatMetadata(metadata: ResponseMetadata): String {
        val timeSec = metadata.responseTimeMs / 1000.0
        val cost = String.format(Locale.US, "%.4f", metadata.costUsd)
        val time = String.format(Locale.US, "%.1f", timeSec)
        return "${metadata.modelId} | in: ${metadata.inputTokens} out: ${metadata.outputTokens} | ${time}s | \$$cost"
    }

    private fun sendMessage() {
        val message = _uiState.value.input.trim()
        if (message.isBlank()) return

        val userMessage = ChatMessage(text = message, isUser = true)
        val updatedMessages = _uiState.value.messages + userMessage

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            input = "",
            messages = updatedMessages
        )

        viewModelScope.launch {
            val aiMessage = try {
                val (text, metadata) = repository.sendMessage(
                    updatedMessages,
                    _uiState.value.settings
                )
                val metadataText = formatMetadata(metadata)
                Log.d("Logger_2", "result message:\n$text")
                Log.d("Logger_2", "result metadata:\n$metadataText")
                ChatMessage(
                    text = text,
                    isUser = false,
                    metadataText = metadataText
                )
            } catch (e: Exception) {
                ChatMessage(text = "Error: ${e.message}", isUser = false)
            }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                messages = _uiState.value.messages + aiMessage
            )
        }
    }
}
