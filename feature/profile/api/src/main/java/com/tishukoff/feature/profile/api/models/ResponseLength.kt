package com.tishukoff.feature.profile.api.models

enum class ResponseLength(val displayName: String, val description: String) {
    BRIEF("Краткий", "Только суть, без лишних деталей"),
    MODERATE("Умеренный", "Баланс между краткостью и деталями"),
    DETAILED("Подробный", "Максимально развёрнутые ответы"),
}
