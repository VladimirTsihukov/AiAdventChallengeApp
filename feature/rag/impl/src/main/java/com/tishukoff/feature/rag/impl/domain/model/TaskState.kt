package com.tishukoff.feature.rag.impl.domain.model

/**
 * Состояние задачи диалога — "память" того, что обсуждается.
 *
 * @param goal цель диалога, определённая из контекста разговора
 * @param clarifications что пользователь уже уточнил
 * @param constraints ограничения и условия, зафиксированные в диалоге
 * @param terms ключевые термины и определения из диалога
 */
data class TaskState(
    val goal: String = "",
    val clarifications: List<String> = emptyList(),
    val constraints: List<String> = emptyList(),
    val terms: Map<String, String> = emptyMap(),
) {
    val isEmpty: Boolean
        get() = goal.isBlank() &&
            clarifications.isEmpty() &&
            constraints.isEmpty() &&
            terms.isEmpty()

    fun toPromptBlock(): String = buildString {
        if (isEmpty) return ""
        appendLine("=== ПАМЯТЬ ЗАДАЧИ ===")
        if (goal.isNotBlank()) appendLine("Цель диалога: $goal")
        if (clarifications.isNotEmpty()) {
            appendLine("Уточнения пользователя:")
            clarifications.forEach { appendLine("- $it") }
        }
        if (constraints.isNotEmpty()) {
            appendLine("Ограничения:")
            constraints.forEach { appendLine("- $it") }
        }
        if (terms.isNotEmpty()) {
            appendLine("Термины:")
            terms.forEach { (k, v) -> appendLine("- $k: $v") }
        }
        appendLine("=== КОНЕЦ ПАМЯТИ ЗАДАЧИ ===")
    }
}
