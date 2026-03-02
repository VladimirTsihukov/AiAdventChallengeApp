package com.tishukoff.core.database.impl

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChatMessageEntity::class,
        ChatEntity::class,
        ContextSummaryEntity::class,
        WorkingMemoryEntity::class,
        LongTermMemoryEntity::class,
    ],
    version = 4,
)
internal abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun chatDao(): ChatDao
    abstract fun contextSummaryDao(): ContextSummaryDao
    abstract fun workingMemoryDao(): WorkingMemoryDao
    abstract fun longTermMemoryDao(): LongTermMemoryDao
}
