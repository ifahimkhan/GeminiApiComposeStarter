package com.fahim.geminiApiComposeStarter.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ChatMessageEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ChatDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao

    companion object {
        fun create(context: Context): ChatDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                ChatDatabase::class.java,
                "gemini_chat.db",
            ).build()
    }
}
