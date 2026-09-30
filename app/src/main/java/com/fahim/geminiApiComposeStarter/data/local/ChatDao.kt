package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Room database operations on chat messages.
 * Synchronous queries are executed off the main thread via [ChatHistoryRepositoryImpl].
 */
@Dao
interface ChatDao {

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC, id ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC, id ASC")
    fun getAllMessagesSnapshot(): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessages(messages: List<ChatMessageEntity>)

    @Query("DELETE FROM chat_messages")
    fun clearAllMessages()
}
