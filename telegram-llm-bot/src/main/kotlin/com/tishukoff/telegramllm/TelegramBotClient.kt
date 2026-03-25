package com.tishukoff.telegramllm

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Клиент Telegram Bot API: получение обновлений (long polling) и отправка сообщений.
 */
class TelegramBotClient(
    private val botToken: String,
) {

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Получает обновления от Telegram через long polling.
     */
    suspend fun getUpdates(offset: Long): List<Update> {
        val response = httpClient.get("$BASE_URL/bot$botToken/getUpdates") {
            parameter("offset", offset)
            parameter("timeout", POLL_TIMEOUT_SECONDS)
        }

        if (!response.status.isSuccess()) {
            println("Telegram getUpdates error: ${response.status.value}")
            return emptyList()
        }

        val body = response.bodyAsText()
        val result = json.decodeFromString<GetUpdatesResponse>(body)
        return result.result
    }

    /**
     * Отправляет текстовое сообщение в указанный чат.
     */
    suspend fun sendMessage(chatId: Long, text: String) {
        val messageText = if (text.length > MAX_MESSAGE_LENGTH) {
            text.take(MAX_MESSAGE_LENGTH) + "\n\n... (truncated)"
        } else {
            text
        }

        val body = buildJsonObject {
            put("chat_id", chatId)
            put("text", messageText)
        }

        val response = httpClient.post("$BASE_URL/bot$botToken/sendMessage") {
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }

        if (!response.status.isSuccess()) {
            val error = response.bodyAsText()
            println("Telegram sendMessage error: ${response.status.value} $error")
        }
    }

    private companion object {
        const val BASE_URL = "https://api.telegram.org"
        const val MAX_MESSAGE_LENGTH = 4096
        const val POLL_TIMEOUT_SECONDS = 30
    }
}

@Serializable
data class GetUpdatesResponse(
    val ok: Boolean = false,
    val result: List<Update> = emptyList(),
)

@Serializable
data class Update(
    val update_id: Long,
    val message: Message? = null,
)

@Serializable
data class Message(
    val message_id: Long = 0,
    val chat: Chat,
    val text: String? = null,
)

@Serializable
data class Chat(
    val id: Long,
)
