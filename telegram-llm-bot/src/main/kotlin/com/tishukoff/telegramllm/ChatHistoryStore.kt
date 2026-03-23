package com.tishukoff.telegramllm

import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory хранилище истории диалогов по chatId.
 * Хранит последние [maxMessages] сообщений на каждый чат.
 */
class ChatHistoryStore(
    private val maxMessages: Int = 20,
) {

    private val histories = ConcurrentHashMap<Long, MutableList<Pair<String, String>>>()

    fun addUserMessage(chatId: Long, text: String) {
        getOrCreate(chatId).add("user" to text)
        trimIfNeeded(chatId)
    }

    fun addAssistantMessage(chatId: Long, text: String) {
        getOrCreate(chatId).add("assistant" to text)
        trimIfNeeded(chatId)
    }

    fun getHistory(chatId: Long): List<Pair<String, String>> {
        return histories[chatId]?.toList().orEmpty()
    }

    fun clearHistory(chatId: Long) {
        histories.remove(chatId)
    }

    fun getAllChatIds(): Set<Long> {
        return histories.keys.toSet()
    }

    private fun getOrCreate(chatId: Long): MutableList<Pair<String, String>> {
        return histories.getOrPut(chatId) { mutableListOf() }
    }

    private fun trimIfNeeded(chatId: Long) {
        val history = histories[chatId] ?: return
        while (history.size > maxMessages) {
            history.removeFirst()
        }
    }
}
