package com.silas.geminichat.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.silas.geminichat.model.MessageRole

@Database(entities = [ChatMessageEntity::class], version = 1, exportSchema = false)
@TypeConverters(ChatDatabase.Converters::class)
abstract class ChatDatabase : RoomDatabase() {

    abstract fun chatMessageDao(): ChatMessageDao

    class Converters {
        @TypeConverter
        fun roleToString(role: MessageRole): String = role.name

        @TypeConverter
        fun stringToRole(value: String): MessageRole = MessageRole.valueOf(value)
    }

    companion object {
        @Volatile
        private var instance: ChatDatabase? = null

        fun getInstance(context: Context): ChatDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "silas_gemini_chat.db"
                ).build().also { instance = it }
            }
    }
}
