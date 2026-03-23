package com.tishukoff.telegramllm

import io.ktor.http.ContentType
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Properties

fun main() {
    val env = loadEnv()

    val botToken = env["TELEGRAM_BOT_TOKEN"] ?: System.getenv("TELEGRAM_BOT_TOKEN")
    if (botToken.isNullOrBlank()) {
        println("ERROR: TELEGRAM_BOT_TOKEN is not set. Add it to .env or environment variables.")
        return
    }

    val ollamaBaseUrl = env["OLLAMA_BASE_URL"]
        ?: System.getenv("OLLAMA_BASE_URL")
        ?: "http://192.168.100.5:11434"

    val ollamaModel = env["OLLAMA_MODEL"]
        ?: System.getenv("OLLAMA_MODEL")
        ?: "phi3:mini"

    val apiPort = (env["API_PORT"] ?: System.getenv("API_PORT"))?.toIntOrNull() ?: 8080

    val telegram = TelegramBotClient(botToken)
    val ollama = OllamaClient(baseUrl = ollamaBaseUrl, model = ollamaModel)
    val historyStore = ChatHistoryStore()

    startApiServer(historyStore, ollamaModel, apiPort)

    println("Telegram LLM Bot started")
    println("Ollama: $ollamaBaseUrl (model: $ollamaModel)")
    println("API server: http://0.0.0.0:$apiPort")
    println("Waiting for messages...")

    var offset = 0L

    @Suppress("TooGenericExceptionCaught")
    while (true) {
        try {
            val updates = kotlinx.coroutines.runBlocking {
                telegram.getUpdates(offset)
            }

            for (update in updates) {
                offset = update.update_id + 1
                val message = update.message ?: continue
                val text = message.text ?: continue
                val chatId = message.chat.id

                println("[$chatId] User: $text")

                kotlinx.coroutines.runBlocking {
                    handleMessage(telegram, ollama, historyStore, chatId, text)
                }
            }
        } catch (e: Exception) {
            println("Polling error: ${e.message}")
            Thread.sleep(RETRY_DELAY_MS)
        }
    }
}

private fun startApiServer(historyStore: ChatHistoryStore, model: String, port: Int) {
    val json = Json { prettyPrint = true }

    embeddedServer(Netty, host = "0.0.0.0", port = port) {
        routing {
            get("/api/chats") {
                val chatIds = historyStore.getAllChatIds()
                val chats = chatIds.map { id ->
                    val messages = historyStore.getHistory(id)
                    ChatInfo(
                        chatId = id,
                        messageCount = messages.size,
                        lastMessage = messages.lastOrNull()?.second.orEmpty(),
                    )
                }
                call.respondText(json.encodeToString(chats), ContentType.Application.Json)
            }

            get("/api/chats/{chatId}/messages") {
                val chatId = call.parameters["chatId"]?.toLongOrNull()
                if (chatId == null) {
                    call.respondText(
                        """{"error":"Invalid chatId"}""",
                        ContentType.Application.Json,
                        io.ktor.http.HttpStatusCode.BadRequest,
                    )
                    return@get
                }

                val messages = historyStore.getHistory(chatId).map { (role, content) ->
                    MessageDto(role = role, content = content)
                }
                call.respondText(json.encodeToString(messages), ContentType.Application.Json)
            }

            delete("/api/chats/{chatId}") {
                val chatId = call.parameters["chatId"]?.toLongOrNull()
                if (chatId == null) {
                    call.respondText(
                        """{"error":"Invalid chatId"}""",
                        ContentType.Application.Json,
                        io.ktor.http.HttpStatusCode.BadRequest,
                    )
                    return@delete
                }

                historyStore.clearHistory(chatId)
                call.respondText("""{"ok":true}""", ContentType.Application.Json)
            }

            get("/api/status") {
                val status = StatusDto(
                    botRunning = true,
                    model = model,
                    activeChatCount = historyStore.getAllChatIds().size,
                )
                call.respondText(json.encodeToString(status), ContentType.Application.Json)
            }
        }
    }.start(wait = false)
}

private suspend fun handleMessage(
    telegram: TelegramBotClient,
    ollama: OllamaClient,
    historyStore: ChatHistoryStore,
    chatId: Long,
    text: String,
) {
    when {
        text == "/start" -> {
            telegram.sendMessage(chatId, "Hello! I'm a bot powered by a local LLM. Send me a message and I'll reply. Use /clear to reset the conversation history.")
        }

        text == "/clear" -> {
            historyStore.clearHistory(chatId)
            telegram.sendMessage(chatId, "Conversation history cleared.")
        }

        else -> {
            historyStore.addUserMessage(chatId, text)
            val history = historyStore.getHistory(chatId)

            @Suppress("TooGenericExceptionCaught")
            try {
                val response = ollama.chat(history)
                historyStore.addAssistantMessage(chatId, response)
                telegram.sendMessage(chatId, response)
                println("[$chatId] Assistant: ${response.take(100)}...")
            } catch (e: Exception) {
                val errorMsg = "Error communicating with LLM: ${e.message}"
                telegram.sendMessage(chatId, errorMsg)
                println("[$chatId] Error: ${e.message}")
            }
        }
    }
}

private fun loadEnv(): Map<String, String> {
    val envFile = File(".env")
    if (!envFile.exists()) return emptyMap()

    val props = Properties()
    envFile.inputStream().use { props.load(it) }
    return props.entries.associate { (k, v) -> k.toString() to v.toString() }
}

private const val RETRY_DELAY_MS = 5000L

@Serializable
data class ChatInfo(
    val chatId: Long,
    val messageCount: Int,
    val lastMessage: String,
)

@Serializable
data class MessageDto(
    val role: String,
    val content: String,
)

@Serializable
data class StatusDto(
    val botRunning: Boolean,
    val model: String,
    val activeChatCount: Int,
)
