package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Room database operations on chat messages.
 */
@Dao
interface ChatDao {

    /**
     * Observes all chat messages ordered chronologically.
     */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC, id ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    /**
     * Inserts a new chat message and returns its generated id.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    /**
     * Clears all messages from the conversation history.
     */
    @Query("DELETE FROM chat_messages")
    suspend fun clearMessages(): Int

    /**
     * Deletes a single message by ID.
     */
    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessage(id: Long): Int
}
