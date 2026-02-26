package com.tishukoff.core.database.impl

import androidx.room.Room
import com.tishukoff.core.database.api.ChatHistoryStorage
import com.tishukoff.core.database.api.ChatStorage
import com.tishukoff.core.database.api.ContextSummaryStorage
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(get(), ChatDatabase::class.java, "chat_database")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    single { get<ChatDatabase>().chatMessageDao() }
    single { get<ChatDatabase>().chatDao() }
    single { get<ChatDatabase>().contextSummaryDao() }
    single<ChatHistoryStorage> { RoomChatHistoryStorage(get()) }
    single<ChatStorage> { RoomChatStorage(get(), get()) }
    single<ContextSummaryStorage> { RoomContextSummaryStorage(get()) }
}
