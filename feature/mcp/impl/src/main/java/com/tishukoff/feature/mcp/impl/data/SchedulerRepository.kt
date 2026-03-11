package com.tishukoff.feature.mcp.impl.data

import com.tishukoff.feature.mcp.api.ScheduledTaskInfo
import com.tishukoff.feature.mcp.api.TaskResultInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

/**
 * Repository for managing scheduled tasks through MCP server tools.
 */
class SchedulerRepository(
    private val mcpClientWrapper: McpClientWrapper,
) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Creates a new scheduled task on the MCP server.
     */
    suspend fun createTask(
        name: String,
        toolName: String,
        toolArguments: Map<String, String>,
        intervalMinutes: Int,
        durationMinutes: Int,
    ): ScheduledTaskInfo {
        val argsJson = buildString {
            append("{")
            append(toolArguments.entries.joinToString(",") { (k, v) ->
                "\"$k\":\"$v\""
            })
            append("}")
        }

        val result = mcpClientWrapper.callTool(
            toolName = "create_scheduled_task",
            arguments = mapOf(
                "name" to name,
                "tool_name" to toolName,
                "tool_arguments" to argsJson,
                "interval_minutes" to intervalMinutes.toString(),
                "duration_minutes" to durationMinutes.toString(),
            ),
        )

        val taskJson = extractJsonObject(result)
        return parseTask(taskJson)
    }

    /**
     * Lists all scheduled tasks from the MCP server.
     */
    suspend fun listTasks(): List<ScheduledTaskInfo> {
        val result = mcpClientWrapper.callTool(
            toolName = "list_scheduled_tasks",
            arguments = emptyMap(),
        )

        if (result.contains("No scheduled tasks")) return emptyList()

        val array = json.parseToJsonElement(result.trim())
        if (array !is JsonArray) return emptyList()

        return array.map { parseTask(it.jsonObject) }
    }

    /**
     * Deletes a scheduled task by ID.
     */
    suspend fun deleteTask(taskId: String) {
        mcpClientWrapper.callTool(
            toolName = "delete_scheduled_task",
            arguments = mapOf("task_id" to taskId),
        )
    }

    /**
     * Toggles a scheduled task active/inactive.
     */
    suspend fun toggleTask(taskId: String, isActive: Boolean) {
        mcpClientWrapper.callTool(
            toolName = "toggle_scheduled_task",
            arguments = mapOf(
                "task_id" to taskId,
                "is_active" to isActive.toString(),
            ),
        )
    }

    /**
     * Gets execution results for a scheduled task.
     */
    suspend fun getTaskResults(taskId: String, limit: Int = 10): List<TaskResultInfo> {
        val result = mcpClientWrapper.callTool(
            toolName = "get_task_results",
            arguments = mapOf(
                "task_id" to taskId,
                "limit" to limit.toString(),
            ),
        )

        if (result.contains("No results yet")) return emptyList()

        val array = json.parseToJsonElement(result.trim())
        if (array !is JsonArray) return emptyList()

        return array.map { element ->
            val obj = element.jsonObject
            TaskResultInfo(
                taskId = obj["taskId"]?.jsonPrimitive?.content.orEmpty(),
                executedAt = obj["executedAt"]?.jsonPrimitive?.long ?: 0L,
                result = obj["result"]?.jsonPrimitive?.content.orEmpty(),
                isError = obj["isError"]?.jsonPrimitive?.boolean ?: false,
            )
        }
    }

    private fun extractJsonObject(text: String): JsonObject {
        val jsonStart = text.indexOf('{')
        val jsonStr = if (jsonStart >= 0) text.substring(jsonStart) else text
        return json.parseToJsonElement(jsonStr.trim()).jsonObject
    }

    private fun parseTask(obj: JsonObject): ScheduledTaskInfo {
        val toolArgsElement = obj["toolArguments"]?.jsonObject
        val toolArgs = toolArgsElement?.entries?.associate { (k, v) ->
            k to v.jsonPrimitive.content
        } ?: emptyMap()

        return ScheduledTaskInfo(
            id = obj["id"]?.jsonPrimitive?.content.orEmpty(),
            name = obj["name"]?.jsonPrimitive?.content.orEmpty(),
            toolName = obj["toolName"]?.jsonPrimitive?.content.orEmpty(),
            toolArguments = toolArgs,
            intervalMinutes = obj["intervalMinutes"]?.jsonPrimitive?.int ?: 0,
            durationMinutes = obj["durationMinutes"]?.jsonPrimitive?.int ?: 0,
            createdAt = obj["createdAt"]?.jsonPrimitive?.long ?: 0L,
            expiresAt = obj["expiresAt"]?.jsonPrimitive?.long ?: 0L,
            lastRunAt = obj["lastRunAt"]?.jsonPrimitive?.long,
            isActive = obj["isActive"]?.jsonPrimitive?.boolean ?: true,
        )
    }
}
