package com.tishukoff.feature.agent.impl

import com.tishukoff.core.database.api.ChatHistoryStorage
import com.tishukoff.core.database.api.ChatMessageRecord
import com.tishukoff.core.database.api.ChatStorage
import com.tishukoff.feature.agent.api.Agent
import com.tishukoff.feature.agent.api.ChatMessage
import com.tishukoff.feature.agent.api.LlmSettings
import com.tishukoff.feature.agent.api.ResponseMetadata
import com.tishukoff.feature.agent.api.TokenStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

internal class ClaudeAgent(
    private val apiKey: String,
    private val settingsRepository: SettingsRepository,
    private val chatHistoryStorage: ChatHistoryStorage,
    private val chatStorage: ChatStorage,
) : Agent {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _currentChatId = MutableStateFlow<Long?>(null)
    override val currentChatId: Flow<Long?> = _currentChatId.asStateFlow()

    private val _tokenStats = MutableStateFlow(TokenStats())
    override val tokenStats: Flow<TokenStats> = _tokenStats.asStateFlow()

    @Suppress("OPT_IN_USAGE")
    override val conversationHistory: Flow<List<ChatMessage>> =
        _currentChatId.flatMapLatest { chatId ->
            if (chatId == null) {
                flowOf(emptyList())
            } else {
                chatHistoryStorage.getByChatId(chatId).map { records ->
                    records.map { it.toChatMessage() }
                }
            }
        }

    override var settings: LlmSettings = settingsRepository.load()
        private set

    override fun updateSettings(newSettings: LlmSettings) {
        settings = newSettings
        settingsRepository.save(newSettings)
    }

    override suspend fun addUserMessage(text: String): ChatMessage {
        val chatId = getOrCreateChatId(text)
        val message = ChatMessage(text = text, isUser = true)
        chatHistoryStorage.insert(chatId, message.toRecord())
        return message
    }

    override suspend fun processRequest(): ChatMessage {
        val chatId = _currentChatId.value ?: error("No active chat")
        return try {
            val (text, metadata) = callApi(chatId)
            updateTokenStats(metadata)
            val metadataText = formatMetadata(metadata)
            val response = ChatMessage(
                text = text,
                isUser = false,
                metadataText = metadataText,
            )
            chatHistoryStorage.insert(chatId, response.toRecord())
            response
        } catch (e: Exception) {
            val errorMessage = ChatMessage(text = "Error: ${e.message}", isUser = false)
            chatHistoryStorage.insert(chatId, errorMessage.toRecord())
            errorMessage
        }
    }

    override suspend fun clearHistory() {
        val chatId = _currentChatId.value ?: return
        chatStorage.deleteChat(chatId)
        _currentChatId.value = null
        _tokenStats.value = TokenStats()
    }

    override suspend fun selectChat(chatId: Long) {
        _currentChatId.value = chatId
        _tokenStats.value = TokenStats(contextWindow = settings.model.contextWindow)
    }

    override fun startNewChat() {
        _currentChatId.value = null
        _tokenStats.value = TokenStats()
    }

    private suspend fun getOrCreateChatId(firstMessageText: String): Long {
        _currentChatId.value?.let { return it }
        val title = firstMessageText.take(40)
        val chatId = chatStorage.createChat(title)
        _currentChatId.value = chatId
        return chatId
    }

    private suspend fun callApi(chatId: Long): Pair<String, ResponseMetadata> {
        return withContext(Dispatchers.IO) {
            val model = settings.model
            val currentMessages = chatHistoryStorage.getByChatIdOnce(chatId)
            val messagesArray = JSONArray().apply {
                for (msg in currentMessages) {
                    put(JSONObject().apply {
                        put("role", if (msg.isUser) "user" else "assistant")
                        put("content", msg.text)
                    })
                }
            }

            val jsonBody = JSONObject().apply {
                put("model", model.apiId)
                put("max_tokens", settings.maxTokens)
                put("temperature", settings.temperature.toDouble())
                if (settings.systemPrompt.isNotBlank()) {
                    put("system", settings.systemPrompt)
                }
                put("messages", messagesArray)
                if (settings.stopSequences.isNotEmpty()) {
                    put("stop_sequences", JSONArray(settings.stopSequences))
                }
            }

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val startTime = System.currentTimeMillis()
            val response = client.newCall(request).execute()
            val elapsed = System.currentTimeMillis() - startTime
            val body = response.body?.string() ?: "Empty response"

            if (response.isSuccessful) {
                val json = JSONObject(body)
                val content = json.getJSONArray("content")
                val text = if (content.length() > 0) {
                    content.getJSONObject(0).getString("text")
                } else {
                    "Empty response from Claude"
                }

                val usage = json.getJSONObject("usage")
                val inputTokens = usage.getInt("input_tokens")
                val outputTokens = usage.getInt("output_tokens")
                val cost = inputTokens * model.inputPricePerMillion / 1_000_000 +
                        outputTokens * model.outputPricePerMillion / 1_000_000

                val metadata = ResponseMetadata(
                    modelId = model.apiId,
                    inputTokens = inputTokens,
                    outputTokens = outputTokens,
                    responseTimeMs = elapsed,
                    costUsd = cost,
                )

                Pair(text, metadata)
            } else {
                error("Error ${response.code}: $body")
            }
        }
    }

    private fun updateTokenStats(metadata: ResponseMetadata) {
        val current = _tokenStats.value
        _tokenStats.value = current.copy(
            totalInputTokens = current.totalInputTokens + metadata.inputTokens,
            totalOutputTokens = current.totalOutputTokens + metadata.outputTokens,
            totalCostUsd = current.totalCostUsd + metadata.costUsd,
            requestCount = current.requestCount + 1,
            contextWindow = settings.model.contextWindow,
            lastRequestInputTokens = metadata.inputTokens,
            lastRequestOutputTokens = metadata.outputTokens,
            lastRequestCostUsd = metadata.costUsd,
        )
    }

    private fun formatMetadata(metadata: ResponseMetadata): String {
        val timeSec = metadata.responseTimeMs / 1000.0
        val cost = String.format(Locale.US, "%.4f", metadata.costUsd)
        val time = String.format(Locale.US, "%.1f", timeSec)
        val stats = _tokenStats.value
        val contextPercent = String.format(Locale.US, "%.1f", stats.contextUsagePercent)
        return "${metadata.modelId} | in: ${metadata.inputTokens} out: ${metadata.outputTokens} | ${time}s | \$$cost | ctx: $contextPercent%"
    }
}

private fun ChatMessageRecord.toChatMessage() = ChatMessage(
    text = text,
    isUser = isUser,
    metadataText = metadataText,
)

private fun ChatMessage.toRecord() = ChatMessageRecord(
    text = text,
    isUser = isUser,
    metadataText = metadataText,
)
