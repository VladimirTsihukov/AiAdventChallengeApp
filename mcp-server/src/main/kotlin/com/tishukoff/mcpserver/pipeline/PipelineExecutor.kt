package com.tishukoff.mcpserver.pipeline

import com.tishukoff.mcpserver.GitHubApiClient
import com.tishukoff.mcpserver.TelegramClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * Executes pipeline steps in background.
 * Each step runs sequentially; results pass from one step to the next.
 */
class PipelineExecutor(
    private val storage: PipelineStorage,
    private val github: GitHubApiClient,
    private val telegram: TelegramClient?,
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true }
    private val cancelledIds = mutableSetOf<String>()

    fun execute(pipeline: PipelineInfo) {
        scope.launch {
            runPipeline(pipeline)
        }
    }

    fun cancel(pipelineId: String) {
        cancelledIds.add(pipelineId)
        val pipeline = storage.get(pipelineId) ?: return
        if (pipeline.status == PipelineStatus.RUNNING.name) {
            storage.save(
                pipeline.copy(
                    status = PipelineStatus.CANCELLED.name,
                    completedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    private suspend fun runPipeline(pipeline: PipelineInfo) {
        var current = pipeline
        var previousStepData = ""
        var contentForTelegram = ""

        println("Pipeline ${pipeline.id}: config = ${pipeline.config}")

        val enabledSteps = buildList {
            if (current.config.searchEnabled) add(PipelineStepType.SEARCH)
            if (current.config.summarizeEnabled) add(PipelineStepType.SUMMARIZE)
            if (current.config.saveToFileEnabled) add(PipelineStepType.SAVE_TO_FILE)
            if (current.config.sendToTelegramEnabled) add(PipelineStepType.SEND_TO_TELEGRAM)
        }

        for (stepType in enabledSteps) {
            if (cancelledIds.remove(current.id)) return

            current = updateStepStatus(current, stepType.name, PipelineStepStatus.IN_PROGRESS)
            storage.save(current)
            delay(STEP_DELAY_MS)

            try {
                val result = when (stepType) {
                    PipelineStepType.SEARCH -> executeSearch(current.config)
                    PipelineStepType.SUMMARIZE -> executeSummarize(previousStepData)
                    PipelineStepType.SAVE_TO_FILE -> executeSaveToFile(previousStepData, current.config.filename)
                    PipelineStepType.SEND_TO_TELEGRAM -> executeSendToTelegram(contentForTelegram)
                }
                if (stepType == PipelineStepType.SUMMARIZE || stepType == PipelineStepType.SEARCH) {
                    contentForTelegram = result
                }
                previousStepData = result
                current = updateStepResult(current, stepType.name, PipelineStepStatus.COMPLETED, result = result)
                storage.save(current)
            } catch (e: Exception) {
                current = updateStepResult(
                    current, stepType.name, PipelineStepStatus.ERROR, error = e.message ?: "Unknown error"
                )
                current = current.copy(
                    status = PipelineStatus.ERROR.name,
                    completedAt = System.currentTimeMillis(),
                )
                storage.save(current)
                return
            }
        }

        current = current.copy(
            status = PipelineStatus.COMPLETED.name,
            completedAt = System.currentTimeMillis(),
        )
        storage.save(current)
    }

    private suspend fun executeSearch(config: PipelineConfig): String {
        val rawResult = github.searchRepositories(config.query, config.perPage)
        return rawResult
    }

    private fun executeSummarize(searchData: String): String {
        if (searchData.isBlank()) return "No data to summarize"

        return try {
            val root = json.parseToJsonElement(searchData).jsonObject
            val items = root["items"]?.jsonArray ?: return "No repositories found"

            buildString {
                appendLine("Found ${items.size} repositories for the search query:\n")
                items.forEachIndexed { index, element ->
                    val repo = element.jsonObject
                    val name = repo["full_name"]?.jsonPrimitive?.content ?: "unknown"
                    val description = repo["description"]?.jsonPrimitive?.content ?: "No description"
                    val stars = repo["stargazers_count"]?.jsonPrimitive?.content ?: "0"
                    val language = repo["language"]?.jsonPrimitive?.content ?: "N/A"
                    val url = repo["html_url"]?.jsonPrimitive?.content ?: ""

                    appendLine("${index + 1}. $name")
                    appendLine("   Stars: $stars | Language: $language")
                    appendLine("   $description")
                    appendLine("   URL: $url")
                    appendLine()
                }
            }.trimEnd()
        } catch (e: Exception) {
            "Summary of raw data:\n${searchData.take(1000)}"
        }
    }

    private suspend fun executeSendToTelegram(data: String): String {
        println("Pipeline: executeSendToTelegram called, data length = ${data.length}, telegram = $telegram")
        val client = telegram ?: error("Telegram is not configured (missing TELEGRAM_BOT_TOKEN or TELEGRAM_CHAT_ID)")
        if (data.isBlank()) return "No data to send"
        val result = client.sendMessage(data)
        println("Pipeline: Telegram result = $result")
        return result
    }

    private fun executeSaveToFile(data: String, filename: String): String {
        val outputDir = File("pipeline_output")
        if (!outputDir.exists()) outputDir.mkdirs()

        val file = File(outputDir, filename)
        file.writeText(data)
        return "Saved to ${file.absolutePath} (${data.length} chars)"
    }

    private companion object {
        const val STEP_DELAY_MS = 2_000L
    }

    private fun updateStepStatus(
        pipeline: PipelineInfo,
        stepName: String,
        status: PipelineStepStatus,
    ): PipelineInfo {
        val updatedSteps = pipeline.steps.map { step ->
            if (step.step == stepName) step.copy(status = status.name)
            else step
        }
        return pipeline.copy(steps = updatedSteps)
    }

    private fun updateStepResult(
        pipeline: PipelineInfo,
        stepName: String,
        status: PipelineStepStatus,
        result: String? = null,
        error: String? = null,
    ): PipelineInfo {
        val updatedSteps = pipeline.steps.map { step ->
            if (step.step == stepName) step.copy(status = status.name, result = result, error = error)
            else step
        }
        return pipeline.copy(steps = updatedSteps)
    }
}
