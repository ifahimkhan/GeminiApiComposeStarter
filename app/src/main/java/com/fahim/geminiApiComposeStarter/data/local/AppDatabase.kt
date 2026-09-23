package com.fahim.geminiApiComposeStarter.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromParticipant(participant: MessageParticipant): String = participant.name

    @TypeConverter
    fun toParticipant(value: String): MessageParticipant =
        try {
            MessageParticipant.valueOf(value)
        } catch (e: Exception) {
            MessageParticipant.USER
        }
}

@Database(entities = [ChatMessageEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gemini_chat_database",
                ).build().also { INSTANCE = it }
            }
    }
}
