package com.tishukoff.core.database.impl

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ChatMessageEntity::class, ChatEntity::class],
    version = 2,
)
internal abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun chatDao(): ChatDao
}

internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS chats (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            "ALTER TABLE chat_messages ADD COLUMN chatId INTEGER NOT NULL DEFAULT 0"
        )

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_chat_messages_chatId ON chat_messages(chatId)"
        )
    }
}
