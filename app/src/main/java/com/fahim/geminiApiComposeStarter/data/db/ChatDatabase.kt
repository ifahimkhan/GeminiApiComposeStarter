package com.fahim.geminiApiComposeStarter.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

/** Converter so Room can store the [MessageRole] enum as a String. */
class Converters {
    @TypeConverter fun fromRole(role: MessageRole): String = role.name
    @TypeConverter fun toRole(name: String): MessageRole = MessageRole.valueOf(name)
}

@Database(entities = [ChatMessage::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ChatDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile private var INSTANCE: ChatDatabase? = null

        fun getInstance(context: Context): ChatDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "chat_history.db",
                ).build().also { INSTANCE = it }
            }
    }
}
