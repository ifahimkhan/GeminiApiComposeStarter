package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query(
        """
        SELECT * FROM chat_messages
        ORDER BY timestamp ASC, id ASC
        """
    )
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Insert
    suspend fun insertMessage(
        message: ChatMessageEntity
    ): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearMessages()
}