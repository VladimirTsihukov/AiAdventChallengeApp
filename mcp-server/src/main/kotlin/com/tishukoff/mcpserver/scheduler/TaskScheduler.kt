package com.tishukoff.mcpserver.scheduler

import com.tishukoff.mcpserver.GitHubApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TaskScheduler(
    private val storage: TaskStorage,
    private val github: GitHubApiClient,
) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun start() {
        scope.launch {
            println("TaskScheduler started")
            while (true) {
                try {
                    tick()
                } catch (e: Exception) {
                    println("Scheduler tick error: ${e.message}")
                }
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        scope.cancel()
        println("TaskScheduler stopped")
    }

    private suspend fun tick() {
        val now = System.currentTimeMillis()
        val tasks = storage.getTasks()

        for (task in tasks) {
            if (!task.isActive) continue

            if (now >= task.expiresAt) {
                storage.updateTask(task.copy(isActive = false))
                println("Task '${task.name}' expired, deactivated")
                continue
            }

            val intervalMs = task.intervalMinutes * 60_000L
            val lastRun = task.lastRunAt ?: 0L
            if (now - lastRun >= intervalMs) {
                executeTask(task)
            }
        }
    }

    private suspend fun executeTask(task: ScheduledTask) {
        println("Executing task '${task.name}' (tool: ${task.toolName})")
        val now = System.currentTimeMillis()
        val args = task.toolArguments.mapValues { it.value.trim() }

        val result = try {
            val data = when (task.toolName) {
                "get_repository" -> {
                    val owner = args["owner"] ?: ""
                    val repo = args["repo"] ?: ""
                    github.getRepository(owner, repo)
                }
                "search_repositories" -> {
                    val query = args["query"] ?: ""
                    val perPage = args["per_page"]?.toIntOrNull() ?: 5
                    github.searchRepositories(query, perPage)
                }
                "get_file_content" -> {
                    val owner = args["owner"] ?: ""
                    val repo = args["repo"] ?: ""
                    val path = args["path"] ?: ""
                    github.getFileContent(owner, repo, path)
                }
                "get_repository_summary" -> {
                    val owner = args["owner"] ?: ""
                    val repo = args["repo"] ?: ""
                    github.getRepositorySummary(owner, repo)
                }
                else -> """{"error": "Unknown tool: ${task.toolName}"}"""
            }
            TaskExecutionResult(
                taskId = task.id,
                executedAt = now,
                result = data,
                isError = false,
            )
        } catch (e: Exception) {
            TaskExecutionResult(
                taskId = task.id,
                executedAt = now,
                result = e.message ?: "Unknown error",
                isError = true,
            )
        }

        storage.addResult(result)
        storage.updateTask(task.copy(lastRunAt = now))
        println("Task '${task.name}' executed, success=${!result.isError}")
    }

    private companion object {
        const val CHECK_INTERVAL_MS = 30_000L
    }
}
