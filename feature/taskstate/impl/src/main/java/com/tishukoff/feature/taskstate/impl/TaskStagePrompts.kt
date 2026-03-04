package com.tishukoff.feature.taskstate.impl

internal object TaskStagePrompts {

    fun planning(): String =
        "Ты планировщик задач. Проанализируй задачу и создай детальный пошаговый план выполнения. " +
            "Выдай пронумерованный список шагов."

    fun execution(planningResult: String): String =
        "Ты исполнитель. Тебе дан план:\n$planningResult\n\n" +
            "Выполни каждый шаг и предоставь полный результат."

    fun validation(taskDescription: String, executionResult: String): String =
        "Ты ревьюер. Исходная задача: $taskDescription\n\n" +
            "Результат выполнения:\n$executionResult\n\n" +
            "Оцени качество, укажи проблемы или подтверди корректность."
}
