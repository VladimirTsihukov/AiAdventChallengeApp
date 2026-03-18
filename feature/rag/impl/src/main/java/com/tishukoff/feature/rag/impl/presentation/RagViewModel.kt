package com.tishukoff.feature.rag.impl.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tishukoff.feature.rag.impl.domain.model.BenchmarkQuestion
import com.tishukoff.feature.rag.impl.domain.model.BenchmarkQuestionResult
import com.tishukoff.feature.rag.impl.domain.model.BenchmarkResult
import com.tishukoff.feature.rag.impl.domain.model.ChunkingStrategy
import com.tishukoff.feature.rag.impl.domain.model.RagDocument
import com.tishukoff.feature.rag.impl.domain.model.RagMode
import com.tishukoff.feature.rag.impl.domain.model.RagSearchConfig
import com.tishukoff.feature.rag.impl.domain.model.benchmarkQuestions
import com.tishukoff.feature.rag.impl.domain.model.isReranked
import com.tishukoff.feature.rag.impl.domain.model.toChunkingStrategy
import com.tishukoff.feature.rag.impl.domain.repository.LlmClient
import com.tishukoff.feature.rag.impl.domain.repository.RagRepository
import com.tishukoff.feature.rag.impl.domain.usecase.IndexDocumentsUseCase
import com.tishukoff.feature.rag.impl.domain.usecase.RerankChunksUseCase
import com.tishukoff.feature.rag.impl.domain.usecase.RewriteQueryUseCase
import com.tishukoff.feature.rag.impl.domain.usecase.SearchDocumentsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

internal class RagViewModel(
    private val context: Context,
    private val indexDocumentsUseCase: IndexDocumentsUseCase,
    private val searchDocumentsUseCase: SearchDocumentsUseCase,
    private val anthropicApiKey: String,
    private val ragRepository: RagRepository,
    private val llmClient: LlmClient,
    private val rewriteQueryUseCase: RewriteQueryUseCase,
    private val rerankChunksUseCase: RerankChunksUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RagUiState())
    val uiState: StateFlow<RagUiState> = _uiState.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

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
        addMessage(RagChatMessage(text = query, isUser = true))

        viewModelScope.launch {
            val answer = generateAnswerForMode(query, mode)
            addMessage(answer)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun generateAnswerForMode(query: String, mode: RagMode): RagChatMessage {
        val chunkingStrategy = mode.toChunkingStrategy()
            ?: return generateNoRagAnswer(query)

        if (mode.isReranked) {
            val state = _uiState.value
            val config = RagSearchConfig(
                initialTopK = state.initialTopK,
                finalTopK = state.finalTopK,
                similarityThreshold = state.similarityThreshold,
            )
            return generateRerankedRagAnswer(query, chunkingStrategy, config)
        }

        return generateRagAnswer(query, chunkingStrategy)
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
                        text = "Все результаты отфильтрованы по порогу релевантности " +
                            "(${config.similarityThreshold}). Попробуйте снизить порог.",
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

                val answer = generateRagLlmAnswer(rewrittenQuery, contextText)
                RagChatMessage(text = header + answer, isUser = false, sources = sources)
            },
            onFailure = { error ->
                RagChatMessage(text = "Ошибка поиска: ${error.message}", isUser = false)
            },
        )
    }

    private suspend fun generateRagAnswer(query: String, strategy: ChunkingStrategy): RagChatMessage {
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

                val answer = generateRagLlmAnswer(query, contextText)
                RagChatMessage(text = answer, isUser = false, sources = sources)
            },
            onFailure = { error ->
                RagChatMessage(text = "Ошибка поиска: ${error.message}", isUser = false)
            },
        )
    }

    private suspend fun generateNoRagAnswer(query: String): RagChatMessage {
        val answer = generateDirectLlmAnswer(query)
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

                val result = evaluateAnswer(bq, answerMessage.text)
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

    private fun evaluateAnswer(question: BenchmarkQuestion, answer: String): BenchmarkQuestionResult {
        val lowerAnswer = answer.lowercase()
        val foundKeywords = question.expectedKeywords.filter { keyword ->
            lowerAnswer.contains(keyword.lowercase())
        }
        val passed = foundKeywords.isNotEmpty()

        return BenchmarkQuestionResult(
            question = question.question,
            answer = answer,
            expectedKeywords = question.expectedKeywords,
            foundKeywords = foundKeywords,
            passed = passed,
        )
    }

    private fun buildBenchmarkSummary(result: BenchmarkResult): String {
        return buildString {
            appendLine("=== Результат бенчмарка ===")
            appendLine("Пройдено: ${result.passedCount}/${result.totalCount}")
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

    private suspend fun generateRagLlmAnswer(query: String, context: String): String {
        return callAnthropicApi(
            buildString {
                append("Ты — помощник, который отвечает на вопросы строго на основе предоставленного контекста.\n")
                append("Если ответа нет в контексте, скажи об этом.\n")
                append("Ссылайся на источники в ответе.\n\n")
                append("Контекст:\n$context\n\n")
                append("Вопрос: $query")
            }
        )
    }

    private suspend fun generateDirectLlmAnswer(query: String): String {
        return callAnthropicApi(
            buildString {
                append("Ты — помощник, который отвечает на вопросы.\n")
                append("Отвечай кратко и по существу.\n\n")
                append("Вопрос: $query")
            }
        )
    }

    private suspend fun callAnthropicApi(userMessage: String): String {
        return try {
            val requestBody = buildJsonObject {
                put("model", "claude-sonnet-4-20250514")
                put("max_tokens", 1024)
                put("messages", buildJsonArray {
                    add(buildJsonObject {
                        put("role", "user")
                        put("content", userMessage)
                    })
                })
            }.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("x-api-key", anthropicApiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(requestBody)
                .build()

            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }

            if (!response.isSuccessful) {
                return "Ошибка API: ${response.code}"
            }

            val body = response.body?.string() ?: return "Пустой ответ"
            val jsonResponse = json.decodeFromString<JsonObject>(body)
            val content = jsonResponse["content"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("text")?.jsonPrimitive?.content

            content ?: "Не удалось получить ответ"
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
