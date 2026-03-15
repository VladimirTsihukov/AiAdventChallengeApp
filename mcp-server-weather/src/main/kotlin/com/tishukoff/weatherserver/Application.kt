package com.tishukoff.weatherserver

import io.ktor.server.netty.Netty
import io.ktor.server.engine.embeddedServer
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.File
import java.util.Properties

fun main() {
    val env = loadEnv()
    val port = (env["PORT"] ?: System.getenv("PORT"))?.toIntOrNull() ?: 3001
    val weatherApiKey = env["OPENWEATHERMAP_API_KEY"] ?: System.getenv("OPENWEATHERMAP_API_KEY")

    if (weatherApiKey.isNullOrBlank() || weatherApiKey == "your_api_key_here") {
        println("ERROR: OPENWEATHERMAP_API_KEY is not set. Get a free key at https://openweathermap.org/api")
        return
    }

    val telegramBotToken = env["TELEGRAM_BOT_TOKEN"] ?: System.getenv("TELEGRAM_BOT_TOKEN")
    val telegramChatId = env["TELEGRAM_CHAT_ID"] ?: System.getenv("TELEGRAM_CHAT_ID")
    val telegram = if (!telegramBotToken.isNullOrBlank() && !telegramChatId.isNullOrBlank()) {
        TelegramClient(botToken = telegramBotToken, chatId = telegramChatId)
    } else {
        println("WARNING: TELEGRAM_BOT_TOKEN or TELEGRAM_CHAT_ID not set — Telegram tool will be unavailable")
        null
    }

    val weather = WeatherApiClient(apiKey = weatherApiKey)

    println("Starting Weather MCP Server on port $port...")

    embeddedServer(Netty, host = "0.0.0.0", port = port) {
        mcp {
            createWeatherMcpServer(weather, telegram)
        }
    }.start(wait = true)
}

private fun loadEnv(): Map<String, String> {
    val envFile = File(".env")
    if (!envFile.exists()) return emptyMap()

    val props = Properties()
    envFile.inputStream().use { props.load(it) }
    return props.entries.associate { (k, v) -> k.toString() to v.toString() }
}

fun createWeatherMcpServer(
    weather: WeatherApiClient,
    telegram: TelegramClient?,
): Server {
    val server = Server(
        serverInfo = Implementation(
            name = "weather-mcp-server",
            version = "1.0.0",
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true),
            ),
        ),
    )

    server.addTool(
        name = "get_current_weather",
        description = "Get current weather for a city. Returns temperature, humidity, wind speed, and weather description.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("city") {
                    put("type", "string")
                    put("description", "City name (e.g. 'Moscow', 'London', 'New York')")
                }
            },
            required = listOf("city"),
        ),
    ) { request ->
        val city = request.arguments?.get("city")?.jsonPrimitive?.content

        if (city.isNullOrBlank()) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Error: 'city' is required")),
                isError = true,
            )
        }

        val result = weather.getCurrentWeather(city)
        CallToolResult(content = listOf(TextContent(result)))
    }

    server.addTool(
        name = "get_forecast",
        description = "Get weather forecast for a city for 1 to 5 days. Returns temperature and description for each 3-hour interval.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("city") {
                    put("type", "string")
                    put("description", "City name (e.g. 'Moscow', 'London', 'New York')")
                }
                putJsonObject("days") {
                    put("type", "integer")
                    put("description", "Number of days (1-5, default 3)")
                }
            },
            required = listOf("city"),
        ),
    ) { request ->
        val city = request.arguments?.get("city")?.jsonPrimitive?.content
        val days = request.arguments?.get("days")?.jsonPrimitive?.content?.toIntOrNull() ?: 3

        if (city.isNullOrBlank()) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Error: 'city' is required")),
                isError = true,
            )
        }

        val result = weather.getForecast(city, days)
        CallToolResult(content = listOf(TextContent(result)))
    }

    server.addTool(
        name = "send_weather_to_telegram",
        description = "Get current weather for a city and send it to Telegram. Combines weather lookup with Telegram notification.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("city") {
                    put("type", "string")
                    put("description", "City name (e.g. 'Moscow', 'London', 'New York')")
                }
            },
            required = listOf("city"),
        ),
    ) { request ->
        val city = request.arguments?.get("city")?.jsonPrimitive?.content

        if (city.isNullOrBlank()) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Error: 'city' is required")),
                isError = true,
            )
        }

        if (telegram == null) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Error: Telegram is not configured")),
                isError = true,
            )
        }

        val weatherResult = weather.getCurrentWeather(city)
        val telegramResult = telegram.sendMessage(weatherResult)
        CallToolResult(content = listOf(TextContent("$weatherResult\n\n$telegramResult")))
    }

    return server
}
