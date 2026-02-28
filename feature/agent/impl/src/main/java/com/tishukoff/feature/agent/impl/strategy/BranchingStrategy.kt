package com.tishukoff.feature.agent.impl.strategy

import com.tishukoff.core.database.api.ChatMessageRecord
import com.tishukoff.feature.agent.api.BranchInfo
import com.tishukoff.feature.agent.api.CompressionStats
import com.tishukoff.feature.agent.api.LlmSettings
import com.tishukoff.feature.agent.impl.ContextCompressor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

internal data class Checkpoint(
    val id: String,
    val name: String,
    val messageCount: Int,
)

internal data class DialogBranch(
    val id: String,
    val name: String,
    val checkpointId: String,
    val additionalMessages: MutableList<ChatMessageRecord> = mutableListOf(),
)

internal class BranchingStrategy : ContextStrategy {

    private val _checkpoints = MutableStateFlow<Map<Long, List<Checkpoint>>>(emptyMap())
    private val _branches = MutableStateFlow<Map<Long, List<DialogBranch>>>(emptyMap())
    private val _currentBranchId = MutableStateFlow<String?>(null)

    val currentBranchId: Flow<String?> = _currentBranchId.asStateFlow()

    val branches: Flow<List<BranchInfo>> = _branches.map { branchMap ->
        branchMap.values.flatten().map { branch ->
            val checkpoints = _checkpoints.value.values.flatten()
            val checkpoint = checkpoints.find { it.id == branch.checkpointId }
            BranchInfo(
                id = branch.id,
                name = branch.name,
                checkpointName = checkpoint?.name ?: "unknown",
                messageCount = (checkpoint?.messageCount ?: 0) + branch.additionalMessages.size,
            )
        }
    }

    fun createCheckpoint(chatId: Long, name: String, messageCount: Int) {
        val chatCheckpoints = _checkpoints.value[chatId].orEmpty().toMutableList()
        chatCheckpoints.add(
            Checkpoint(
                id = UUID.randomUUID().toString(),
                name = name,
                messageCount = messageCount,
            )
        )
        _checkpoints.value = _checkpoints.value + (chatId to chatCheckpoints)
    }

    fun createBranch(chatId: Long, checkpointId: String, name: String) {
        val chatBranches = _branches.value[chatId].orEmpty().toMutableList()
        val branchId = UUID.randomUUID().toString()
        chatBranches.add(
            DialogBranch(
                id = branchId,
                name = name,
                checkpointId = checkpointId,
            )
        )
        _branches.value = _branches.value + (chatId to chatBranches)
        _currentBranchId.value = branchId
    }

    fun switchBranch(branchId: String) {
        _currentBranchId.value = branchId
    }

    fun getCheckpoints(chatId: Long): List<Checkpoint> {
        return _checkpoints.value[chatId].orEmpty()
    }

    override suspend fun buildContext(
        chatId: Long,
        allMessages: List<ChatMessageRecord>,
        settings: LlmSettings,
    ): StrategyContext {
        val currentBranch = getCurrentBranch(chatId)

        val messagesToSend = if (currentBranch != null) {
            val checkpoint = _checkpoints.value[chatId]
                ?.find { it.id == currentBranch.checkpointId }
            val baseMessages = if (checkpoint != null) {
                allMessages.take(checkpoint.messageCount)
            } else {
                allMessages
            }
            baseMessages + currentBranch.additionalMessages
        } else {
            allMessages
        }

        val originalTokenEstimate = allMessages.sumOf { ContextCompressor.estimateTokens(it.text) }
        val sentTokenEstimate = messagesToSend.sumOf { ContextCompressor.estimateTokens(it.text) }
        val branchCount = _branches.value[chatId]?.size ?: 0

        return StrategyContext(
            systemPromptPrefix = "",
            messagesToSend = messagesToSend,
            stats = CompressionStats(
                isEnabled = true,
                summaryCount = branchCount,
                originalTokenEstimate = originalTokenEstimate,
                compressedTokenEstimate = sentTokenEstimate,
                tokensSaved = (originalTokenEstimate - sentTokenEstimate).coerceAtLeast(0),
                compressionRatio = 0f,
            ),
        )
    }

    fun addMessageToBranch(chatId: Long, message: ChatMessageRecord) {
        val currentBranch = getCurrentBranch(chatId) ?: return
        currentBranch.additionalMessages.add(message)
    }

    private fun getCurrentBranch(chatId: Long): DialogBranch? {
        val branchId = _currentBranchId.value ?: return null
        return _branches.value[chatId]?.find { it.id == branchId }
    }
}
