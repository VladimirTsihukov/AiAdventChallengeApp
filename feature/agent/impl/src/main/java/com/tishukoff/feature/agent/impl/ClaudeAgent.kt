package com.tishukoff.feature.agent.impl

import com.tishukoff.core.database.api.ChatHistoryStorage
import com.tishukoff.core.database.api.ChatMessageRecord
import com.tishukoff.core.database.api.ChatStorage
import com.tishukoff.core.database.api.ContextSummaryStorage
import com.tishukoff.feature.agent.api.Agent
import com.tishukoff.feature.agent.api.BranchInfo
import com.tishukoff.feature.agent.api.ChatMessage
import com.tishukoff.feature.agent.api.CompressionStats
import com.tishukoff.feature.agent.api.ContextStrategyType
import com.tishukoff.feature.agent.api.LlmSettings
import com.tishukoff.feature.agent.api.ResponseMetadata
import com.tishukoff.feature.agent.api.TokenStats
import com.tishukoff.feature.agent.api.ToolCallEntry
import com.tishukoff.feature.agent.impl.strategy.BranchingStrategy
import com.tishukoff.feature.agent.impl.strategy.ContextStrategy
import com.tishukoff.feature.agent.impl.strategy.SlidingWindowStrategy
import com.tishukoff.feature.agent.impl.strategy.StickyFactsStrategy
import com.tishukoff.feature.agent.impl.strategy.SummarizationStrategy
import com.tishukoff.feature.invariant.api.InvariantProvider
import com.tishukoff.feature.mcp.api.McpToolRouter
import com.tishukoff.feature.memory.api.MemoryManager
import com.tishukoff.feature.profile.api.ProfileProvider
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
    private val contextSummaryStorage: ContextSummaryStorage,
    private val memoryManager: MemoryManager,
    private val profileProvider: ProfileProvider,
    private val invariantProvider: InvariantProvider,
    private val toolRouter: McpToolRouter?,
) : Agent {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val contextCompressor = ContextCompressor(
        apiKey = apiKey,
        client = client,
        contextSummaryStorage = contextSummaryStorage,
    )

    private val branchingStrategy = BranchingStrategy()

    private val strategies: Map<ContextStrategyType, ContextStrategy> = mapOf(
        ContextStrategyType.SUMMARIZATION to SummarizationStrategy(contextCompressor),
        ContextStrategyType.SLIDING_WINDOW to SlidingWindowStrategy(),
        ContextStrategyType.STICKY_FACTS to StickyFactsStrategy(apiKey, client),
        ContextStrategyType.BRANCHING to branchingStrategy,
    )

    private val _currentChatId = MutableStateFlow<Long?>(null)
    override val currentChatId: Flow<Long?> = _currentChatId.asStateFlow()

    private val _tokenStats = MutableStateFlow(TokenStats())
    override val tokenStats: Flow<TokenStats> = _tokenStats.asStateFlow()

    private val _compressionStats = MutableStateFlow(CompressionStats())
    override val compressionStats: Flow<CompressionStats> = _compressionStats.asStateFlow()

    override val branches: Flow<List<BranchInfo>> = branchingStrategy.branches
    override val currentBranchId: Flow<String?> = branchingStrategy.currentBranchId

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

    private val _settings = MutableStateFlow(settingsRepository.load())
    override val settings: Flow<LlmSettings> = _settings.asStateFlow()

    private val currentSettings: LlmSettings get() = _settings.value

    override fun updateSettings(newSettings: LlmSettings) {
        _settings.value = newSettings
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
            val (text, metadata, toolCalls) = callApi(chatId)
            updateTokenStats(metadata)
            val metadataText = formatMetadata(metadata)
            val response = ChatMessage(
                text = text,
                isUser = false,
                metadataText = metadataText,
                toolCalls = toolCalls,
            )
            chatHistoryStorage.insert(chatId, response.toRecord())

            val lastUserMsg = chatHistoryStorage.getByChatIdOnce(chatId)
                .lastOrNull { it.isUser }?.text.orEmpty()
            try {
                memoryManager.processNewMessage(chatId, lastUserMsg, text)
            } catch (_: Exception) { }

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
        _tokenStats.value = TokenStats(contextWindow = currentSettings.model.contextWindow)
    }

    override fun startNewChat() {
        _currentChatId.value = null
        _tokenStats.value = TokenStats()
    }

    override fun createCheckpoint(name: String) {
        val chatId = _currentChatId.value ?: return
        val messageCount = _tokenStats.value.requestCount * 2
        branchingStrategy.createCheckpoint(chatId, name, messageCount)
    }

    override fun createBranch(checkpointId: String, name: String) {
        val chatId = _currentChatId.value ?: return
        branchingStrategy.createBranch(chatId, checkpointId, name)
    }

    override fun switchBranch(branchId: String) {
        branchingStrategy.switchBranch(branchId)
    }

    private suspend fun getOrCreateChatId(firstMessageText: String): Long {
        _currentChatId.value?.let { return it }
        val title = firstMessageText.take(40)
        val chatId = chatStorage.createChat(title)
        _currentChatId.value = chatId
        return chatId
    }

    private data class ApiResult(
        val text: String,
        val metadata: ResponseMetadata,
        val toolCalls: List<ToolCallEntry>,
    )

    private suspend fun callApi(chatId: Long): ApiResult {
        return withContext(Dispatchers.IO) {
            val settings = currentSettings
            val model = settings.model
            val currentMessages = chatHistoryStorage.getByChatIdOnce(chatId)

            val strategy = strategies[settings.contextStrategy]
            val strategyContext = strategy?.buildContext(
                chatId = chatId,
                allMessages = currentMessages,
                settings = settings,
            )

            val systemPromptText: String
            val messagesToSend: List<ChatMessageRecord>

            val profilePrompt = profileProvider.buildProfilePrompt()
            val memoryPrefix = memoryManager.buildMemoryPromptPrefix(chatId)
            val invariantPrompt = invariantProvider.buildInvariantPrompt()

            if (strategyContext != null) {
                _compressionStats.value = strategyContext.stats
                messagesToSend = strategyContext.messagesToSend

                systemPromptText = buildString {
                    if (invariantPrompt.isNotBlank()) {
                        append(invariantPrompt)
                    }
                    if (profilePrompt.isNotBlank()) {
                        if (isNotEmpty()) append("\n\n")
                        append(profilePrompt)
                    }
                    if (memoryPrefix.isNotBlank()) {
                        if (isNotEmpty()) append("\n\n")
                        append(memoryPrefix)
                    }
                    if (strategyContext.systemPromptPrefix.isNotBlank()) {
                        if (isNotEmpty()) append("\n\n")
                        append(strategyContext.systemPromptPrefix)
                    }
                    if (settings.systemPrompt.isNotBlank()) {
                        if (isNotEmpty()) append("\n\n")
                        append(settings.systemPrompt)
                    }
                }
            } else {
                _compressionStats.value = CompressionStats()
                messagesToSend = currentMessages
                systemPromptText = buildString {
                    if (invariantPrompt.isNotBlank()) {
                        append(invariantPrompt)
                    }
                    if (profilePrompt.isNotBlank()) {
                        if (isNotEmpty()) append("\n\n")
                        append(profilePrompt)
                    }
                    if (memoryPrefix.isNotBlank()) {
                        if (isNotEmpty()) append("\n\n")
                        append(memoryPrefix)
                    }
                    if (settings.systemPrompt.isNotBlank()) {
                        if (isNotEmpty()) append("\n\n")
                        append(settings.systemPrompt)
                    }
                }
            }

            val toolsArray = buildToolsArray()
            val collectedToolCalls = mutableListOf<ToolCallEntry>()
            var totalInputTokens = 0
            var totalOutputTokens = 0
            var totalElapsed = 0L

            // Build initial messages
            val conversationMessages = JSONArray().apply {
                for (msg in messagesToSend) {
                    put(JSONObject().apply {
                        put("role", if (msg.isUser) "user" else "assistant")
                        put("content", msg.text)
                    })
                }
            }

            var maxIterations = MAX_TOOL_ITERATIONS

            while (true) {
                val jsonBody = JSONObject().apply {
                    put("model", model.apiId)
                    put("max_tokens", settings.maxTokens)
                    put("temperature", settings.temperature.toDouble())
                    if (systemPromptText.isNotBlank()) {
                        put("system", systemPromptText)
                    }
                    put("messages", conversationMessages)
                    if (settings.stopSequences.isNotEmpty()) {
                        put("stop_sequences", JSONArray(settings.stopSequences))
                    }
                    if (toolsArray != null) {
                        put("tools", toolsArray)
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
                totalElapsed += elapsed
                val body = response.body?.string() ?: "Empty response"

                if (!response.isSuccessful) {
                    error("Error ${response.code}: $body")
                }

                val json = JSONObject(body)
                val content = json.getJSONArray("content")
                val stopReason = json.optString("stop_reason", "end_turn")
                val usage = json.getJSONObject("usage")
                totalInputTokens += usage.getInt("input_tokens")
                totalOutputTokens += usage.getInt("output_tokens")

                if (stopReason == "tool_use" && toolRouter != null) {
                    // Add assistant message with full content (text + tool_use blocks)
                    conversationMessages.put(JSONObject().apply {
                        put("role", "assistant")
                        put("content", content)
                    })

                    // Process each tool_use block
                    val toolResultsContent = JSONArray()
                    for (i in 0 until content.length()) {
                        val block = content.getJSONObject(i)
                        if (block.getString("type") == "tool_use") {
                            val toolUseId = block.getString("id")
                            val toolName = block.getString("name")
                            val toolInput = block.getJSONObject("input")

                            val arguments = mutableMapOf<String, Any?>()
                            for (key in toolInput.keys()) {
                                arguments[key] = toolInput.get(key)
                            }

                            val toolResult = try {
                                toolRouter.callTool(toolName, arguments)
                            } catch (e: Exception) {
                                "Error calling tool '$toolName': ${e.message}"
                            }

                            val serverName = toolRouter.getServerNameForTool(toolName)

                            collectedToolCalls.add(
                                ToolCallEntry(
                                    toolName = toolName,
                                    serverName = serverName,
                                    arguments = arguments.mapValues { it.value?.toString().orEmpty() },
                                    result = toolResult,
                                )
                            )

                            toolResultsContent.put(JSONObject().apply {
                                put("type", "tool_result")
                                put("tool_use_id", toolUseId)
                                put("content", toolResult)
                            })
                        }
                    }

                    // Add user message with tool results
                    conversationMessages.put(JSONObject().apply {
                        put("role", "user")
                        put("content", toolResultsContent)
                    })

                    maxIterations--
                    if (maxIterations <= 0) {
                        break
                    }
                } else {
                    // Final response — extract text
                    val text = buildString {
                        for (i in 0 until content.length()) {
                            val block = content.getJSONObject(i)
                            if (block.getString("type") == "text") {
                                if (isNotEmpty()) append("\n")
                                append(block.getString("text"))
                            }
                        }
                    }.ifEmpty { "Empty response from Claude" }

                    val cost = totalInputTokens * model.inputPricePerMillion / 1_000_000 +
                            totalOutputTokens * model.outputPricePerMillion / 1_000_000

                    val metadata = ResponseMetadata(
                        modelId = model.apiId,
                        inputTokens = totalInputTokens,
                        outputTokens = totalOutputTokens,
                        responseTimeMs = totalElapsed,
                        costUsd = cost,
                    )

                    return@withContext ApiResult(text, metadata, collectedToolCalls)
                }
            }

            // Fallback if max iterations exceeded
            val cost = totalInputTokens * model.inputPricePerMillion / 1_000_000 +
                    totalOutputTokens * model.outputPricePerMillion / 1_000_000
            val metadata = ResponseMetadata(
                modelId = model.apiId,
                inputTokens = totalInputTokens,
                outputTokens = totalOutputTokens,
                responseTimeMs = totalElapsed,
                costUsd = cost,
            )
            ApiResult(
                "Reached maximum tool call iterations ($MAX_TOOL_ITERATIONS)",
                metadata,
                collectedToolCalls,
            )
        }
    }

    private suspend fun buildToolsArray(): JSONArray? {
        val mcpTools = toolRouter?.getAllTools() ?: return null
        if (mcpTools.isEmpty()) return null

        return JSONArray().apply {
            for (tool in mcpTools) {
                put(JSONObject().apply {
                    put("name", tool.name)
                    put("description", tool.description)
                    put("input_schema", JSONObject(tool.inputSchemaJson))
                })
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
            contextWindow = currentSettings.model.contextWindow,
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

    private companion object {
        const val MAX_TOOL_ITERATIONS = 10
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
