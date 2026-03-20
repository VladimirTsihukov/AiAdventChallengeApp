package com.tishukoff.feature.rag.impl.domain.model

/**
 * Шаг сценарного бенчмарка — один вопрос в связном диалоге.
 */
data class BenchmarkScenarioStep(
    val question: String,
    val expectedKeywords: List<String>,
    val expectedSource: String,
)

/**
 * Сценарный бенчмарк — связный диалог из нескольких шагов.
 */
data class BenchmarkScenario(
    val name: String,
    val steps: List<BenchmarkScenarioStep>,
)

val benchmarkScenario = BenchmarkScenario(
    name = "Онбординг нового сотрудника",
    steps = listOf(
        BenchmarkScenarioStep(
            question = "Я новый сотрудник в Zephyra. С чего мне начать?",
            expectedKeywords = listOf("онбординг", "испытательный"),
            expectedSource = "onboarding_guide.md",
        ),
        BenchmarkScenarioStep(
            question = "Какой у меня будет испытательный срок?",
            expectedKeywords = listOf("3 месяца"),
            expectedSource = "onboarding_guide.md",
        ),
        BenchmarkScenarioStep(
            question = "А какие инструменты мне нужно будет установить?",
            expectedKeywords = listOf("zephyr-cli"),
            expectedSource = "internal_tools.md",
        ),
        BenchmarkScenarioStep(
            question = "Как подключиться к VPN?",
            expectedKeywords = listOf("vpn connect"),
            expectedSource = "internal_tools.md",
        ),
        BenchmarkScenarioStep(
            question = "Хорошо, а какая у меня будет зарплата как у Junior?",
            expectedKeywords = listOf("Junior"),
            expectedSource = "salary_and_benefits.md",
        ),
        BenchmarkScenarioStep(
            question = "Сколько дней отпуска мне положено?",
            expectedKeywords = listOf("28"),
            expectedSource = "salary_and_benefits.md",
        ),
        BenchmarkScenarioStep(
            question = "Расскажи подробнее о компании — кто её основал?",
            expectedKeywords = listOf("Марк Виренко", "Алиса Штормберг"),
            expectedSource = "company_zephyra.md",
        ),
        BenchmarkScenarioStep(
            question = "Чем именно занимается компания?",
            expectedKeywords = listOf("дрон", "агро"),
            expectedSource = "company_zephyra.md",
        ),
        BenchmarkScenarioStep(
            question = "Какой основной проект сейчас ведётся?",
            expectedKeywords = listOf("Нимбус", "Nimbus"),
            expectedSource = "project_nimbus.md",
        ),
        BenchmarkScenarioStep(
            question = "Напомни, какие ограничения по Pull Request в этом проекте?",
            expectedKeywords = listOf("400"),
            expectedSource = "project_nimbus.md",
        ),
        BenchmarkScenarioStep(
            question = "Подведи итог: что мне как новому сотруднику важно запомнить?",
            expectedKeywords = listOf("испытательный", "VPN", "Нимбус", "Nimbus"),
            expectedSource = "onboarding_guide.md",
        ),
    ),
)
