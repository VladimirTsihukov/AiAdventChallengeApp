package com.tishukoff.mcpserver

import com.tishukoff.mcpserver.pipeline.PipelineConfig
import com.tishukoff.mcpserver.pipeline.PipelineExecutor
import com.tishukoff.mcpserver.pipeline.PipelineInfo
import com.tishukoff.mcpserver.pipeline.PipelineStatus
import com.tishukoff.mcpserver.pipeline.PipelineStepResult
import com.tishukoff.mcpserver.pipeline.PipelineStepStatus
import com.tishukoff.mcpserver.pipeline.PipelineStepType
import com.tishukoff.mcpserver.pipeline.PipelineStorage
import com.tishukoff.mcpserver.scheduler.ScheduledTask
import com.tishukoff.mcpserver.scheduler.TaskScheduler
import com.tishukoff.mcpserver.scheduler.TaskStorage
import io.ktor.server.netty.Netty
import io.ktor.server.engine.embeddedServer
import java.io.File
import java.util.Properties
import java.util.UUID
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun main() {
    val env = loadEnv()
    val port = (env["PORT"] ?: System.getenv("PORT"))?.toIntOrNull() ?: 3000
    val githubToken = env["GITHUB_TOKEN"] ?: System.getenv("GITHUB_TOKEN")

    val telegramBotToken = env["TELEGRAM_BOT_TOKEN"] ?: System.getenv("TELEGRAM_BOT_TOKEN")
    val telegramChatId = env["TELEGRAM_CHAT_ID"] ?: System.getenv("TELEGRAM_CHAT_ID")
    val telegram = if (!telegramBotToken.isNullOrBlank() && !telegramChatId.isNullOrBlank()) {
        TelegramClient(botToken = telegramBotToken, chatId = telegramChatId)
    } else {
        println("WARNING: TELEGRAM_BOT_TOKEN or TELEGRAM_CHAT_ID not set — Telegram step will be unavailable")
        null
    }

    val github = GitHubApiClient(token = githubToken)
    val storage = TaskStorage()
    val scheduler = TaskScheduler(storage, github)
    val pipelineStorage = PipelineStorage()
    val pipelineExecutor = PipelineExecutor(pipelineStorage, github, telegram)

    scheduler.start()

    Runtime.getRuntime().addShutdownHook(Thread { scheduler.stop() })

    println("Starting GitHub MCP Server on port $port...")

    embeddedServer(Netty, host = "0.0.0.0", port = port) {
        mcp {
            createMcpServer(github, storage, pipelineStorage, pipelineExecutor)
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

private val prettyJson = Json { prettyPrint = true }

fun createMcpServer(
    github: GitHubApiClient,
    storage: TaskStorage,
    pipelineStorage: PipelineStorage,
    pipelineExecutor: PipelineExecutor,
): Server {
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

    server.addTool(
        name = "get_repository_summary",
        description = "Get a compact summary of a GitHub repository (name, stars, forks, language, description). Lightweight alternative to get_repository.",
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

        val result = github.getRepositorySummary(owner, repo)
        CallToolResult(content = listOf(TextContent(result)))
    }

    // --- Scheduler tools ---

    server.addTool(
        name = "create_scheduled_task",
        description = "Create a scheduled task that periodically calls a GitHub tool. The task will auto-expire after the specified duration.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("name") {
                    put("type", "string")
                    put("description", "Task name (e.g. 'Track Kotlin repo stars')")
                }
                putJsonObject("tool_name") {
                    put("type", "string")
                    put("description", "Tool to call: get_repository, search_repositories, or get_file_content")
                }
                putJsonObject("tool_arguments") {
                    put("type", "string")
                    put("description", "JSON string with tool arguments, e.g. {\"owner\":\"JetBrains\",\"repo\":\"kotlin\"}")
                }
                putJsonObject("interval_minutes") {
                    put("type", "integer")
                    put("description", "How often to run (in minutes). E.g. 1, 60, 1440")
                }
                putJsonObject("duration_minutes") {
                    put("type", "integer")
                    put("description", "How long the task stays active (in minutes). E.g. 60, 1440, 10080")
                }
            },
            required = listOf("name", "tool_name", "tool_arguments", "interval_minutes", "duration_minutes"),
        ),
    ) { request ->
        val name = request.arguments?.get("name")?.jsonPrimitive?.content
        val toolName = request.arguments?.get("tool_name")?.jsonPrimitive?.content
        val toolArgsJson = request.arguments?.get("tool_arguments")?.jsonPrimitive?.content
        val intervalMinutes = request.arguments?.get("interval_minutes")?.jsonPrimitive?.content?.toIntOrNull()
        val durationMinutes = request.arguments?.get("duration_minutes")?.jsonPrimitive?.content?.toIntOrNull()

        if (name.isNullOrBlank() || toolName.isNullOrBlank() || toolArgsJson.isNullOrBlank()
            || intervalMinutes == null || durationMinutes == null
        ) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Error: all parameters are required")),
                isError = true,
            )
        }

        val validTools = listOf("get_repository", "get_repository_summary", "search_repositories", "get_file_content")
        if (toolName !in validTools) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Error: tool_name must be one of: $validTools")),
                isError = true,
            )
        }

        val toolArguments = try {
            Json.decodeFromString<Map<String, String>>(toolArgsJson)
        } catch (e: Exception) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Error: tool_arguments must be a valid JSON object: ${e.message}")),
                isError = true,
            )
        }

        val now = System.currentTimeMillis()
        val task = ScheduledTask(
            id = UUID.randomUUID().toString(),
            name = name,
            toolName = toolName,
            toolArguments = toolArguments,
            intervalMinutes = intervalMinutes,
            durationMinutes = durationMinutes,
            createdAt = now,
            expiresAt = now + durationMinutes * 60_000L,
        )

        storage.addTask(task)

        val response = prettyJson.encodeToString(ScheduledTask.serializer(), task)
        CallToolResult(content = listOf(TextContent("Task created:\n$response")))
    }

    server.addTool(
        name = "list_scheduled_tasks",
        description = "List all scheduled tasks with their status.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {},
            required = emptyList(),
        ),
    ) { _ ->
        val tasks = storage.getTasks()
        if (tasks.isEmpty()) {
            return@addTool CallToolResult(content = listOf(TextContent("No scheduled tasks")))
        }

        val response = prettyJson.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(ScheduledTask.serializer()),
            tasks,
        )
        CallToolResult(content = listOf(TextContent(response)))
    }

    server.addTool(
        name = "delete_scheduled_task",
        description = "Delete a scheduled task by ID.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("task_id") {
                    put("type", "string")
                    put("description", "Task ID to delete")
                }
            },
            required = listOf("task_id"),
        ),
    ) { request ->
        val taskId = request.arguments?.get("task_id")?.jsonPrimitive?.content

        if (taskId.isNullOrBlank()) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Error: 'task_id' is required")),
                isError = true,
            )
        }

        val tasks = storage.getTasks()
        if (tasks.none { it.id == taskId }) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Error: task '$taskId' not found")),
                isError = true,
            )
        }

        storage.removeTask(taskId)
        CallToolResult(content = listOf(TextContent("Task '$taskId' deleted")))
    }

    server.addTool(
        name = "toggle_scheduled_task",
        description = "Enable or disable a scheduled task.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("task_id") {
                    put("type", "string")
                    put("description", "Task ID to toggle")
                }
                putJsonObject("is_active") {
                    put("type", "boolean")
                    put("description", "true to enable, false to disable")
                }
            },
            required = listOf("task_id", "is_active"),
        ),
    ) { request ->
        val taskId = request.arguments?.get("task_id")?.jsonPrimitive?.content
        val isActive = request.arguments?.get("is_active")?.jsonPrimitive?.content?.toBooleanStrictOrNull()

        if (taskId.isNullOrBlank() || isActive == null) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Error: 'task_id' and 'is_active' are required")),
                isError = true,
            )
        }

        val tasks = storage.getTasks()
        val task = tasks.find { it.id == taskId }
        if (task == null) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Error: task '$taskId' not found")),
                isError = true,
            )
        }

        storage.updateTask(task.copy(isActive = isActive))
        val status = if (isActive) "enabled" else "disabled"
        CallToolResult(content = listOf(TextContent("Task '${task.name}' $status")))
    }

    server.addTool(
        name = "get_task_results",
        description = "Get execution results for a scheduled task.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("task_id") {
                    put("type", "string")
                    put("description", "Task ID")
                }
                putJsonObject("limit") {
                    put("type", "integer")
                    put("description", "Max number of results to return (default 10)")
                }
            },
            required = listOf("task_id"),
        ),
    ) { request ->
        val taskId = request.arguments?.get("task_id")?.jsonPrimitive?.content
        val limit = request.arguments?.get("limit")?.jsonPrimitive?.content?.toIntOrNull() ?: 10

        if (taskId.isNullOrBlank()) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Error: 'task_id' is required")),
                isError = true,
            )
        }

        val results = storage.getResults(taskId, limit)
        if (results.isEmpty()) {
            return@addTool CallToolResult(
                content = listOf(TextContent("No results yet for task '$taskId'")),
            )
        }

        val response = prettyJson.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(
                com.tishukoff.mcpserver.scheduler.TaskExecutionResult.serializer()
            ),
            results,
        )
        CallToolResult(content = listOf(TextContent(response)))
    }

    // --- Pipeline tools ---

    server.addTool(
        name = "create_pipeline",
        description = "Create and run a pipeline that chains steps: search → summarize → save_to_file → send_to_telegram. Each step can be enabled/disabled. The pipeline runs in background on the server.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("query") {
                    put("type", "string")
                    put("description", "Search query for GitHub repositories")
                }
                putJsonObject("filename") {
                    put("type", "string")
                    put("description", "Output filename for save_to_file step (default: pipeline_result.txt)")
                }
                putJsonObject("per_page") {
                    put("type", "integer")
                    put("description", "Number of search results (1-10, default 5)")
                }
                putJsonObject("search_enabled") {
                    put("type", "boolean")
                    put("description", "Enable search step (default true)")
                }
                putJsonObject("summarize_enabled") {
                    put("type", "boolean")
                    put("description", "Enable summarize step (default true)")
                }
                putJsonObject("save_to_file_enabled") {
                    put("type", "boolean")
                    put("description", "Enable save_to_file step (default true)")
                }
                putJsonObject("send_to_telegram_enabled") {
                    put("type", "boolean")
                    put("description", "Enable send_to_telegram step (default false)")
                }
            },
            required = listOf("query"),
        ),
    ) { request ->
        val query = request.arguments?.get("query")?.jsonPrimitive?.content
        if (query.isNullOrBlank()) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Error: 'query' is required")),
                isError = true,
            )
        }

        val filename = request.arguments?.get("filename")?.jsonPrimitive?.content ?: "pipeline_result.txt"
        val perPage = request.arguments?.get("per_page")?.jsonPrimitive?.content?.toIntOrNull() ?: 5
        val searchEnabled = request.arguments?.get("search_enabled")?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: true
        val summarizeEnabled = request.arguments?.get("summarize_enabled")?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: true
        val saveToFileEnabled = request.arguments?.get("save_to_file_enabled")?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: true
        val sendToTelegramEnabled = request.arguments?.get("send_to_telegram_enabled")?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false

        if (!searchEnabled && !summarizeEnabled && !saveToFileEnabled && !sendToTelegramEnabled) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Error: at least one step must be enabled")),
                isError = true,
            )
        }

        val config = PipelineConfig(
            searchEnabled = searchEnabled,
            summarizeEnabled = summarizeEnabled,
            saveToFileEnabled = saveToFileEnabled,
            sendToTelegramEnabled = sendToTelegramEnabled,
            query = query,
            filename = filename,
            perPage = perPage.coerceIn(1, 10),
        )

        val steps = buildList {
            add(PipelineStepResult(
                step = PipelineStepType.SEARCH.name,
                status = if (searchEnabled) PipelineStepStatus.PENDING.name else PipelineStepStatus.SKIPPED.name,
            ))
            add(PipelineStepResult(
                step = PipelineStepType.SUMMARIZE.name,
                status = if (summarizeEnabled) PipelineStepStatus.PENDING.name else PipelineStepStatus.SKIPPED.name,
            ))
            add(PipelineStepResult(
                step = PipelineStepType.SAVE_TO_FILE.name,
                status = if (saveToFileEnabled) PipelineStepStatus.PENDING.name else PipelineStepStatus.SKIPPED.name,
            ))
            add(PipelineStepResult(
                step = PipelineStepType.SEND_TO_TELEGRAM.name,
                status = if (sendToTelegramEnabled) PipelineStepStatus.PENDING.name else PipelineStepStatus.SKIPPED.name,
            ))
        }

        val pipeline = PipelineInfo(
            id = UUID.randomUUID().toString(),
            config = config,
            status = PipelineStatus.RUNNING.name,
            steps = steps,
            createdAt = System.currentTimeMillis(),
        )

        pipelineStorage.save(pipeline)
        pipelineExecutor.execute(pipeline)

        val response = prettyJson.encodeToString(PipelineInfo.serializer(), pipeline)
        CallToolResult(content = listOf(TextContent(response)))
    }

    server.addTool(
        name = "get_pipeline_status",
        description = "Get the current status of a pipeline, including progress of each step.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("pipeline_id") {
                    put("type", "string")
                    put("description", "Pipeline ID")
                }
            },
            required = listOf("pipeline_id"),
        ),
    ) { request ->
        val pipelineId = request.arguments?.get("pipeline_id")?.jsonPrimitive?.content
        if (pipelineId.isNullOrBlank()) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Error: 'pipeline_id' is required")),
                isError = true,
            )
        }

        val pipeline = pipelineStorage.get(pipelineId)
        if (pipeline == null) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Error: pipeline '$pipelineId' not found")),
                isError = true,
            )
        }

        val response = prettyJson.encodeToString(PipelineInfo.serializer(), pipeline)
        CallToolResult(content = listOf(TextContent(response)))
    }

    server.addTool(
        name = "cancel_pipeline",
        description = "Cancel a running pipeline.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("pipeline_id") {
                    put("type", "string")
                    put("description", "Pipeline ID to cancel")
                }
            },
            required = listOf("pipeline_id"),
        ),
    ) { request ->
        val pipelineId = request.arguments?.get("pipeline_id")?.jsonPrimitive?.content
        if (pipelineId.isNullOrBlank()) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Error: 'pipeline_id' is required")),
                isError = true,
            )
        }

        val pipeline = pipelineStorage.get(pipelineId)
        if (pipeline == null) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Error: pipeline '$pipelineId' not found")),
                isError = true,
            )
        }

        pipelineExecutor.cancel(pipelineId)
        CallToolResult(content = listOf(TextContent("Pipeline '$pipelineId' cancelled")))
    }

    return server
}
