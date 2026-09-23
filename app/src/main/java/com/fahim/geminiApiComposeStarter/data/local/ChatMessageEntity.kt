package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.Participant

/**
 * Room database entity representing a stored chat message.
 *
 * @property id Unique message identifier (Primary Key).
 * @property text Content of the message.
 * @property participant Participant role ("USER" or "MODEL").
 * @property timestamp Milliseconds since epoch.
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val text: String,
    val participant: String,
    val timestamp: Long,
)

/**
 * Maps a Room database entity to the UI/domain [ChatMessage] model.
 */
fun ChatMessageEntity.toDomain(): ChatMessage = ChatMessage(
    id = id,
    text = text,
    participant = if (participant == Participant.MODEL.name) Participant.MODEL else Participant.USER,
    timestamp = timestamp,
)

/**
 * Maps a UI/domain [ChatMessage] model to a Room database [ChatMessageEntity].
 */
fun ChatMessage.toEntity(): ChatMessageEntity = ChatMessageEntity(
    id = id,
    text = text,
    participant = participant.name,
    timestamp = timestamp,
)
