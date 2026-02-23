package com.tishukoff.aiadventchallengeapp.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tishukoff.aiadventchallengeapp.data.ClaudeModel
import com.tishukoff.aiadventchallengeapp.data.ClaudeRepository
import com.tishukoff.aiadventchallengeapp.data.ResponseMetadata
import com.tishukoff.aiadventchallengeapp.data.SettingsRepository
import com.tishukoff.aiadventchallengeapp.presentation.ui.models.ChatMessage
import com.tishukoff.aiadventchallengeapp.presentation.ui.models.CompareIntent
import com.tishukoff.aiadventchallengeapp.presentation.ui.models.CompareResult
import com.tishukoff.aiadventchallengeapp.presentation.ui.models.CompareUiState
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class CompareViewModel(
    private val repository: ClaudeRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompareUiState())
    val uiState: StateFlow<CompareUiState> = _uiState.asStateFlow()

    fun handleIntent(intent: CompareIntent) {
        when (intent) {
            is CompareIntent.UpdatePrompt -> {
                _uiState.value = _uiState.value.copy(prompt = intent.text)
            }
            is CompareIntent.Compare -> compare()
        }
    }

    private fun formatMetadata(metadata: ResponseMetadata): String {
        val timeSec = metadata.responseTimeMs / 1000.0
        val cost = String.format(Locale.US, "%.4f", metadata.costUsd)
        val time = String.format(Locale.US, "%.1f", timeSec)
        return "${metadata.modelId} | in: ${metadata.inputTokens} out: ${metadata.outputTokens} | ${time}s | \$$cost"
    }

    private fun compare() {
        val prompt = _uiState.value.prompt.trim()
        if (prompt.isBlank()) return

        val baseSettings = settingsRepository.load()
        val conversation = listOf(ChatMessage(text = prompt, isUser = true))

        val initialResults = ClaudeModel.entries.associateWith { CompareResult.Loading as CompareResult }
        _uiState.value = _uiState.value.copy(isLoading = true, results = initialResults)

        viewModelScope.launch {
            val deferreds = ClaudeModel.entries.map { model ->
                val settings = baseSettings.copy(model = model)
                async {
                    model to try {
                        val (text, metadata) = repository.sendMessage(conversation, settings)
                        CompareResult.Success(text, formatMetadata(metadata))
                    } catch (e: Exception) {
                        CompareResult.Error(e.message ?: "Unknown error")
                    }
                }
            }

            for (deferred in deferreds) {
                val (model, result) = deferred.await()
                _uiState.value = _uiState.value.copy(
                    results = _uiState.value.results + (model to result)
                )
            }

            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}
