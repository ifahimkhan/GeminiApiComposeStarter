package com.fahim.geminiApiComposeStarter.data 
import androidx.room.Query
import androidx.room.Dao
import androidx.room.Insert
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Insert
    suspend fun insertMessage(
        message: ChatMessageEntity
    )

    @Query(
        """
        SELECT * FROM chat_messages
        ORDER BY timestamp ASC, id ASC
        """
    )
    fun observeMessages():
            Flow<List<ChatMessageEntity>>

    @Query(
        "DELETE FROM chat_messages"
    )
    suspend fun clearMessages()
}