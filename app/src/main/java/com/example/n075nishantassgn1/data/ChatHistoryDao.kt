package com.example.n075nishantassgn1.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatHistoryDao {

    @Insert
    suspend fun insert(message: ChatHistoryEntity)

    @Query("SELECT * FROM chat_history ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatHistoryEntity>>

    @Query("DELETE FROM chat_history")
    suspend fun clearHistory()
}