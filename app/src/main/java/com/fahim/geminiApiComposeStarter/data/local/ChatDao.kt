package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query(
        """
        SELECT *
        FROM chat_sessions
        ORDER BY updatedAt DESC
        """
    )
    fun observeSessions(): Flow<List<ChatSession>>

    @Query(
        """
        SELECT *
        FROM chat_messages
        WHERE sessionId = :sessionId
        ORDER BY timestamp ASC, id ASC
        """
    )
    fun observeMessages(
        sessionId: Long
    ): Flow<List<ChatMessage>>

    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    fun insertSession(
        session: ChatSession
    ): Long

    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    fun insertMessage(
        message: ChatMessage
    ): Long

    @Query(
        """
        UPDATE chat_sessions
        SET updatedAt = :updatedAt
        WHERE id = :sessionId
        """
    )
    fun touchSession(
        sessionId: Long,
        updatedAt: Long
    ): Int

    @Query(
        """
        DELETE FROM chat_sessions
        WHERE id = :sessionId
        """
    )
    fun deleteSession(
        sessionId: Long
    ): Int

    @Query(
        """
        DELETE FROM chat_sessions
        """
    )
    fun deleteAllSessions(): Int
}