package com.tishukoff.feature.profile.api.models

enum class ResponseStyle(val displayName: String, val description: String) {
    FORMAL("Формальный", "Профессиональный и деловой тон"),
    CASUAL("Неформальный", "Дружелюбный и расслабленный тон"),
    FRIENDLY("Дружеский", "Тёплый и поддерживающий тон"),
}
