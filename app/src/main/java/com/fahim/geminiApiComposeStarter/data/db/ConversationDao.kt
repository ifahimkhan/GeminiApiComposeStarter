package com.fahim.geminiApiComposeStarter.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversation_messages ORDER BY created_at ASC")
    fun getConversationStream(): Flow<List<ConversationMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMessage(item: ConversationMessage): Long

    @Query("DELETE FROM conversation_messages")
    suspend fun clearConversation()
}
