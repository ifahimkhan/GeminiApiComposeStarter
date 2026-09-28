package com.fahim.geminiApiComposeStarter.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC, id ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long): Int

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll(): Int

    @Query("SELECT * FROM chat_messages ORDER BY id DESC LIMIT 1")
    suspend fun getLastMessage(): ChatMessageEntity?
}
