package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>): List<Long>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun observeMessages(conversationId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessages(conversationId: String): List<ChatMessageEntity>

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteConversation(conversationId: String): Int

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages(): Int

    @Query("""
        SELECT 
            m.conversationId AS conversationId,
            MAX(m.timestamp) AS lastTimestamp,
            COALESCE(
                (SELECT text FROM chat_messages WHERE conversationId = m.conversationId AND sender = 'USER' ORDER BY timestamp ASC LIMIT 1),
                (SELECT text FROM chat_messages WHERE conversationId = m.conversationId ORDER BY timestamp ASC LIMIT 1),
                'Chat'
            ) AS title
        FROM chat_messages m
        GROUP BY m.conversationId
        ORDER BY lastTimestamp DESC
    """)
    fun observeRecentConversations(): Flow<List<RecentConversation>>

    @Query("SELECT conversationId FROM chat_messages ORDER BY timestamp DESC LIMIT 1")
    suspend fun getMostRecentConversationId(): String?
}
