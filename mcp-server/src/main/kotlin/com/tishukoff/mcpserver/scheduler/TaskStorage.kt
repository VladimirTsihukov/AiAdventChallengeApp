package com.tishukoff.mcpserver.scheduler

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File

class TaskStorage(
    private val filePath: String = "scheduled_tasks.json",
) {

    private val mutex = Mutex()
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun loadAll(): TaskStoreData = mutex.withLock {
        val file = File(filePath)
        if (!file.exists()) return@withLock TaskStoreData()
        return@withLock try {
            json.decodeFromString<TaskStoreData>(file.readText())
        } catch (e: Exception) {
            println("Failed to load tasks: ${e.message}")
            TaskStoreData()
        }
    }

    suspend fun saveAll(data: TaskStoreData): Unit = mutex.withLock {
        File(filePath).writeText(json.encodeToString(TaskStoreData.serializer(), data))
    }

    suspend fun addTask(task: ScheduledTask) {
        val data = loadAll()
        saveAll(data.copy(tasks = data.tasks + task))
    }

    suspend fun removeTask(taskId: String) {
        val data = loadAll()
        saveAll(
            data.copy(
                tasks = data.tasks.filter { it.id != taskId },
                results = data.results.filter { it.taskId != taskId },
            )
        )
    }

    suspend fun updateTask(task: ScheduledTask) {
        val data = loadAll()
        saveAll(
            data.copy(
                tasks = data.tasks.map { if (it.id == task.id) task else it }
            )
        )
    }

    suspend fun getTasks(): List<ScheduledTask> = loadAll().tasks

    suspend fun getResults(taskId: String, limit: Int = 10): List<TaskExecutionResult> =
        loadAll().results
            .filter { it.taskId == taskId }
            .sortedByDescending { it.executedAt }
            .take(limit)

    suspend fun addResult(result: TaskExecutionResult) {
        val data = loadAll()
        saveAll(data.copy(results = data.results + result))
    }
}
