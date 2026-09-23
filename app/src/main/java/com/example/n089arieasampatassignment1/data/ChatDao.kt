package com.example.n089arieasampatassignment1.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.n089arieasampatassignment1.model.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query(
        "SELECT * FROM chat_messages ORDER BY timestamp ASC"
    )
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()
}
