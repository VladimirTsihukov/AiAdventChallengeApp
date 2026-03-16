package com.tishukoff.feature.mcp.api

/**
 * Routes tool calls to the appropriate MCP server.
 * Aggregates tools from all connected servers.
 */
interface McpToolRouter {

    /**
     * Returns all tools from all connected servers.
     */
    suspend fun getAllTools(): List<McpTool>

    /**
     * Calls a tool by name, routing to the correct server.
     */
    suspend fun callTool(toolName: String, arguments: Map<String, Any?>): String

    /**
     * Returns the list of currently connected servers.
     */
    fun getConnectedServers(): List<McpServerInfo>

    /**
     * Returns the server name that hosts the given tool.
     */
    fun getServerNameForTool(toolName: String): String
}
