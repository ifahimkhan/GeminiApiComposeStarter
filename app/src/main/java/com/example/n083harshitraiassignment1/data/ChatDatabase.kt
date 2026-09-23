package com.example.n083harshitraiassignment1.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.n083harshitraiassignment1.model.ChatMessage

@Database(
    entities = [ChatMessage::class],
    version = 1,
    exportSchema = false
)
abstract class ChatDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao

    companion object {

        @Volatile
        private var INSTANCE: ChatDatabase? = null

        fun getDatabase(
            context: Context
        ): ChatDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "gemini_chat_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance

                instance
            }
        }
    }
}