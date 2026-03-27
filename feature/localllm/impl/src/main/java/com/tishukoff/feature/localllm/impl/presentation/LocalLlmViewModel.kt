package com.tishukoff.feature.localllm.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tishukoff.feature.localllm.impl.data.remote.OllamaGenerateClient
import com.tishukoff.feature.localllm.impl.domain.model.BenchmarkEvent
import com.tishukoff.feature.localllm.impl.domain.model.LlmConfig
import com.tishukoff.feature.localllm.impl.domain.model.LocalLlmMessage
import com.tishukoff.feature.localllm.impl.domain.usecase.CheckServerHealthUseCase
import com.tishukoff.feature.localllm.impl.domain.usecase.RunBenchmarkUseCase
import com.tishukoff.feature.localllm.impl.domain.usecase.RunStabilityTestUseCase
import com.tishukoff.feature.localllm.impl.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LocalLlmViewModel internal constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val runBenchmarkUseCase: RunBenchmarkUseCase,
    private val checkServerHealthUseCase: CheckServerHealthUseCase,
    private val runStabilityTestUseCase: RunStabilityTestUseCase,
    private val ollamaClient: OllamaGenerateClient,
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
            is LocalLlmIntent.ToggleSettings -> {
                _uiState.update { it.copy(isSettingsExpanded = !it.isSettingsExpanded) }
            }
            is LocalLlmIntent.UpdateTemperature -> updateConfig { copy(temperature = intent.value) }
            is LocalLlmIntent.UpdateMaxTokens -> updateConfig { copy(maxTokens = intent.value) }
            is LocalLlmIntent.UpdateContextWindow -> updateConfig { copy(contextWindow = intent.value) }
            is LocalLlmIntent.UpdateRepeatPenalty -> updateConfig { copy(repeatPenalty = intent.value) }
            is LocalLlmIntent.UpdateTopP -> updateConfig { copy(topP = intent.value) }
            is LocalLlmIntent.UpdateTopK -> updateConfig { copy(topK = intent.value) }
            is LocalLlmIntent.UpdateSystemPrompt -> updateConfig { copy(systemPrompt = intent.value) }
            is LocalLlmIntent.ApplyPreset -> {
                _uiState.update { it.copy(config = intent.config) }
            }
            is LocalLlmIntent.RunBenchmark -> runBenchmark()
            is LocalLlmIntent.DismissBenchmark -> {
                _uiState.update {
                    it.copy(benchmarkResult = null, benchmarkLiveEntries = emptyList())
                }
            }
            is LocalLlmIntent.ToggleServerStatus -> {
                _uiState.update {
                    it.copy(isServerStatusExpanded = !it.isServerStatusExpanded)
                }
            }
            is LocalLlmIntent.CheckHealth -> checkHealth()
            is LocalLlmIntent.RunStabilityTest -> runStabilityTest()
            is LocalLlmIntent.UpdateServerIp -> {
                _uiState.update { it.copy(serverIp = intent.ip) }
            }
            is LocalLlmIntent.ApplyServerIp -> applyServerIp()
        }
    }

    private fun updateConfig(transform: LlmConfig.() -> LlmConfig) {
        _uiState.update { it.copy(config = it.config.transform()) }
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
            val config = _uiState.value.config
            val result = sendMessageUseCase(_uiState.value.messages, config)
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

    private fun runBenchmark() {
        if (_uiState.value.isBenchmarkRunning) return

        _uiState.update {
            it.copy(
                isBenchmarkRunning = true,
                benchmarkLiveEntries = emptyList(),
                benchmarkCurrentQuestion = "",
                benchmarkCurrentConfig = "",
                benchmarkQuestionIndex = 0,
                benchmarkTotalQuestions = 0,
                benchmarkResult = null,
                error = null,
            )
        }

        viewModelScope.launch {
            runBenchmarkUseCase(
                defaultConfig = LlmConfig.DEFAULT,
                optimizedConfig = _uiState.value.config,
            ).catch { error ->
                _uiState.update {
                    it.copy(
                        isBenchmarkRunning = false,
                        error = "Benchmark error: ${error.message}",
                    )
                }
            }.collect { event ->
                when (event) {
                    is BenchmarkEvent.AskingQuestion -> {
                        _uiState.update {
                            it.copy(
                                benchmarkCurrentQuestion = event.question,
                                benchmarkCurrentConfig = event.configLabel,
                                benchmarkQuestionIndex = event.questionIndex,
                                benchmarkTotalQuestions = event.totalQuestions,
                            )
                        }
                    }
                    is BenchmarkEvent.EntryCompleted -> {
                        _uiState.update {
                            it.copy(
                                benchmarkLiveEntries = it.benchmarkLiveEntries + event.entry,
                            )
                        }
                    }
                    is BenchmarkEvent.SuiteCompleted -> {
                        _uiState.update {
                            it.copy(benchmarkCurrentQuestion = "")
                        }
                    }
                    is BenchmarkEvent.Finished -> {
                        _uiState.update {
                            it.copy(
                                isBenchmarkRunning = false,
                                benchmarkCurrentQuestion = "",
                                benchmarkResult = event.comparison,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkHealth() {
        if (_uiState.value.isCheckingHealth) return

        _uiState.update { it.copy(isCheckingHealth = true) }

        viewModelScope.launch {
            val result = checkServerHealthUseCase()
            result.fold(
                onSuccess = { status ->
                    _uiState.update {
                        it.copy(
                            serverStatus = status,
                            isCheckingHealth = false,
                            requestsInLastMinute = ollamaClient.getRequestsInLastMinute(),
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isCheckingHealth = false,
                            error = "Health check failed: ${error.message}",
                        )
                    }
                },
            )
        }
    }

    private fun runStabilityTest() {
        if (_uiState.value.isStabilityTestRunning) return

        _uiState.update { it.copy(isStabilityTestRunning = true, stabilityTestResult = null) }

        viewModelScope.launch {
            val result = runStabilityTestUseCase(
                concurrentRequests = _uiState.value.rateLimitConfig.maxConcurrentRequests + 3,
                config = _uiState.value.config,
            )
            result.fold(
                onSuccess = { testResult ->
                    _uiState.update {
                        it.copy(
                            stabilityTestResult = testResult,
                            isStabilityTestRunning = false,
                            requestsInLastMinute = ollamaClient.getRequestsInLastMinute(),
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isStabilityTestRunning = false,
                            error = "Stability test failed: ${error.message}",
                        )
                    }
                },
            )
        }
    }

    private fun applyServerIp() {
        val newUrl = _uiState.value.serverIp.trim()
        if (newUrl.isBlank()) return

        viewModelScope.launch {
            ollamaClient.updateBaseUrl(newUrl)
            _uiState.update { it.copy(serverStatus = null, stabilityTestResult = null) }
            checkHealth()
        }
    }
}
