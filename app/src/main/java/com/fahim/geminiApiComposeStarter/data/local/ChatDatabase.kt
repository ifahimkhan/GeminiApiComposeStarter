package com.fahim.geminiApiComposeStarter.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ChatSession::class,
        ChatMessage::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(ChatTypeConverters::class)
abstract class ChatDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao

    companion object {

        @Volatile
        private var instance: ChatDatabase? = null

        fun getInstance(context: Context): ChatDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room
                    .databaseBuilder(
                        context.applicationContext,
                        ChatDatabase::class.java,
                        DATABASE_NAME,
                    )
                    /*
                     * The schema changes from the earlier one-conversation
                     * database to sessions + messages.
                     *
                     * For this student assignment build, a one-time destructive
                     * migration is used because the exact earlier local schema
                     * may differ between your intermediate versions.
                     */
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { database ->
                        instance = database
                    }
            }
        }

        private const val DATABASE_NAME =
            "gemini_chat.db"
    }
}
