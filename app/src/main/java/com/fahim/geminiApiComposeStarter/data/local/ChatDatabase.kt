package com.fahim.geminiApiComposeStarter.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val text: String,
    val timestamp: Long,
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM messages ORDER BY id ASC")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    @Insert
    suspend fun insert(message: ChatMessageEntity): Long

    @Query("DELETE FROM messages")
    suspend fun clear()
}

@Database(entities = [ChatMessageEntity::class], version = 1, exportSchema = false)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        fun create(context: Context): ChatDatabase =
            Room.databaseBuilder(context.applicationContext, ChatDatabase::class.java, "chat.db")
                .build()
    }
}
