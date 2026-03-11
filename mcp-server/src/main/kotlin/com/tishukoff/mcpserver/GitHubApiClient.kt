package com.tishukoff.mcpserver

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class GitHubApiClient(
    private val token: String?,
) {

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    /**
     * Returns raw JSON string with repository info.
     */
    suspend fun getRepository(owner: String, repo: String): String {
        val response = httpClient.get("$BASE_URL/repos/$owner/$repo") {
            applyAuth()
        }
        if (!response.status.isSuccess()) {
            return """{"error": "GitHub API returned ${response.status.value}: ${response.bodyAsText()}"}"""
        }
        return response.bodyAsText()
    }

    /**
     * Searches repositories by query. Returns raw JSON string.
     */
    suspend fun searchRepositories(query: String, perPage: Int = 5): String {
        val response = httpClient.get("$BASE_URL/search/repositories") {
            parameter("q", query)
            parameter("per_page", perPage)
            applyAuth()
        }
        if (!response.status.isSuccess()) {
            return """{"error": "GitHub API returned ${response.status.value}: ${response.bodyAsText()}"}"""
        }
        return response.bodyAsText()
    }

    /**
     * Gets file content from a repository. Returns raw JSON string.
     */
    suspend fun getFileContent(owner: String, repo: String, path: String): String {
        val response = httpClient.get("$BASE_URL/repos/$owner/$repo/contents/$path") {
            applyAuth()
        }
        if (!response.status.isSuccess()) {
            return """{"error": "GitHub API returned ${response.status.value}: ${response.bodyAsText()}"}"""
        }
        return response.bodyAsText()
    }

    private fun io.ktor.client.request.HttpRequestBuilder.applyAuth() {
        if (!token.isNullOrBlank()) {
            header("Authorization", "Bearer $token")
        }
        header("Accept", "application/vnd.github.v3+json")
    }

    private companion object {
        const val BASE_URL = "https://api.github.com"
    }
}
