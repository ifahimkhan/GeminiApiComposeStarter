package com.fahim.geminiApiComposeStarter.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface ChatDao {

    // ── Reads ─────────────────────────────────────────────────────────────────

    @Transaction
    @Query("SELECT * FROM conversations ORDER BY position ASC")
    suspend fun loadAll(): List<ConversationWithMessages>

    // ── Writes ────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessages(messages: List<MessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttachments(attachments: List<AttachmentEntity>)

    /** Removes messages no longer in the conversation so deletions propagate. */
    @Query("DELETE FROM messages WHERE conversationId = :convId AND id NOT IN (:keepIds)")
    suspend fun deleteStaleMessages(convId: String, keepIds: List<Long>)

    /** Removes attachments no longer associated with a message. */
    @Query("DELETE FROM attachments WHERE messageId = :msgId AND uri NOT IN (:keepUris)")
    suspend fun deleteStaleAttachments(msgId: Long, keepUris: List<String>)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)

    @Query("DELETE FROM conversations WHERE id NOT IN (:keepIds)")
    suspend fun deleteConversationsNotIn(keepIds: List<String>)
}
