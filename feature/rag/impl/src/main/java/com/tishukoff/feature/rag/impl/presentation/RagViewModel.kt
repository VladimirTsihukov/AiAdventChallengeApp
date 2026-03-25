package com.tishukoff.feature.rag.impl.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tishukoff.feature.rag.impl.domain.model.BenchmarkQuestion
import com.tishukoff.feature.rag.impl.domain.model.BenchmarkQuestionResult
import com.tishukoff.feature.rag.impl.domain.model.BenchmarkResult
import com.tishukoff.feature.rag.impl.domain.model.BenchmarkScenarioStep
import com.tishukoff.feature.rag.impl.domain.model.ChunkingStrategy
import com.tishukoff.feature.rag.impl.domain.model.RagDocument
import com.tishukoff.feature.rag.impl.domain.model.RagMode
import com.tishukoff.feature.rag.impl.domain.model.RagSearchConfig
import com.tishukoff.feature.rag.impl.domain.model.TaskState
import com.tishukoff.feature.rag.impl.domain.model.benchmarkQuestions
import com.tishukoff.feature.rag.impl.domain.model.benchmarkScenario
import com.tishukoff.feature.rag.impl.domain.model.isComparison
import com.tishukoff.feature.rag.impl.domain.model.isLocal
import com.tishukoff.feature.rag.impl.domain.model.isReranked
import com.tishukoff.feature.rag.impl.domain.model.toChunkingStrategy
import com.tishukoff.feature.rag.impl.domain.repository.LlmClient
import com.tishukoff.feature.rag.impl.domain.repository.RagRepository
import com.tishukoff.feature.rag.impl.domain.usecase.ExtractTaskStateUseCase
import com.tishukoff.feature.rag.impl.domain.usecase.IndexDocumentsUseCase
import com.tishukoff.feature.rag.impl.domain.usecase.RerankChunksUseCase
import com.tishukoff.feature.rag.impl.domain.usecase.RewriteQueryUseCase
import com.tishukoff.feature.rag.impl.domain.usecase.SearchDocumentsUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal class RagViewModel(
    private val context: Context,
    private val indexDocumentsUseCase: IndexDocumentsUseCase,
    private val searchDocumentsUseCase: SearchDocumentsUseCase,
    private val ragRepository: RagRepository,
    private val cloudLlmClient: LlmClient,
    private val localLlmClient: LlmClient,
    private val rewriteQueryUseCase: RewriteQueryUseCase,
    private val rerankChunksUseCase: RerankChunksUseCase,
    private val extractTaskStateUseCase: ExtractTaskStateUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RagUiState())
    val uiState: StateFlow<RagUiState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    private fun RagMode.selectLlmClient(): LlmClient =
        if (isLocal) localLlmClient else cloudLlmClient

    init {
        loadChunkCounts()
    }

    fun handleIntent(intent: RagIntent) {
        when (intent) {
            is RagIntent.UpdateInput -> _uiState.update { it.copy(input = intent.text) }
            is RagIntent.SendMessage -> sendMessage()
            is RagIntent.IndexDocuments -> indexDocuments()
            is RagIntent.SwitchMode -> switchMode(intent.mode)
            is RagIntent.DismissError -> _uiState.update { it.copy(error = null) }
            is RagIntent.RunBenchmark -> runBenchmark()
            is RagIntent.DismissBenchmarkResult -> _uiState.update { it.copy(benchmarkResult = null) }
            is RagIntent.UpdateSimilarityThreshold -> _uiState.update { it.copy(similarityThreshold = intent.value) }
            is RagIntent.UpdateInitialTopK -> _uiState.update { it.copy(initialTopK = intent.value) }
            is RagIntent.UpdateFinalTopK -> _uiState.update { it.copy(finalTopK = intent.value) }
            is RagIntent.ClearTaskState -> _uiState.update {
                it.copy(taskState = TaskState(), memoryRagMessages = emptyList())
            }
            is RagIntent.RunScenarioBenchmark -> runScenarioBenchmark()
        }
    }

    private fun loadChunkCounts() {
        viewModelScope.launch {
            val fixedCount = ragRepository.getIndexedChunkCount(ChunkingStrategy.FIXED_SIZE)
            val structuralCount = ragRepository.getIndexedChunkCount(ChunkingStrategy.STRUCTURAL)
            _uiState.update {
                it.copy(
                    fixedSizeChunkCount = fixedCount,
                    structuralChunkCount = structuralCount,
                )
            }
        }
    }

    private fun switchMode(mode: RagMode) {
        _uiState.update { it.copy(currentMode = mode) }
    }

    private fun addMessage(message: RagChatMessage, mode: RagMode? = null) {
        val targetMode = mode ?: _uiState.value.currentMode
        _uiState.update {
            when (targetMode) {
                RagMode.FIXED_SIZE -> it.copy(
                    fixedSizeMessages = it.fixedSizeMessages + message,
                )
                RagMode.STRUCTURAL -> it.copy(
                    structuralMessages = it.structuralMessages + message,
                )
                RagMode.NO_RAG -> it.copy(
                    noRagMessages = it.noRagMessages + message,
                )
                RagMode.FIXED_RERANKED -> it.copy(
                    fixedRerankedMessages = it.fixedRerankedMessages + message,
                )
                RagMode.STRUCTURAL_RERANKED -> it.copy(
                    structuralRerankedMessages = it.structuralRerankedMessages + message,
                )
                RagMode.MEMORY_RAG -> it.copy(
                    memoryRagMessages = it.memoryRagMessages + message,
                )
                RagMode.LOCAL_FIXED -> it.copy(
                    localFixedMessages = it.localFixedMessages + message,
                )
                RagMode.LOCAL_STRUCTURAL -> it.copy(
                    localStructuralMessages = it.localStructuralMessages + message,
                )
                RagMode.LOCAL_NO_RAG -> it.copy(
                    localNoRagMessages = it.localNoRagMessages + message,
                )
                RagMode.COMPARISON -> it
            }
        }
    }

    private fun addMessageToRagModes(message: RagChatMessage) {
        _uiState.update {
            it.copy(
                fixedSizeMessages = it.fixedSizeMessages + message,
                structuralMessages = it.structuralMessages + message,
                fixedRerankedMessages = it.fixedRerankedMessages + message,
                structuralRerankedMessages = it.structuralRerankedMessages + message,
                memoryRagMessages = it.memoryRagMessages + message,
                localFixedMessages = it.localFixedMessages + message,
                localStructuralMessages = it.localStructuralMessages + message,
            )
        }
    }

    private fun indexDocuments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isIndexing = true, indexingProgress = "Загрузка документов...") }

            val documents = loadDocumentsFromAssets()

            for (strategy in ChunkingStrategy.entries) {
                _uiState.update {
                    it.copy(indexingProgress = "Индексация (${strategy.name})...")
                }

                indexDocumentsUseCase(documents, strategy)
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isIndexing = false,
                                error = "Ошибка индексации: ${error.message}",
                            )
                        }
                        return@launch
                    }
            }

            val fixedCount = ragRepository.getIndexedChunkCount(ChunkingStrategy.FIXED_SIZE)
            val structuralCount = ragRepository.getIndexedChunkCount(ChunkingStrategy.STRUCTURAL)

            _uiState.update {
                it.copy(
                    isIndexing = false,
                    indexingProgress = "",
                    fixedSizeChunkCount = fixedCount,
                    structuralChunkCount = structuralCount,
                )
            }

            addMessageToRagModes(
                RagChatMessage(
                    text = "Индексация завершена!\n" +
                        "Fixed-size: $fixedCount чанков\n" +
                        "Structural: $structuralCount чанков",
                    isUser = false,
                )
            )
        }
    }

    private fun sendMessage() {
        val query = _uiState.value.input.trim()
        if (query.isBlank()) return

        val mode = _uiState.value.currentMode

        _uiState.update { it.copy(input = "", isLoading = true) }

        if (mode.isComparison) {
            sendComparisonMessage(query)
        } else {
            addMessage(RagChatMessage(text = query, isUser = true))
            viewModelScope.launch {
                val answer = generateAnswerForMode(query, mode)
                addMessage(answer)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun sendComparisonMessage(query: String) {
        viewModelScope.launch {
            val strategy = ChunkingStrategy.STRUCTURAL

            val entry = ComparisonEntry(query = query)
            _uiState.update { it.copy(comparisonEntries = it.comparisonEntries + entry) }

            try {
                coroutineScope {
                    val cloudDeferred = async {
                        val start = System.currentTimeMillis()
                        val answer = generateRagAnswer(query, strategy, cloudLlmClient)
                        val duration = System.currentTimeMillis() - start
                        answer.copy(durationMs = duration, modelLabel = "Cloud (Claude)")
                    }
                    val localDeferred = async {
                        val start = System.currentTimeMillis()
                        val answer = generateLocalRagAnswer(query, strategy)
                        val duration = System.currentTimeMillis() - start
                        answer.copy(durationMs = duration, modelLabel = "Local (phi3:mini)")
                    }

                    val cloudAnswer = cloudDeferred.await()
                    val localAnswer = localDeferred.await()

                    _uiState.update { state ->
                        val entries = state.comparisonEntries.toMutableList()
                        val idx = entries.indexOfLast { it.query == query && it.cloudAnswer == null }
                        if (idx >= 0) {
                            entries[idx] = entries[idx].copy(
                                cloudAnswer = cloudAnswer,
                                localAnswer = localAnswer,
                            )
                        }
                        state.copy(comparisonEntries = entries)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Ошибка сравнения: ${e.message}") }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun generateAnswerForMode(query: String, mode: RagMode): RagChatMessage {
        if (mode == RagMode.MEMORY_RAG) {
            return generateMemoryRagAnswer(query)
        }

        val llmClient = mode.selectLlmClient()
        val chunkingStrategy = mode.toChunkingStrategy()
            ?: return generateNoRagAnswer(query, llmClient)

        if (mode.isReranked) {
            val state = _uiState.value
            val config = RagSearchConfig(
                initialTopK = state.initialTopK,
                finalTopK = state.finalTopK,
                similarityThreshold = state.similarityThreshold,
            )
            return generateRerankedRagAnswer(query, chunkingStrategy, config)
        }

        if (mode.isLocal) {
            return generateLocalRagAnswer(query, chunkingStrategy)
        }

        return generateRagAnswer(query, chunkingStrategy, llmClient)
    }

    private suspend fun generateRerankedRagAnswer(
        query: String,
        strategy: ChunkingStrategy,
        config: RagSearchConfig,
    ): RagChatMessage {
        val rewrittenQuery = rewriteQueryUseCase(query).getOrDefault(query)

        val searchResult = searchDocumentsUseCase(rewrittenQuery, strategy, config.initialTopK)

        return searchResult.fold(
            onSuccess = { results ->
                if (results.isEmpty()) {
                    return RagChatMessage(
                        text = "Не найдено релевантных документов. Попробуйте сначала выполнить индексацию.",
                        isUser = false,
                    )
                }

                val filtered = results.filter { it.score >= config.similarityThreshold }
                if (filtered.isEmpty()) {
                    return RagChatMessage(
                        text = "Недостаточно данных для точного ответа. " +
                            "Пожалуйста, уточните вопрос или переформулируйте.",
                        isUser = false,
                    )
                }

                val reranked = rerankChunksUseCase(rewrittenQuery, filtered, config.finalTopK)
                    .getOrDefault(filtered.take(config.finalTopK))

                val contextText = reranked.joinToString("\n\n---\n\n") { result ->
                    "[Источник: ${result.chunk.metadata.source}, " +
                        "Раздел: ${result.chunk.metadata.section}, " +
                        "Релевантность: ${"%.2f".format(result.score)}]\n${result.chunk.text}"
                }

                val sources = reranked.map { result ->
                    SourceInfo(
                        fileName = result.chunk.metadata.source,
                        section = "[reranked] ${result.chunk.metadata.section}",
                        score = result.score,
                        chunkPreview = result.chunk.text.take(100) + "...",
                    )
                }

                val header = if (rewrittenQuery != query) {
                    "Переписанный запрос: $rewrittenQuery\n\n"
                } else {
                    ""
                }

                val rawAnswer = generateRagLlmAnswer(rewrittenQuery, contextText)
                val parsed = parseLlmRagResponse(rawAnswer)
                RagChatMessage(
                    text = header + parsed.answer,
                    isUser = false,
                    sources = sources,
                    quotes = parsed.quotes,
                )
            },
            onFailure = { error ->
                RagChatMessage(text = "Ошибка поиска: ${error.message}", isUser = false)
            },
        )
    }

    private suspend fun generateRagAnswer(
        query: String,
        strategy: ChunkingStrategy,
        llmClient: LlmClient = cloudLlmClient,
    ): RagChatMessage {
        val searchResult = searchDocumentsUseCase(query, strategy)

        return searchResult.fold(
            onSuccess = { results ->
                if (results.isEmpty()) {
                    return RagChatMessage(
                        text = "Не найдено релевантных документов. Попробуйте сначала выполнить индексацию.",
                        isUser = false,
                    )
                }

                val contextText = results.joinToString("\n\n---\n\n") { result ->
                    "[Источник: ${result.chunk.metadata.source}, " +
                        "Раздел: ${result.chunk.metadata.section}, " +
                        "Релевантность: ${"%.2f".format(result.score)}]\n${result.chunk.text}"
                }

                val sources = results.map { result ->
                    SourceInfo(
                        fileName = result.chunk.metadata.source,
                        section = result.chunk.metadata.section,
                        score = result.score,
                        chunkPreview = result.chunk.text.take(100) + "...",
                    )
                }

                val rawAnswer = generateRagLlmAnswer(query, contextText, llmClient)
                val parsed = parseLlmRagResponse(rawAnswer)
                RagChatMessage(
                    text = parsed.answer,
                    isUser = false,
                    sources = sources,
                    quotes = parsed.quotes,
                )
            },
            onFailure = { error ->
                RagChatMessage(text = "Ошибка поиска: ${error.message}", isUser = false)
            },
        )
    }

    private suspend fun generateLocalRagAnswer(
        query: String,
        strategy: ChunkingStrategy,
    ): RagChatMessage {
        val searchResult = searchDocumentsUseCase(query, strategy)

        return searchResult.fold(
            onSuccess = { results ->
                if (results.isEmpty()) {
                    return RagChatMessage(
                        text = "Не найдено релевантных документов. Попробуйте сначала выполнить индексацию.",
                        isUser = false,
                    )
                }

                val contextText = results.joinToString("\n\n---\n\n") { result ->
                    "[Источник: ${result.chunk.metadata.source}, " +
                        "Раздел: ${result.chunk.metadata.section}]\n${result.chunk.text}"
                }

                val sources = results.map { result ->
                    SourceInfo(
                        fileName = result.chunk.metadata.source,
                        section = result.chunk.metadata.section,
                        score = result.score,
                        chunkPreview = result.chunk.text.take(100) + "...",
                    )
                }

                val prompt = buildString {
                    append("You are a helpful assistant that answers questions based on the provided context.\n")
                    append("If the context does not contain the answer, say so honestly.\n")
                    append("Always mention the source documents.\n\n")
                    append("Context:\n$contextText\n\n")
                    append("Question: $query\n\n")
                    append("Answer:")
                }

                val rawAnswer = try {
                    localLlmClient.complete(prompt)
                } catch (e: Exception) {
                    "Ошибка локальной LLM: ${e.message}"
                }

                RagChatMessage(
                    text = rawAnswer,
                    isUser = false,
                    sources = sources,
                )
            },
            onFailure = { error ->
                RagChatMessage(text = "Ошибка поиска: ${error.message}", isUser = false)
            },
        )
    }

    private suspend fun generateMemoryRagAnswer(query: String): RagChatMessage {
        val state = _uiState.value
        val strategy = ChunkingStrategy.STRUCTURAL
        val config = RagSearchConfig(
            initialTopK = state.initialTopK,
            finalTopK = state.finalTopK,
            similarityThreshold = state.similarityThreshold,
        )

        val rewrittenQuery = rewriteQueryUseCase(query).getOrDefault(query)

        val searchResult = searchDocumentsUseCase(rewrittenQuery, strategy, config.initialTopK)

        return searchResult.fold(
            onSuccess = { results ->
                if (results.isEmpty()) {
                    return RagChatMessage(
                        text = "Не найдено релевантных документов. Попробуйте сначала выполнить индексацию.",
                        isUser = false,
                    )
                }

                val filtered = results.filter { it.score >= config.similarityThreshold }
                val reranked = if (filtered.isNotEmpty()) {
                    rerankChunksUseCase(rewrittenQuery, filtered, config.finalTopK)
                        .getOrDefault(filtered.take(config.finalTopK))
                } else {
                    results.take(config.finalTopK)
                }

                val contextText = reranked.joinToString("\n\n---\n\n") { result ->
                    "[Источник: ${result.chunk.metadata.source}, " +
                        "Раздел: ${result.chunk.metadata.section}, " +
                        "Релевантность: ${"%.2f".format(result.score)}]\n${result.chunk.text}"
                }

                val sources = reranked.map { result ->
                    SourceInfo(
                        fileName = result.chunk.metadata.source,
                        section = result.chunk.metadata.section,
                        score = result.score,
                        chunkPreview = result.chunk.text.take(100) + "...",
                    )
                }

                val conversationHistory = buildConversationHistory(state.memoryRagMessages)
                val taskStateBlock = state.taskState.toPromptBlock()

                val systemPrompt = buildString {
                    append("Ты — помощник, который ведёт связный диалог и отвечает на вопросы ")
                    append("строго на основе предоставленного контекста из документов.\n\n")
                    append("Правила:\n")
                    append("- Всегда отвечай с учётом предыдущих сообщений диалога\n")
                    append("- Используй память задачи для понимания цели пользователя\n")
                    append("- Если контекст не содержит ответа, честно скажи об этом\n")
                    append("- Всегда указывай источники информации\n\n")
                    if (taskStateBlock.isNotBlank()) {
                        append(taskStateBlock)
                        append("\n")
                    }
                    append("Контекст из документов:\n$contextText\n\n")
                    append("Ты ОБЯЗАН вернуть ответ строго в формате JSON (без markdown-обёртки):\n")
                    append("{\n")
                    append("  \"answer\": \"<текст ответа>\",\n")
                    append("  \"sources\": [{\"file\": \"<имя файла>\", \"section\": \"<раздел>\"}],\n")
                    append("  \"quotes\": [\"<дословная цитата из контекста>\"]\n")
                    append("}\n")
                }

                val messages = buildMessagesWithHistory(conversationHistory, systemPrompt, query)
                val rawAnswer = try {
                    cloudLlmClient.chat(messages, systemPrompt)
                } catch (e: Exception) {
                    "Ошибка генерации ответа: ${e.message}"
                }
                val parsed = parseLlmRagResponse(rawAnswer)

                updateTaskState(state.memoryRagMessages, query, parsed.answer)

                val header = if (rewrittenQuery != query) {
                    "Переписанный запрос: $rewrittenQuery\n\n"
                } else {
                    ""
                }

                RagChatMessage(
                    text = header + parsed.answer,
                    isUser = false,
                    sources = sources,
                    quotes = parsed.quotes,
                )
            },
            onFailure = { error ->
                RagChatMessage(text = "Ошибка поиска: ${error.message}", isUser = false)
            },
        )
    }

    private fun buildConversationHistory(messages: List<RagChatMessage>): List<Pair<String, String>> {
        return messages.map { msg ->
            val role = if (msg.isUser) "user" else "assistant"
            role to msg.text
        }
    }

    private fun buildMessagesWithHistory(
        history: List<Pair<String, String>>,
        systemPrompt: String,
        currentQuery: String,
    ): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        // Берём последние 20 сообщений для ограничения контекста
        val recentHistory = history.takeLast(20)
        result.addAll(recentHistory)
        result.add("user" to currentQuery)
        return result
    }

    private fun updateTaskState(
        messages: List<RagChatMessage>,
        currentQuery: String,
        currentAnswer: String,
    ) {
        viewModelScope.launch {
            val history = buildString {
                messages.forEach { msg ->
                    val role = if (msg.isUser) "User" else "Assistant"
                    appendLine("$role: ${msg.text}")
                }
                appendLine("User: $currentQuery")
                appendLine("Assistant: $currentAnswer")
            }
            val currentState = _uiState.value.taskState
            extractTaskStateUseCase(history, currentState)
                .onSuccess { newState ->
                    _uiState.update { it.copy(taskState = newState) }
                }
        }
    }

    private fun runScenarioBenchmark() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBenchmarkRunning = true,
                    benchmarkResult = null,
                    benchmarkProgress = "",
                    taskState = TaskState(),
                    memoryRagMessages = emptyList(),
                )
            }

            val allResults = mutableListOf<BenchmarkQuestionResult>()
            val scenario = benchmarkScenario

            addMessage(
                RagChatMessage(
                    text = "=== Сценарий: ${scenario.name} ===",
                    isUser = false,
                ),
                RagMode.MEMORY_RAG,
            )

            for ((stepIndex, step) in scenario.steps.withIndex()) {
                _uiState.update {
                    it.copy(
                        benchmarkProgress = "Шаг ${stepIndex + 1}/${scenario.steps.size}",
                    )
                }

                addMessage(
                    RagChatMessage(text = step.question, isUser = true),
                    RagMode.MEMORY_RAG,
                )

                val answerMessage = generateMemoryRagAnswer(step.question)
                addMessage(answerMessage, RagMode.MEMORY_RAG)

                val result = evaluateScenarioStep(step, answerMessage)
                allResults.add(result)
            }

            val benchmarkResult = BenchmarkResult(allResults)
            val summaryText = buildBenchmarkSummary(benchmarkResult)
            addMessage(RagChatMessage(text = summaryText, isUser = false), RagMode.MEMORY_RAG)

            _uiState.update {
                it.copy(
                    isBenchmarkRunning = false,
                    benchmarkProgress = "",
                    benchmarkResult = benchmarkResult,
                )
            }
        }
    }

    private fun evaluateScenarioStep(
        step: BenchmarkScenarioStep,
        message: RagChatMessage,
    ): BenchmarkQuestionResult {
        val lowerAnswer = message.text.lowercase()
        val foundKeywords = step.expectedKeywords.filter { keyword ->
            lowerAnswer.contains(keyword.lowercase())
        }
        return BenchmarkQuestionResult(
            question = step.question,
            answer = message.text,
            expectedKeywords = step.expectedKeywords,
            foundKeywords = foundKeywords,
            passed = foundKeywords.isNotEmpty(),
            hasSources = message.sources.isNotEmpty(),
            hasQuotes = message.quotes.isNotEmpty(),
        )
    }

    private suspend fun generateNoRagAnswer(
        query: String,
        llmClient: LlmClient = cloudLlmClient,
    ): RagChatMessage {
        val answer = generateDirectLlmAnswer(query, llmClient)
        return RagChatMessage(text = answer, isUser = false)
    }

    private fun runBenchmark() {
        val mode = _uiState.value.currentMode

        viewModelScope.launch {
            _uiState.update {
                it.copy(isBenchmarkRunning = true, benchmarkResult = null, benchmarkProgress = "")
            }

            val results = mutableListOf<BenchmarkQuestionResult>()

            for ((index, bq) in benchmarkQuestions.withIndex()) {
                _uiState.update {
                    it.copy(benchmarkProgress = "Вопрос ${index + 1}/${benchmarkQuestions.size}")
                }

                addMessage(RagChatMessage(text = bq.question, isUser = true), mode)

                val answerMessage = generateAnswerForMode(bq.question, mode)
                addMessage(answerMessage, mode)

                val result = evaluateAnswer(bq, answerMessage)
                results.add(result)
            }

            val benchmarkResult = BenchmarkResult(results)

            val summaryText = buildBenchmarkSummary(benchmarkResult)
            addMessage(RagChatMessage(text = summaryText, isUser = false), mode)

            _uiState.update {
                it.copy(
                    isBenchmarkRunning = false,
                    benchmarkProgress = "",
                    benchmarkResult = benchmarkResult,
                )
            }
        }
    }

    private fun evaluateAnswer(question: BenchmarkQuestion, message: RagChatMessage): BenchmarkQuestionResult {
        val lowerAnswer = message.text.lowercase()
        val foundKeywords = question.expectedKeywords.filter { keyword ->
            lowerAnswer.contains(keyword.lowercase())
        }
        val passed = foundKeywords.isNotEmpty()

        return BenchmarkQuestionResult(
            question = question.question,
            answer = message.text,
            expectedKeywords = question.expectedKeywords,
            foundKeywords = foundKeywords,
            passed = passed,
            hasSources = message.sources.isNotEmpty(),
            hasQuotes = message.quotes.isNotEmpty(),
        )
    }

    private fun buildBenchmarkSummary(result: BenchmarkResult): String {
        val sourcesCount = result.results.count { it.hasSources }
        val quotesCount = result.results.count { it.hasQuotes }

        return buildString {
            appendLine("=== Результат бенчмарка ===")
            appendLine("Пройдено: ${result.passedCount}/${result.totalCount}")
            appendLine("Источники: $sourcesCount/${result.totalCount}")
            appendLine("Цитаты: $quotesCount/${result.totalCount}")
            appendLine()
            result.results.forEachIndexed { index, qr ->
                val status = if (qr.passed) "PASS" else "FAIL"
                appendLine("${index + 1}. [$status] ${qr.question}")
                if (!qr.passed) {
                    appendLine("   Ожидалось: ${qr.expectedKeywords.joinToString(", ")}")
                }
            }
        }
    }

    private data class LlmRagResponse(
        val answer: String,
        val sources: List<LlmSourceRef> = emptyList(),
        val quotes: List<String> = emptyList(),
    )

    private data class LlmSourceRef(
        val file: String,
        val section: String,
    )

    private fun parseLlmRagResponse(raw: String): LlmRagResponse {
        return try {
            val trimmed = raw.trim().let { text ->
                if (text.startsWith("```json")) {
                    text.removePrefix("```json").removeSuffix("```").trim()
                } else if (text.startsWith("```")) {
                    text.removePrefix("```").removeSuffix("```").trim()
                } else {
                    text
                }
            }
            val jsonObj = json.decodeFromString<JsonObject>(trimmed)
            val answer = jsonObj["answer"]?.jsonPrimitive?.content ?: raw
            val sources = jsonObj["sources"]?.jsonArray?.mapNotNull { element ->
                val obj = element.jsonObject
                val file = obj["file"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val section = obj["section"]?.jsonPrimitive?.content ?: ""
                LlmSourceRef(file = file, section = section)
            } ?: emptyList()
            val quotes = jsonObj["quotes"]?.jsonArray?.mapNotNull { element ->
                element.jsonPrimitive.content.takeIf { it.isNotBlank() }
            } ?: emptyList()
            LlmRagResponse(answer = answer, sources = sources, quotes = quotes)
        } catch (_: Exception) {
            LlmRagResponse(answer = raw)
        }
    }

    private suspend fun generateRagLlmAnswer(
        query: String,
        context: String,
        llmClient: LlmClient = cloudLlmClient,
    ): String {
        val prompt = buildString {
            append("Ты — помощник, который отвечает на вопросы строго на основе предоставленного контекста.\n")
            append("Если контекст не содержит ответа на вопрос, верни JSON с answer: \"Я не могу ответить на этот вопрос на основе имеющихся данных.\"\n\n")
            append("Ты ОБЯЗАН вернуть ответ строго в формате JSON (без markdown-обёртки):\n")
            append("{\n")
            append("  \"answer\": \"<текст ответа>\",\n")
            append("  \"sources\": [{\"file\": \"<имя файла>\", \"section\": \"<раздел>\"}],\n")
            append("  \"quotes\": [\"<дословная цитата из контекста>\"]\n")
            append("}\n\n")
            append("Правила:\n")
            append("- answer: развёрнутый ответ на вопрос\n")
            append("- sources: список источников из контекста (file и section берутся из заголовков чанков)\n")
            append("- quotes: дословные фрагменты из контекста, подтверждающие ответ\n")
            append("- Если ответа нет в контексте — answer содержит отказ, sources и quotes пустые\n\n")
            append("Контекст:\n$context\n\n")
            append("Вопрос: $query")
        }
        return try {
            llmClient.complete(prompt)
        } catch (e: Exception) {
            "Ошибка генерации ответа: ${e.message}"
        }
    }

    private suspend fun generateDirectLlmAnswer(
        query: String,
        llmClient: LlmClient = cloudLlmClient,
    ): String {
        val prompt = buildString {
            append("Ты — помощник, который отвечает на вопросы.\n")
            append("Отвечай кратко и по существу.\n\n")
            append("Вопрос: $query")
        }
        return try {
            llmClient.complete(prompt)
        } catch (e: Exception) {
            "Ошибка генерации ответа: ${e.message}"
        }
    }

    private fun loadDocumentsFromAssets(): List<RagDocument> {
        val assetManager = context.assets
        val files = assetManager.list("rag_documents") ?: return emptyList()

        return files.mapNotNull { fileName ->
            try {
                val content = assetManager.open("rag_documents/$fileName")
                    .bufferedReader()
                    .use { it.readText() }
                RagDocument(fileName = fileName, content = content)
            } catch (e: Exception) {
                null
            }
        }
    }
}
