package com.fahim.geminiApiComposeStarter.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ChatEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}