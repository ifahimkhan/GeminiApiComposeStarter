package com.fahim.geminiApiComposeStarter.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fahim.geminiApiComposeStarter.data.local.dao.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.entity.ChatMessageEntity

@Database(entities = [ChatMessageEntity::class], version = 1, exportSchema = false)
abstract class PitwallDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: PitwallDatabase? = null

        fun getDatabase(context: Context): PitwallDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PitwallDatabase::class.java,
                    "pitwall_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
