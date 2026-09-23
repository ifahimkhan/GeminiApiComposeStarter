package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for persisting and observing chat conversations in Room.
 */
@Dao
interface ChatMessageDao {

    /**
     * Observes all messages chronologically ordered by creation timestamp.
     */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    /**
     * Retrieves an immediate one-shot snapshot of all messages.
     */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    suspend fun getAllMessagesSnapshot(): List<ChatMessageEntity>

    /**
     * Inserts or replaces a chat message.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    /**
     * Inserts or replaces multiple chat messages.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    /**
     * Deletes all messages in the conversation history.
     */
    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()
}
