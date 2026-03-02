package com.tishukoff.core.database.impl

import androidx.room.Room
import com.tishukoff.core.database.api.ChatHistoryStorage
import com.tishukoff.core.database.api.ChatStorage
import com.tishukoff.core.database.api.ContextSummaryStorage
import com.tishukoff.core.database.api.LongTermMemoryStorage
import com.tishukoff.core.database.api.WorkingMemoryStorage
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
    single { get<ChatDatabase>().workingMemoryDao() }
    single { get<ChatDatabase>().longTermMemoryDao() }
    single<ChatHistoryStorage> { RoomChatHistoryStorage(get()) }
    single<ChatStorage> { RoomChatStorage(get(), get()) }
    single<ContextSummaryStorage> { RoomContextSummaryStorage(get()) }
    single<WorkingMemoryStorage> { RoomWorkingMemoryStorage(get()) }
    single<LongTermMemoryStorage> { RoomLongTermMemoryStorage(get()) }
}
