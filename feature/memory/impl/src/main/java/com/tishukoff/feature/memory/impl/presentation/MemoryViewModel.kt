package com.tishukoff.feature.memory.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tishukoff.core.database.api.ChatHistoryStorage
import com.tishukoff.feature.agent.api.Agent
import com.tishukoff.feature.memory.api.MemoryEntry
import com.tishukoff.feature.memory.api.MemoryManager
import com.tishukoff.feature.memory.api.MemoryType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@Suppress("OPT_IN_USAGE")
class MemoryViewModel(
    private val agent: Agent,
    private val memoryManager: MemoryManager,
    private val chatHistoryStorage: ChatHistoryStorage,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemoryUiState())
    val uiState: StateFlow<MemoryUiState> = _uiState.asStateFlow()

    init {
        agent.currentChatId
            .onEach { chatId ->
                _uiState.value = _uiState.value.copy(currentChatId = chatId)
            }
            .launchIn(viewModelScope)

        agent.currentChatId
            .flatMapLatest { chatId ->
                if (chatId == null) flowOf(emptyList())
                else chatHistoryStorage.getByChatId(chatId).map { records ->
                    records.map { record ->
                        MemoryEntry(
                            key = if (record.isUser) "User" else "Assistant",
                            value = record.text,
                            type = MemoryType.SHORT_TERM,
                            chatId = chatId,
                        )
                    }
                }
            }
            .onEach { entries ->
                _uiState.value = _uiState.value.copy(shortTermEntries = entries)
            }
            .launchIn(viewModelScope)

        agent.currentChatId
            .flatMapLatest { chatId ->
                if (chatId == null) flowOf(emptyList())
                else memoryManager.getWorkingMemory(chatId)
            }
            .onEach { entries ->
                _uiState.value = _uiState.value.copy(workingEntries = entries)
            }
            .launchIn(viewModelScope)

        memoryManager.getLongTermMemory()
            .onEach { entries ->
                _uiState.value = _uiState.value.copy(longTermEntries = entries)
            }
            .launchIn(viewModelScope)
    }

    fun selectTab(tab: MemoryType) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun deleteWorkingEntry(key: String) {
        val chatId = _uiState.value.currentChatId ?: return
        viewModelScope.launch {
            memoryManager.deleteWorkingMemoryEntry(chatId, key)
        }
    }

    fun deleteLongTermEntry(id: Long) {
        viewModelScope.launch {
            memoryManager.deleteLongTermMemoryEntry(id)
        }
    }
}
