package com.example.geminiapicomposestarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.geminiapicomposestarter.data.ChatMessage
import com.example.geminiapicomposestarter.data.Participant

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val participant: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomain(): ChatMessage {
        val p = if (participant == Participant.USER.name) Participant.USER else Participant.MODEL
        return ChatMessage(
            id = id.toString(),
            text = text,
            participant = p,
            timestamp = timestamp
        )
    }

    companion object {
        fun fromDomain(msg: ChatMessage): ChatMessageEntity {
            return ChatMessageEntity(
                text = msg.text,
                participant = msg.participant.name,
                timestamp = msg.timestamp
            )
        }
    }
}
