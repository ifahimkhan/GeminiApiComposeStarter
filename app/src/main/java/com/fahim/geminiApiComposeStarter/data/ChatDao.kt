package com.fahim.geminiApiComposeStarter.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Insert
    suspend fun insertMessage(message: ChatEntity)

    @Insert
    suspend fun insertMessages(messages: List<ChatEntity>)

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun observeMessages(): Flow<List<ChatEntity>>

    // Required for exporting chat history
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    suspend fun getAllMessages(): List<ChatEntity>

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()
}