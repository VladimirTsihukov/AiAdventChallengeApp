package com.tishukoff.feature.rag.impl.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rag_chunks",
    indices = [Index(value = ["strategy"])],
)
internal data class RagChunkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val embedding: ByteArray,
    val source: String,
    val title: String,
    val section: String,
    @ColumnInfo(name = "chunk_index") val chunkIndex: Int,
    val strategy: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RagChunkEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
