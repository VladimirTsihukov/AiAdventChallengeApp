package com.tishukoff.feature.rag.impl.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RagChunkEntity::class],
    version = 1,
    exportSchema = false,
)
internal abstract class RagDatabase : RoomDatabase() {
    abstract fun ragChunkDao(): RagChunkDao
}
