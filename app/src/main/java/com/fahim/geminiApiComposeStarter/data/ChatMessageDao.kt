package com.fahim.geminiApiComposeStarter.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Insert
    fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    fun clearHistory()
}
