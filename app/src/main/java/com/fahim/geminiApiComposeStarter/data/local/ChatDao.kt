package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Room database operations on chat conversation messages.
 * Synchronous CRUD methods are dispatched on Dispatchers.IO from GeminiRepositoryImpl.
 */
@Dao
interface ChatDao {

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    fun clearAllMessages(): Int

    @Query("DELETE FROM chat_messages WHERE id = :id")
    fun deleteMessageById(id: Long): Int
}
