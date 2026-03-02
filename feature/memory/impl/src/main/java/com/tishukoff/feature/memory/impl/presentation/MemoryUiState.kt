package com.tishukoff.feature.memory.impl.presentation

import com.tishukoff.feature.memory.api.MemoryEntry
import com.tishukoff.feature.memory.api.MemoryType

data class MemoryUiState(
    val selectedTab: MemoryType = MemoryType.SHORT_TERM,
    val shortTermEntries: List<MemoryEntry> = emptyList(),
    val workingEntries: List<MemoryEntry> = emptyList(),
    val longTermEntries: List<MemoryEntry> = emptyList(),
    val currentChatId: Long? = null,
)
