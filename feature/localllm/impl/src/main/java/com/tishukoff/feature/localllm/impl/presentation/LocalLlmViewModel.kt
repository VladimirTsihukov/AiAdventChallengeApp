package com.tishukoff.feature.localllm.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tishukoff.feature.localllm.impl.domain.model.LocalLlmMessage
import com.tishukoff.feature.localllm.impl.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LocalLlmViewModel internal constructor(
    private val sendMessageUseCase: SendMessageUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocalLlmUiState())
    val uiState: StateFlow<LocalLlmUiState> = _uiState.asStateFlow()

    fun handleIntent(intent: LocalLlmIntent) {
        when (intent) {
            is LocalLlmIntent.UpdateInput -> {
                _uiState.update { it.copy(input = intent.text) }
            }
            is LocalLlmIntent.SendMessage -> sendMessage()
            is LocalLlmIntent.ClearHistory -> {
                _uiState.update { it.copy(messages = emptyList()) }
            }
            is LocalLlmIntent.DismissError -> {
                _uiState.update { it.copy(error = null) }
            }
        }
    }

    private fun sendMessage() {
        val input = _uiState.value.input.trim()
        if (input.isEmpty()) return

        val userMessage = LocalLlmMessage(text = input, role = LocalLlmMessage.Role.USER)
        _uiState.update {
            it.copy(
                input = "",
                messages = it.messages + userMessage,
                isLoading = true,
                error = null,
            )
        }

        viewModelScope.launch {
            val result = sendMessageUseCase(_uiState.value.messages)
            result.fold(
                onSuccess = { response ->
                    val assistantMessage = LocalLlmMessage(
                        text = response,
                        role = LocalLlmMessage.Role.ASSISTANT,
                    )
                    _uiState.update {
                        it.copy(
                            messages = it.messages + assistantMessage,
                            isLoading = false,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Unknown error",
                        )
                    }
                },
            )
        }
    }
}
