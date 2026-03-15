package com.tishukoff.weatherserver

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

/**
 * HTTP client for OpenWeatherMap API.
 */
class WeatherApiClient(private val apiKey: String) {

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    /**
     * Gets current weather for a city.
     */
    suspend fun getCurrentWeather(city: String): String {
        val response = httpClient.get(
            "$BASE_URL/data/2.5/weather?q=$city&appid=$apiKey&units=metric&lang=en"
        )

        if (!response.status.isSuccess()) {
            return "Error: ${response.status.value} — ${response.bodyAsText()}"
        }

        val body = response.bodyAsText()
        val obj = json.decodeFromString<JsonObject>(body)

        val main = obj["main"]?.jsonObject
        val weather = obj["weather"]?.jsonArray?.firstOrNull()?.jsonObject
        val wind = obj["wind"]?.jsonObject
        val name = obj["name"]?.jsonPrimitive?.content ?: city

        return buildString {
            appendLine("Weather in $name:")
            appendLine("  Temperature: ${main?.get("temp")?.jsonPrimitive?.double}°C")
            appendLine("  Feels like: ${main?.get("feels_like")?.jsonPrimitive?.double}°C")
            appendLine("  Humidity: ${main?.get("humidity")?.jsonPrimitive?.int}%")
            appendLine("  Description: ${weather?.get("description")?.jsonPrimitive?.content}")
            appendLine("  Wind: ${wind?.get("speed")?.jsonPrimitive?.double} m/s")
        }
    }

    /**
     * Gets weather forecast for a city for the specified number of days (1-5).
     */
    suspend fun getForecast(city: String, days: Int): String {
        val cnt = (days.coerceIn(1, 5)) * 8
        val response = httpClient.get(
            "$BASE_URL/data/2.5/forecast?q=$city&cnt=$cnt&appid=$apiKey&units=metric&lang=en"
        )

        if (!response.status.isSuccess()) {
            return "Error: ${response.status.value} — ${response.bodyAsText()}"
        }

        val body = response.bodyAsText()
        val obj = json.decodeFromString<JsonObject>(body)
        val list = obj["list"]?.jsonArray ?: return "No forecast data"
        val cityName = obj["city"]?.jsonObject?.get("name")?.jsonPrimitive?.content ?: city

        return buildString {
            appendLine("Forecast for $cityName ($days days):")
            appendLine()
            for (item in list) {
                val itemObj = item.jsonObject
                val dt = itemObj["dt_txt"]?.jsonPrimitive?.content ?: ""
                val main = itemObj["main"]?.jsonObject
                val weather = itemObj["weather"]?.jsonArray?.firstOrNull()?.jsonObject
                val temp = main?.get("temp")?.jsonPrimitive?.double
                val description = weather?.get("description")?.jsonPrimitive?.content
                appendLine("  $dt — ${temp}°C, $description")
            }
        }
    }

    private companion object {
        const val BASE_URL = "https://api.openweathermap.org"
    }
}
