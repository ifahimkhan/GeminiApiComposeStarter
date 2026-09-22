package com.silas.geminichat.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.silas.geminichat.model.ChatMessage
import com.silas.geminichat.model.MessageRole

/** Room entity backing the persisted chat history. */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val role: MessageRole,
    val content: String,
    val timestamp: Long,
) {
    fun toModel(): ChatMessage = ChatMessage(id = id, role = role, content = content, timestamp = timestamp)

    companion object {
        fun fromModel(message: ChatMessage): ChatMessageEntity = ChatMessageEntity(
            id = message.id,
            role = message.role,
            content = message.content,
            timestamp = message.timestamp,
        )
    }
}
