package com.fahim.geminiApiComposeStarter.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM messages ORDER BY rowid ASC")
    fun observeAll(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessage)

    @Query("DELETE FROM messages")
    suspend fun clear()
}

@Database(entities = [ChatMessage::class], version = 1, exportSchema = false)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun messages(): ChatMessageDao
}

interface ChatHistory {
    fun observeAll(): Flow<List<ChatMessage>>
    suspend fun save(message: ChatMessage)
    suspend fun clear()
}

class RoomChatHistory(private val dao: ChatMessageDao) : ChatHistory {
    override fun observeAll(): Flow<List<ChatMessage>> = dao.observeAll()
    override suspend fun save(message: ChatMessage) = dao.insert(message)
    override suspend fun clear() = dao.clear()
}
