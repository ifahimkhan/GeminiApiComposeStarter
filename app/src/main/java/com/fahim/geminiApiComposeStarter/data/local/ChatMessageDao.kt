package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_messages ORDER BY id ASC")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    @Insert
    fun insert(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    fun clear(): Int
}