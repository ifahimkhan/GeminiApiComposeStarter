package com.fahim.geminiApiComposeStarter.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    /** Insert a single message; returns the new row ID. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    /** Observe all messages ordered chronologically. */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    /** Delete the full conversation history. */
    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()
}
