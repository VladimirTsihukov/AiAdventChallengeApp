package com.tishukoff.feature.rag.impl.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tishukoff.feature.rag.impl.domain.model.ChunkingStrategy
import com.tishukoff.feature.rag.impl.domain.model.RagDocument
import com.tishukoff.feature.rag.impl.domain.repository.RagRepository
import com.tishukoff.feature.rag.impl.domain.usecase.IndexDocumentsUseCase
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
            is RagIntent.SwitchStrategy -> switchStrategy(intent.strategy)
            is RagIntent.DismissError -> _uiState.update { it.copy(error = null) }
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

    private fun switchStrategy(strategy: ChunkingStrategy) {
        _uiState.update { it.copy(currentStrategy = strategy) }
    }

    private fun addMessage(message: RagChatMessage, strategy: ChunkingStrategy? = null) {
        val targetStrategy = strategy ?: _uiState.value.currentStrategy
        _uiState.update {
            when (targetStrategy) {
                ChunkingStrategy.FIXED_SIZE -> it.copy(
                    fixedSizeMessages = it.fixedSizeMessages + message,
                )
                ChunkingStrategy.STRUCTURAL -> it.copy(
                    structuralMessages = it.structuralMessages + message,
                )
            }
        }
    }

    private fun addMessageToBoth(message: RagChatMessage) {
        _uiState.update {
            it.copy(
                fixedSizeMessages = it.fixedSizeMessages + message,
                structuralMessages = it.structuralMessages + message,
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

            addMessageToBoth(
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

        val strategy = _uiState.value.currentStrategy

        _uiState.update { it.copy(input = "", isLoading = true) }
        addMessage(RagChatMessage(text = query, isUser = true))

        viewModelScope.launch {
            searchDocumentsUseCase(query, strategy)
                .onSuccess { results ->
                    if (results.isEmpty()) {
                        addMessage(
                            RagChatMessage(
                                text = "Не найдено релевантных документов. Попробуйте сначала выполнить индексацию.",
                                isUser = false,
                            )
                        )
                        _uiState.update { it.copy(isLoading = false) }
                        return@launch
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

                    val answer = generateAnswer(query, contextText)

                    addMessage(
                        RagChatMessage(
                            text = answer,
                            isUser = false,
                            sources = sources,
                        )
                    )
                    _uiState.update { it.copy(isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Ошибка поиска: ${error.message}",
                        )
                    }
                }
        }
    }

    private suspend fun generateAnswer(query: String, context: String): String {
        return try {
            val requestBody = buildJsonObject {
                put("model", "claude-sonnet-4-20250514")
                put("max_tokens", 1024)
                put("messages", buildJsonArray {
                    add(buildJsonObject {
                        put("role", "user")
                        put("content", buildString {
                            append("Ты — помощник, который отвечает на вопросы строго на основе предоставленного контекста.\n")
                            append("Если ответа нет в контексте, скажи об этом.\n")
                            append("Ссылайся на источники в ответе.\n\n")
                            append("Контекст:\n$context\n\n")
                            append("Вопрос: $query")
                        })
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
