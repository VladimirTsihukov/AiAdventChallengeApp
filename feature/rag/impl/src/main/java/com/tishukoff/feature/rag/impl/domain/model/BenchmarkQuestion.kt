package com.tishukoff.feature.rag.impl.domain.model

/**
 * Контрольный вопрос для бенчмарка RAG-агента.
 *
 * @param question текст вопроса
 * @param expectedKeywords ключевые слова/фразы, которые должны присутствовать в ответе
 * @param expectedSource ожидаемый файл-источник
 */
data class BenchmarkQuestion(
    val question: String,
    val expectedKeywords: List<String>,
    val expectedSource: String,
)

/**
 * Результат проверки одного вопроса.
 */
data class BenchmarkQuestionResult(
    val question: String,
    val answer: String,
    val expectedKeywords: List<String>,
    val foundKeywords: List<String>,
    val passed: Boolean,
)

/**
 * Итоговый результат бенчмарка.
 */
data class BenchmarkResult(
    val results: List<BenchmarkQuestionResult>,
) {
    val passedCount: Int get() = results.count { it.passed }
    val totalCount: Int get() = results.size
}

val benchmarkQuestions = listOf(
    BenchmarkQuestion(
        question = "Кто основал компанию Zephyra Technologies?",
        expectedKeywords = listOf("Марк Виренко", "Алиса Штормберг"),
        expectedSource = "company_zephyra.md",
    ),
    BenchmarkQuestion(
        question = "Сколько сотрудников работает в Zephyra?",
        expectedKeywords = listOf("87"),
        expectedSource = "company_zephyra.md",
    ),
    BenchmarkQuestion(
        question = "Что произошло во время инцидента «Ледяной шторм»?",
        expectedKeywords = listOf("влажности", "полив"),
        expectedSource = "project_nimbus.md",
    ),
    BenchmarkQuestion(
        question = "Какой испытательный срок в компании?",
        expectedKeywords = listOf("3 месяца"),
        expectedSource = "onboarding_guide.md",
    ),
    BenchmarkQuestion(
        question = "Какая зарплата у Senior-разработчика?",
        expectedKeywords = listOf("210 000", "210000"),
        expectedSource = "salary_and_benefits.md",
    ),
    BenchmarkQuestion(
        question = "Как подключиться к VPN компании?",
        expectedKeywords = listOf("zephyr-cli vpn connect"),
        expectedSource = "internal_tools.md",
    ),
    BenchmarkQuestion(
        question = "Что такое дрон Пчёлка-7?",
        expectedKeywords = listOf("дрон", "опылитель"),
        expectedSource = "company_zephyra.md",
    ),
    BenchmarkQuestion(
        question = "Какие базы данных использует система Нимбус?",
        expectedKeywords = listOf("CrystalDB", "PostgreSQL", "Redis"),
        expectedSource = "project_nimbus.md",
    ),
    BenchmarkQuestion(
        question = "Сколько дней отпуска положено сотрудникам?",
        expectedKeywords = listOf("28"),
        expectedSource = "salary_and_benefits.md",
    ),
    BenchmarkQuestion(
        question = "Какое максимальное ограничение на размер Pull Request?",
        expectedKeywords = listOf("400"),
        expectedSource = "project_nimbus.md",
    ),
)
