package com.tishukoff.mcpserver.pipeline

import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory storage for pipeline state.
 */
class PipelineStorage {

    private val pipelines = ConcurrentHashMap<String, PipelineInfo>()

    fun get(id: String): PipelineInfo? = pipelines[id]

    fun getAll(): List<PipelineInfo> = pipelines.values.toList()

    fun save(pipeline: PipelineInfo) {
        pipelines[pipeline.id] = pipeline
    }

    fun remove(id: String): Boolean = pipelines.remove(id) != null
}
