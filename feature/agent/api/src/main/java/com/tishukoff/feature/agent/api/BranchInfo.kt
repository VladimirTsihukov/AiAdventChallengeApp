package com.tishukoff.feature.agent.api

data class BranchInfo(
    val id: String,
    val name: String,
    val checkpointName: String,
    val messageCount: Int,
)
