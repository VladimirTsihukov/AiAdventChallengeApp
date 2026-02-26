package com.tishukoff.core.database.impl

import androidx.room.Room
import com.tishukoff.core.database.api.ChatHistoryStorage
import com.tishukoff.core.database.api.ChatStorage
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(get(), ChatDatabase::class.java, "chat_database")
            .addMigrations(MIGRATION_1_2)
            .build()
    }
    single { get<ChatDatabase>().chatMessageDao() }
    single { get<ChatDatabase>().chatDao() }
    single<ChatHistoryStorage> { RoomChatHistoryStorage(get()) }
    single<ChatStorage> { RoomChatStorage(get(), get()) }
}
