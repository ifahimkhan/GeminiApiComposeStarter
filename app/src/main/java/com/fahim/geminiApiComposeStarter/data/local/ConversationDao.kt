package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("""
        SELECT * FROM conversations
        ORDER BY updatedAt DESC
    """)
    fun observeConversations(): Flow<List<ConversationEntity>>

    @Insert
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Query("""
        UPDATE conversations
        SET title = :title,
            updatedAt = :updatedAt
        WHERE id = :conversationId
    """)
    suspend fun updateConversation(
        conversationId: Long,
        title: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Delete
    suspend fun deleteConversation(conversation: ConversationEntity)

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()
}