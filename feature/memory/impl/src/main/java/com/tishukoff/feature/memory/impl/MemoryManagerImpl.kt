package com.tishukoff.feature.memory.impl

import com.tishukoff.core.database.api.LongTermMemoryRecord
import com.tishukoff.core.database.api.LongTermMemoryStorage
import com.tishukoff.core.database.api.WorkingMemoryRecord
import com.tishukoff.core.database.api.WorkingMemoryStorage
import com.tishukoff.feature.memory.api.MemoryEntry
import com.tishukoff.feature.memory.api.MemoryManager
import com.tishukoff.feature.memory.api.MemoryType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

internal class MemoryManagerImpl(
    private val apiKey: String,
    private val workingMemoryStorage: WorkingMemoryStorage,
    private val longTermMemoryStorage: LongTermMemoryStorage,
) : MemoryManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun processNewMessage(
        chatId: Long,
        userMessage: String,
        assistantResponse: String,
    ) {
        if (userMessage.isBlank() && assistantResponse.isBlank()) return

        val extractedFacts = extractFacts(userMessage, assistantResponse)
        val now = System.currentTimeMillis()

        for (fact in extractedFacts) {
            when (fact) {
                is ExtractedFact.Working -> {
                    workingMemoryStorage.upsert(
                        WorkingMemoryRecord(
                            chatId = chatId,
                            key = fact.key,
                            value = fact.value,
                            timestamp = now,
                        )
                    )
                }
                is ExtractedFact.LongTerm -> {
                    longTermMemoryStorage.upsert(
                        LongTermMemoryRecord(
                            key = fact.key,
                            value = fact.value,
                            category = fact.category,
                            timestamp = now,
                        )
                    )
                }
            }
        }
    }

    override fun getWorkingMemory(chatId: Long): Flow<List<MemoryEntry>> =
        workingMemoryStorage.getByChatId(chatId).map { records ->
            records.map { it.toMemoryEntry() }
        }

    override fun getLongTermMemory(): Flow<List<MemoryEntry>> =
        longTermMemoryStorage.getAll().map { records ->
            records.map { it.toMemoryEntry() }
        }

    override suspend fun buildMemoryPromptPrefix(chatId: Long): String = buildString {
        val longTermRecords = longTermMemoryStorage.getAllOnce()
        if (longTermRecords.isNotEmpty()) {
            appendLine("[Long-term Memory]")
            val grouped = longTermRecords.groupBy { it.category }
            for ((category, records) in grouped) {
                appendLine("# $category")
                for (record in records) {
                    appendLine("- ${record.key}: ${record.value}")
                }
            }
        }

        val workingRecords = workingMemoryStorage.getByChatIdOnce(chatId)
        if (workingRecords.isNotEmpty()) {
            if (isNotEmpty()) appendLine()
            appendLine("[Working Memory]")
            for (record in workingRecords) {
                appendLine("- ${record.key}: ${record.value}")
            }
        }
    }

    override suspend fun deleteWorkingMemoryEntry(chatId: Long, key: String) {
        workingMemoryStorage.deleteByKey(chatId, key)
    }

    override suspend fun deleteLongTermMemoryEntry(id: Long) {
        longTermMemoryStorage.deleteById(id)
    }

    private suspend fun extractFacts(
        userMessage: String,
        assistantResponse: String,
    ): List<ExtractedFact> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildExtractionPrompt(userMessage, assistantResponse)
            val response = callHaiku(prompt)
            parseFacts(response)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun buildExtractionPrompt(userMessage: String, assistantResponse: String): String =
        """Analyze this conversation exchange and extract important facts worth remembering.

Classify each fact as:
- WORKING: facts relevant only to the current task/conversation (current goals, decisions in progress, temporary context)
- LONG_TERM: facts about the user that persist across conversations (preferences, profile info, knowledge, past decisions)

For LONG_TERM facts, assign a category: preferences, profile, knowledge, or decisions.

Respond with one fact per line in this exact format:
WORKING|key|value
LONG_TERM|key|value|category

If no important facts are found, respond with a single line:
NONE

User message: $userMessage

Assistant response: $assistantResponse"""

    private fun callHaiku(prompt: String): String {
        val messagesArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })
        }

        val jsonBody = JSONObject().apply {
            put("model", "claude-haiku-4-5-20251001")
            put("max_tokens", 1024)
            put("temperature", 0.0)
            put("messages", messagesArray)
        }

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string().orEmpty()

        if (!response.isSuccessful) return ""

        val json = JSONObject(body)
        val content = json.getJSONArray("content")
        return if (content.length() > 0) {
            content.getJSONObject(0).getString("text")
        } else {
            ""
        }
    }

    private fun parseFacts(response: String): List<ExtractedFact> {
        if (response.isBlank() || response.trim() == "NONE") return emptyList()

        return response.lines().mapNotNull { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("WORKING|") -> {
                    val parts = trimmed.split("|", limit = 3)
                    if (parts.size == 3) {
                        ExtractedFact.Working(key = parts[1], value = parts[2])
                    } else null
                }
                trimmed.startsWith("LONG_TERM|") -> {
                    val parts = trimmed.split("|", limit = 4)
                    if (parts.size == 4) {
                        ExtractedFact.LongTerm(
                            key = parts[1],
                            value = parts[2],
                            category = parts[3],
                        )
                    } else null
                }
                else -> null
            }
        }
    }
}

private sealed interface ExtractedFact {
    data class Working(val key: String, val value: String) : ExtractedFact
    data class LongTerm(val key: String, val value: String, val category: String) : ExtractedFact
}

private fun WorkingMemoryRecord.toMemoryEntry() = MemoryEntry(
    id = id,
    key = key,
    value = value,
    type = MemoryType.WORKING,
    chatId = chatId,
    timestamp = timestamp,
)

private fun LongTermMemoryRecord.toMemoryEntry() = MemoryEntry(
    id = id,
    key = key,
    value = value,
    type = MemoryType.LONG_TERM,
    category = category,
    timestamp = timestamp,
)
