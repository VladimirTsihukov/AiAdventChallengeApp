package com.tishukoff.weatherserver

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Client for sending messages via Telegram Bot API.
 */
class TelegramClient(
    private val botToken: String,
    private val chatId: String,
) {

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    /**
     * Sends a text message to the configured Telegram chat.
     */
    suspend fun sendMessage(text: String): String {
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

        return if (response.status.isSuccess()) {
            "Message sent to Telegram (${messageText.length} chars)"
        } else {
            val error = response.bodyAsText()
            "Telegram API error ${response.status.value}: $error"
        }
    }

    private companion object {
        const val BASE_URL = "https://api.telegram.org"
        const val MAX_MESSAGE_LENGTH = 4096
    }
}
