package com.tishukoff.core.database.impl

import androidx.room.Room
import com.tishukoff.core.database.api.ChatHistoryStorage
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(get(), ChatDatabase::class.java, "chat_database")
            .build()
    }
    single { get<ChatDatabase>().chatMessageDao() }
    single<ChatHistoryStorage> { RoomChatHistoryStorage(get()) }
}
