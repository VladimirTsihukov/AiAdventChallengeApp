package com.tishukoff.core.database.impl

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ChatMessageEntity::class, ChatEntity::class, ContextSummaryEntity::class],
    version = 3,
)
internal abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun chatDao(): ChatDao
    abstract fun contextSummaryDao(): ContextSummaryDao
}
