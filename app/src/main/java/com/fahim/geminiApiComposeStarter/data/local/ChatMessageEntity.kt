package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.Participant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(
    tableName = "chat_messages",
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["timestamp"]),
    ],
)
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val text: String,
    val sender: String,
    val timestamp: Long,
)

fun ChatMessageEntity.toChatMessage(): ChatMessage {
    val participant = try {
        Participant.valueOf(sender)
    } catch (_: Exception) {
        if (sender.equals("USER", ignoreCase = true)) Participant.USER else Participant.GEMINI
    }
    return ChatMessage(
        id = id,
        conversationId = conversationId,
        text = text,
        participant = participant,
        timestamp = formatTimestamp(timestamp),
    )
}

fun ChatMessage.toEntity(conversationId: String, timestampMs: Long = System.currentTimeMillis()): ChatMessageEntity {
    return ChatMessageEntity(
        id = id,
        conversationId = conversationId,
        text = text,
        sender = participant.name,
        timestamp = timestampMs,
    )
}

private fun formatTimestamp(timestampMs: Long): String {
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestampMs))
}
