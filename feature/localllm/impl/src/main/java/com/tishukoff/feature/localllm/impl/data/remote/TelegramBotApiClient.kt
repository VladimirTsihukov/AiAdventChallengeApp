package com.tishukoff.feature.localllm.impl.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * HTTP-клиент для REST API telegram-llm-bot сервера.
 */
internal class TelegramBotApiClient(
    private val baseUrl: String = "http://192.168.100.5:8080",
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Получает список активных чатов бота.
     */
    suspend fun getChats(): List<ChatInfoDto> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api/chats")
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw RuntimeException("Bot API error: ${response.code}")
        }

        val body = response.body?.string()
            ?: throw RuntimeException("Empty response from Bot API")

        json.decodeFromString<List<ChatInfoDto>>(body)
    }

    /**
     * Получает историю сообщений конкретного чата.
     */
    suspend fun getMessages(chatId: Long): List<MessageDto> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api/chats/$chatId/messages")
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw RuntimeException("Bot API error: ${response.code}")
        }

        val body = response.body?.string()
            ?: throw RuntimeException("Empty response from Bot API")

        json.decodeFromString<List<MessageDto>>(body)
    }

    /**
     * Получает статус бота.
     */
    suspend fun getStatus(): BotStatusDto = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api/status")
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw RuntimeException("Bot API error: ${response.code}")
        }

        val body = response.body?.string()
            ?: throw RuntimeException("Empty response from Bot API")

        json.decodeFromString<BotStatusDto>(body)
    }

    /**
     * Удаляет историю переписки конкретного чата.
     */
    suspend fun clearHistory(chatId: Long): Unit = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api/chats/$chatId")
            .delete()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw RuntimeException("Bot API error: ${response.code}")
        }
    }
}

@Serializable
internal data class ChatInfoDto(
    val chatId: Long,
    val messageCount: Int,
    val lastMessage: String,
)

@Serializable
internal data class MessageDto(
    val role: String,
    val content: String,
)

@Serializable
internal data class BotStatusDto(
    val botRunning: Boolean,
    val model: String,
    val activeChatCount: Int,
)
