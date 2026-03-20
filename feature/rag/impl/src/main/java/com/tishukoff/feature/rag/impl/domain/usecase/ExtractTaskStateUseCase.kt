package com.tishukoff.feature.rag.impl.domain.usecase

import com.tishukoff.feature.rag.impl.domain.model.TaskState
import com.tishukoff.feature.rag.impl.domain.repository.LlmClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Извлекает и обновляет состояние задачи на основе истории диалога.
 * Вызывает LLM для анализа диалога и формирования task state.
 */
class ExtractTaskStateUseCase(
    private val llmClient: LlmClient,
) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * @param conversationHistory история диалога в формате "User: ..\nAssistant: .."
     * @param currentState текущее состояние задачи (может быть пустым)
     * @return обновлённый [TaskState]
     */
    suspend operator fun invoke(
        conversationHistory: String,
        currentState: TaskState,
    ): Result<TaskState> = runCatching {
        val prompt = buildString {
            append("Проанализируй диалог и извлеки текущее состояние задачи.\n\n")
            if (!currentState.isEmpty) {
                append("Текущее состояние задачи:\n")
                append(currentState.toPromptBlock())
                append("\n")
            }
            append("Диалог:\n$conversationHistory\n\n")
            append("Верни ТОЛЬКО JSON (без markdown-обёртки) в формате:\n")
            append("{\n")
            append("  \"goal\": \"<цель диалога — чего хочет добиться пользователь>\",\n")
            append("  \"clarifications\": [\"<что пользователь уже уточнил>\"],\n")
            append("  \"constraints\": [\"<ограничения и условия>\"],\n")
            append("  \"terms\": {\"<термин>\": \"<определение>\"}\n")
            append("}\n\n")
            append("Правила:\n")
            append("- goal: краткая формулировка цели диалога\n")
            append("- clarifications: список уточнений, которые сделал пользователь\n")
            append("- constraints: ограничения, которые пользователь зафиксировал\n")
            append("- terms: ключевые термины с определениями из контекста диалога\n")
            append("- Сохраняй информацию из предыдущего состояния, дополняя новой\n")
            append("- Если диалог только начался и данных мало, оставь пустые списки\n")
        }

        val response = llmClient.complete(prompt)
        parseTaskState(response)
    }

    private fun parseTaskState(raw: String): TaskState {
        val trimmed = raw.trim().let { text ->
            when {
                text.startsWith("```json") -> text.removePrefix("```json").removeSuffix("```").trim()
                text.startsWith("```") -> text.removePrefix("```").removeSuffix("```").trim()
                else -> text
            }
        }

        val jsonObj = json.decodeFromString<JsonObject>(trimmed)

        val goal = jsonObj["goal"]?.jsonPrimitive?.content.orEmpty()

        val clarifications = jsonObj["clarifications"]?.jsonArray?.mapNotNull {
            it.jsonPrimitive.content.takeIf { s -> s.isNotBlank() }
        }.orEmpty()

        val constraints = jsonObj["constraints"]?.jsonArray?.mapNotNull {
            it.jsonPrimitive.content.takeIf { s -> s.isNotBlank() }
        }.orEmpty()

        val terms = jsonObj["terms"]?.jsonObject?.entries?.associate { (k, v) ->
            k to v.jsonPrimitive.content
        }.orEmpty()

        return TaskState(
            goal = goal,
            clarifications = clarifications,
            constraints = constraints,
            terms = terms,
        )
    }
}
