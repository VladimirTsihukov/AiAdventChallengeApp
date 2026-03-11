package com.tishukoff.feature.mcp.impl.data

import com.tishukoff.feature.mcp.api.McpTool
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.sse.SSE
import java.util.concurrent.TimeUnit
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.TextContent

/**
 * Wrapper around the MCP Kotlin SDK client.
 * Manages connection lifecycle and provides tool listing.
 */
class McpClientWrapper {

    private var client: Client? = null
    private var httpClient: HttpClient? = null

    /**
     * Connects to the MCP server at the given URL and returns the list of available tools.
     * Automatically detects transport type: SSE for URLs ending with /sse, Streamable HTTP otherwise.
     */
    suspend fun connectAndListTools(serverUrl: String): McpConnectionResult {
        disconnect()

        val http = HttpClient(OkHttp) {
            engine {
                config {
                    connectTimeout(30, TimeUnit.SECONDS)
                    readTimeout(60, TimeUnit.SECONDS)
                    writeTimeout(30, TimeUnit.SECONDS)
                }
            }
            install(SSE)
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 60_000
            }
        }
        httpClient = http

        val mcpClient = Client(
            clientInfo = Implementation(
                name = "AiAdventChallengeApp",
                version = "1.0.0",
            )
        )
        client = mcpClient

        val transport = SseClientTransport(
            client = http,
            urlString = serverUrl,
        )

        mcpClient.connect(transport)

        val serverName = mcpClient.serverVersion?.name.orEmpty()
        val serverVersion = mcpClient.serverVersion?.version.orEmpty()

        val toolsResult = mcpClient.listTools()
        val tools = toolsResult.tools.map { tool ->
            McpTool(
                name = tool.name,
                description = tool.description.orEmpty(),
                inputSchemaJson = tool.inputSchema.toString(),
            )
        }

        return McpConnectionResult(
            serverName = serverName,
            serverVersion = serverVersion,
            tools = tools,
        )
    }

    /**
     * Calls a tool on the connected MCP server and returns the text result.
     */
    suspend fun callTool(toolName: String, arguments: Map<String, Any?>): String {
        val mcpClient = client ?: error("Not connected to MCP server")

        val result = mcpClient.callTool(
            name = toolName,
            arguments = arguments,
        )

        return result.content
            .filterIsInstance<TextContent>()
            .joinToString("\n") { it.text }
    }

    /**
     * Returns the list of tools from the currently connected MCP server.
     */
    suspend fun listTools(): List<McpTool> {
        val mcpClient = client ?: error("Not connected to MCP server")
        val toolsResult = mcpClient.listTools()
        return toolsResult.tools.map { tool ->
            McpTool(
                name = tool.name,
                description = tool.description.orEmpty(),
                inputSchemaJson = tool.inputSchema.toString(),
            )
        }
    }

    fun disconnect() {
        client = null
        httpClient?.close()
        httpClient = null
    }
}

data class McpConnectionResult(
    val serverName: String,
    val serverVersion: String,
    val tools: List<McpTool>,
)
