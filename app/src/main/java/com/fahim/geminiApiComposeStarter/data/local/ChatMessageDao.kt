package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChatMessageDao {

    @Query(
        "SELECT * FROM chat_messages " +
                "WHERE conversationId = :conversationId " +
                "ORDER BY timestamp ASC, id ASC",
    )
    suspend fun getForConversation(conversationId: Long): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun clearForConversation(conversationId: Long)
}