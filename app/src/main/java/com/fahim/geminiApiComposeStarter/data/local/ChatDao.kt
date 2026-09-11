package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM conversations ORDER BY createdAt DESC")
    fun getAllConversations(): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: Long): Conversation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: Conversation): Long

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: Long)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: Long): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND branchId = :branchId ORDER BY timestamp ASC")
    fun getMessagesForBranch(conversationId: Long, branchId: String): Flow<List<Message>>

    @Query("""
        WITH RECURSIVE path(id, conversationId, role, content, timestamp, parentMessageId, branchId) AS (
            SELECT * FROM messages WHERE id = :leafMessageId
            UNION ALL
            SELECT m.* FROM messages m JOIN path ON m.id = path.parentMessageId
        )
        SELECT * FROM path ORDER BY timestamp ASC
    """)
    fun getMessagesPath(leafMessageId: Long): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("SELECT DISTINCT branchId FROM messages WHERE conversationId = :conversationId")
    fun getBranchesForConversation(conversationId: Long): Flow<List<String>>
}
