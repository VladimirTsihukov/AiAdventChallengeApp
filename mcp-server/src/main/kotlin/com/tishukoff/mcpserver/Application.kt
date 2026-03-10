package com.tishukoff.mcpserver

import io.ktor.server.netty.Netty
import io.ktor.server.engine.embeddedServer
import java.io.File
import java.util.Properties
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

fun main() {
    val env = loadEnv()
    val port = (env["PORT"] ?: System.getenv("PORT"))?.toIntOrNull() ?: 3000
    val githubToken = env["GITHUB_TOKEN"] ?: System.getenv("GITHUB_TOKEN")

    val github = GitHubApiClient(token = githubToken)

    println("Starting GitHub MCP Server on port $port...")

    embeddedServer(Netty, host = "0.0.0.0", port = port) {
        mcp {
            createMcpServer(github)
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

fun createMcpServer(github: GitHubApiClient): Server {
    val server = Server(
        serverInfo = Implementation(
            name = "github-mcp-server",
            version = "1.0.0",
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true),
            ),
        ),
    )

    server.addTool(
        name = "get_repository",
        description = "Get information about a GitHub repository (stars, forks, description, language, etc.)",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("owner") {
                    put("type", "string")
                    put("description", "Repository owner (user or organization)")
                }
                putJsonObject("repo") {
                    put("type", "string")
                    put("description", "Repository name")
                }
            },
            required = listOf("owner", "repo"),
        ),
    ) { request ->
        val owner = request.arguments?.get("owner")?.jsonPrimitive?.content
        val repo = request.arguments?.get("repo")?.jsonPrimitive?.content

        if (owner.isNullOrBlank() || repo.isNullOrBlank()) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Error: 'owner' and 'repo' are required")),
                isError = true,
            )
        }

        val result = github.getRepository(owner, repo)
        CallToolResult(content = listOf(TextContent(result)))
    }

    server.addTool(
        name = "search_repositories",
        description = "Search GitHub repositories by query. Returns top results with name, description, stars, and language.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("query") {
                    put("type", "string")
                    put("description", "Search query (e.g. 'kotlin mcp', 'language:rust stars:>1000')")
                }
                putJsonObject("per_page") {
                    put("type", "integer")
                    put("description", "Number of results (1-10, default 5)")
                }
            },
            required = listOf("query"),
        ),
    ) { request ->
        val query = request.arguments?.get("query")?.jsonPrimitive?.content
        val perPage = request.arguments?.get("per_page")?.jsonPrimitive?.content?.toIntOrNull() ?: 5

        if (query.isNullOrBlank()) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Error: 'query' is required")),
                isError = true,
            )
        }

        val result = github.searchRepositories(query, perPage.coerceIn(1, 10))
        CallToolResult(content = listOf(TextContent(result)))
    }

    server.addTool(
        name = "get_file_content",
        description = "Get the content of a file from a GitHub repository.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("owner") {
                    put("type", "string")
                    put("description", "Repository owner (user or organization)")
                }
                putJsonObject("repo") {
                    put("type", "string")
                    put("description", "Repository name")
                }
                putJsonObject("path") {
                    put("type", "string")
                    put("description", "File path in the repository (e.g. 'README.md', 'src/main.kt')")
                }
            },
            required = listOf("owner", "repo", "path"),
        ),
    ) { request ->
        val owner = request.arguments?.get("owner")?.jsonPrimitive?.content
        val repo = request.arguments?.get("repo")?.jsonPrimitive?.content
        val path = request.arguments?.get("path")?.jsonPrimitive?.content

        if (owner.isNullOrBlank() || repo.isNullOrBlank() || path.isNullOrBlank()) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Error: 'owner', 'repo', and 'path' are required")),
                isError = true,
            )
        }

        val result = github.getFileContent(owner, repo, path)
        CallToolResult(content = listOf(TextContent(result)))
    }

    return server
}
