package com.tishukoff.feature.profile.api.models

enum class PreferredFormat(val displayName: String, val description: String) {
    PLAIN_TEXT("Простой текст", "Обычный текст без форматирования"),
    STRUCTURED("Структурированный", "Списки, заголовки, разделы"),
    CODE_FOCUSED("Код-ориентированный", "Примеры кода, технические детали"),
}
