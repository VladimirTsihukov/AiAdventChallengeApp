package com.tishukoff.feature.mcp.impl.presentation.pipeline

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tishukoff.feature.mcp.impl.data.PipelineRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PipelineViewModel(
    private val repository: PipelineRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PipelineUiState>(PipelineUiState.Configuring())
    val uiState: StateFlow<PipelineUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    fun updateQuery(query: String) {
        updateForm { it.copy(query = query) }
    }

    fun updateFilename(filename: String) {
        updateForm { it.copy(filename = filename) }
    }

    fun updatePerPage(perPage: String) {
        updateForm { it.copy(perPage = perPage) }
    }

    fun toggleSearch(enabled: Boolean) {
        updateForm { it.copy(searchEnabled = enabled) }
    }

    fun toggleSummarize(enabled: Boolean) {
        updateForm { it.copy(summarizeEnabled = enabled) }
    }

    fun toggleSaveToFile(enabled: Boolean) {
        updateForm { it.copy(saveToFileEnabled = enabled) }
    }

    fun toggleTelegram(enabled: Boolean) {
        updateForm { it.copy(telegramEnabled = enabled) }
    }

    fun runPipeline() {
        val state = _uiState.value
        if (state !is PipelineUiState.Configuring) return

        val form = state.form
        if (form.query.isBlank()) return
        if (!form.searchEnabled && !form.summarizeEnabled && !form.saveToFileEnabled && !form.telegramEnabled) return

        viewModelScope.launch {
            try {
                val result = repository.createPipeline(
                    query = form.query,
                    filename = form.filename,
                    perPage = form.perPage.toIntOrNull() ?: 5,
                    searchEnabled = form.searchEnabled,
                    summarizeEnabled = form.summarizeEnabled,
                    saveToFileEnabled = form.saveToFileEnabled,
                    telegramEnabled = form.telegramEnabled,
                )

                _uiState.value = PipelineUiState.Running(
                    pipelineId = result.id,
                    steps = result.steps,
                    overallStatus = result.status,
                )

                startPolling(result.id)
            } catch (e: Exception) {
                Log.e(TAG, "runPipeline error", e)
                val message = e.message.orEmpty()
                if (message.contains("Not connected to MCP server")) {
                    _uiState.value = PipelineUiState.NotConnected
                } else {
                    _uiState.value = PipelineUiState.Error(message.ifBlank { "Failed to create pipeline" })
                }
            }
        }
    }

    fun cancelPipeline() {
        val state = _uiState.value
        if (state !is PipelineUiState.Running) return

        viewModelScope.launch {
            try {
                repository.cancelPipeline(state.pipelineId)
                pollingJob?.cancel()
                _uiState.value = PipelineUiState.Completed(
                    pipelineId = state.pipelineId,
                    steps = state.steps,
                    overallStatus = "CANCELLED",
                )
            } catch (e: Exception) {
                Log.d(TAG, "cancelPipeline error: ${e.message}")
            }
        }
    }

    fun resetToConfiguring() {
        pollingJob?.cancel()
        _uiState.value = PipelineUiState.Configuring()
    }

    private fun startPolling(pipelineId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(POLL_INTERVAL_MS)
                try {
                    val status = repository.getPipelineStatus(pipelineId)
                    when (status.status) {
                        "RUNNING" -> {
                            _uiState.value = PipelineUiState.Running(
                                pipelineId = status.id,
                                steps = status.steps,
                                overallStatus = status.status,
                            )
                        }
                        else -> {
                            _uiState.value = PipelineUiState.Completed(
                                pipelineId = status.id,
                                steps = status.steps,
                                overallStatus = status.status,
                            )
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "polling error: ${e.message}")
                }
            }
        }
    }

    private fun updateForm(transform: (PipelineForm) -> PipelineForm) {
        _uiState.update { state ->
            if (state is PipelineUiState.Configuring) {
                state.copy(form = transform(state.form))
            } else state
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }

    private companion object {
        const val TAG = "PipelineViewModel"
        const val POLL_INTERVAL_MS = 3_000L
    }
}
