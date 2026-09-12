package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class MessageAuthor {
    USER,
    GEMINI,
}

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["sessionId"]),
    ],
)
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val sessionId: Long,
    val author: MessageAuthor,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
)

class ChatTypeConverters {

    @TypeConverter
    fun authorToString(author: MessageAuthor): String {
        return author.name
    }

    @TypeConverter
    fun stringToAuthor(value: String): MessageAuthor {
        return runCatching {
            MessageAuthor.valueOf(value)
        }.getOrDefault(MessageAuthor.USER)
    }
}
