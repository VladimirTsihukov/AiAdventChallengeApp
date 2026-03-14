package com.tishukoff.feature.mcp.impl.data

import com.tishukoff.feature.mcp.api.PipelineStatusInfo
import com.tishukoff.feature.mcp.api.PipelineStepInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

/**
 * Repository for managing pipelines through MCP server tools.
 */
class PipelineRepository(
    private val mcpClientWrapper: McpClientWrapper,
) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Creates and starts a pipeline on the MCP server.
     * Returns the pipeline ID.
     */
    suspend fun createPipeline(
        query: String,
        filename: String,
        perPage: Int,
        searchEnabled: Boolean,
        summarizeEnabled: Boolean,
        saveToFileEnabled: Boolean,
        telegramEnabled: Boolean,
    ): PipelineStatusInfo {
        val result = mcpClientWrapper.callTool(
            toolName = "create_pipeline",
            arguments = mapOf(
                "query" to query,
                "filename" to filename,
                "per_page" to perPage.toString(),
                "search_enabled" to searchEnabled.toString(),
                "summarize_enabled" to summarizeEnabled.toString(),
                "save_to_file_enabled" to saveToFileEnabled.toString(),
                "send_to_telegram_enabled" to telegramEnabled.toString(),
            ),
        )

        val obj = extractJsonObject(result)
        return parsePipeline(obj)
    }

    /**
     * Gets the current status of a pipeline.
     */
    suspend fun getPipelineStatus(pipelineId: String): PipelineStatusInfo {
        val result = mcpClientWrapper.callTool(
            toolName = "get_pipeline_status",
            arguments = mapOf("pipeline_id" to pipelineId),
        )

        val obj = extractJsonObject(result)
        return parsePipeline(obj)
    }

    /**
     * Cancels a running pipeline.
     */
    suspend fun cancelPipeline(pipelineId: String) {
        mcpClientWrapper.callTool(
            toolName = "cancel_pipeline",
            arguments = mapOf("pipeline_id" to pipelineId),
        )
    }

    private fun extractJsonObject(text: String): JsonObject {
        val jsonStart = text.indexOf('{')
        val jsonStr = if (jsonStart >= 0) text.substring(jsonStart) else text
        return json.parseToJsonElement(jsonStr.trim()).jsonObject
    }

    private fun parsePipeline(obj: JsonObject): PipelineStatusInfo {
        val steps = obj["steps"]?.jsonArray?.map { element ->
            val stepObj = element.jsonObject
            PipelineStepInfo(
                step = stepObj["step"]?.jsonPrimitive?.content.orEmpty(),
                status = stepObj["status"]?.jsonPrimitive?.content.orEmpty(),
                result = stepObj["result"]?.jsonPrimitive?.content,
                error = stepObj["error"]?.jsonPrimitive?.content,
            )
        } ?: emptyList()

        return PipelineStatusInfo(
            id = obj["id"]?.jsonPrimitive?.content.orEmpty(),
            status = obj["status"]?.jsonPrimitive?.content.orEmpty(),
            steps = steps,
            createdAt = obj["createdAt"]?.jsonPrimitive?.long ?: 0L,
            completedAt = obj["completedAt"]?.jsonPrimitive?.long,
        )
    }
}
