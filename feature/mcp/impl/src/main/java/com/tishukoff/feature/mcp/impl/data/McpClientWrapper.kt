package com.tishukoff.feature.mcp.impl.data

import com.tishukoff.feature.mcp.api.McpServerInfo
import com.tishukoff.feature.mcp.api.McpTool
import com.tishukoff.feature.mcp.api.McpToolRouter
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.sse.SSE
import java.util.concurrent.TimeUnit
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages connections to multiple MCP servers and routes tool calls.
 */
class McpClientWrapper : McpToolRouter {

    private val connections = mutableMapOf<String, McpServerConnection>()

    private val _connectedServers = MutableStateFlow<List<McpServerInfo>>(emptyList())
    val connectedServers: StateFlow<List<McpServerInfo>> = _connectedServers.asStateFlow()

    /**
     * Connects to an MCP server and returns server info with tools.
     * Multiple servers can be connected simultaneously.
     */
    suspend fun connectToServer(serverUrl: String): McpConnectionResult {
        disconnectServer(serverUrl)

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
                requestTimeoutMillis = Long.MAX_VALUE
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = Long.MAX_VALUE
            }
        }

        val mcpClient = Client(
            clientInfo = Implementation(
                name = "AiAdventChallengeApp",
                version = "1.0.0",
            )
        )

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
                inputSchemaJson = toolSchemaToJson(tool.inputSchema),
                serverId = serverUrl,
            )
        }

        val serverInfo = McpServerInfo(
            id = serverUrl,
            url = serverUrl,
            name = serverName,
            version = serverVersion,
            tools = tools,
        )

        connections[serverUrl] = McpServerConnection(
            id = serverUrl,
            client = mcpClient,
            httpClient = http,
            serverInfo = serverInfo,
        )

        updateConnectedServersFlow()

        return McpConnectionResult(
            serverName = serverName,
            serverVersion = serverVersion,
            tools = tools,
        )
    }

    /**
     * Disconnects from a specific server.
     */
    fun disconnectServer(serverId: String) {
        connections.remove(serverId)?.let { conn ->
            conn.httpClient.close()
        }
        updateConnectedServersFlow()
    }

    /**
     * Disconnects from all servers.
     */
    fun disconnectAll() {
        connections.values.forEach { it.httpClient.close() }
        connections.clear()
        updateConnectedServersFlow()
    }

    // --- McpToolRouter ---

    override suspend fun getAllTools(): List<McpTool> {
        return connections.values.flatMap { it.serverInfo.tools }
    }

    override suspend fun callTool(toolName: String, arguments: Map<String, Any?>): String {
        val connection = connections.values.firstOrNull { conn ->
            conn.serverInfo.tools.any { it.name == toolName }
        } ?: error("Tool '$toolName' not found on any connected server")

        val result = connection.client.callTool(
            name = toolName,
            arguments = arguments,
        )

        return result.content
            .filterIsInstance<TextContent>()
            .joinToString("\n") { it.text }
    }

    override fun getConnectedServers(): List<McpServerInfo> {
        return connections.values.map { it.serverInfo }
    }

    override fun getServerNameForTool(toolName: String): String {
        return connections.values
            .firstOrNull { conn -> conn.serverInfo.tools.any { it.name == toolName } }
            ?.serverInfo?.name
            .orEmpty()
    }

    // --- Legacy single-server API (for existing McpViewModel) ---

    @Deprecated("Use connectToServer instead", ReplaceWith("connectToServer(serverUrl)"))
    suspend fun connectAndListTools(serverUrl: String): McpConnectionResult {
        return connectToServer(serverUrl)
    }

    /**
     * Calls a tool on any connected server (finds the right one automatically).
     */
    suspend fun callToolLegacy(toolName: String, arguments: Map<String, Any?>): String {
        return callTool(toolName, arguments)
    }

    /**
     * Returns tools from all connected servers.
     */
    suspend fun listTools(): List<McpTool> = getAllTools()

    @Deprecated("Use disconnectAll instead", ReplaceWith("disconnectAll()"))
    fun disconnect() = disconnectAll()

    private fun updateConnectedServersFlow() {
        _connectedServers.value = connections.values.map { it.serverInfo }
    }

    private fun toolSchemaToJson(schema: ToolSchema): String {
        val jsonObject = buildJsonObject {
            put("type", "object")
            schema.properties?.let { put("properties", it) }
            val req = schema.required
            if (!req.isNullOrEmpty()) {
                putJsonArray("required") {
                    req.forEach { add(JsonPrimitive(it)) }
                }
            }
        }
        return jsonObject.toString()
    }

    private class McpServerConnection(
        val id: String,
        val client: Client,
        val httpClient: HttpClient,
        val serverInfo: McpServerInfo,
    )
}

data class McpConnectionResult(
    val serverName: String,
    val serverVersion: String,
    val tools: List<McpTool>,
)
