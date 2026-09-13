package com.fahim.geminiApiComposeStarter.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatEntity>>

    @Insert
    suspend fun insertMessage(message: ChatEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChat()
}