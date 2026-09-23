package com.fahim.geminiApiComposeStarter.data 

import androidx.room.RoomDatabase
import androidx.room.Database
import android.content.Context
import androidx.room.Room

@Database(
    entities = [ChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ChatDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao

    companion object {

        @Volatile
        private var INSTANCE: ChatDatabase? = null

        fun getInstance(
            context: Context
        ): ChatDatabase {

            return INSTANCE
                ?: synchronized(this) {

                    INSTANCE
                        ?: Room.databaseBuilder(
                            context.applicationContext,
                            ChatDatabase::class.java,
                            "gemini_chat_database"
                        )
                            .build()
                            .also {
                                INSTANCE = it
                            }
                }
        }
    }
}