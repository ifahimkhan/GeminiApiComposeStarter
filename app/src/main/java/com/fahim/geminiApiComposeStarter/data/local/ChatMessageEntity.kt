package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fahim.geminiApiComposeStarter.data.Author
import com.fahim.geminiApiComposeStarter.data.ChatMessage

@Entity(tableName = "messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val author: String,
    val text: String,
    val createdAt: Long,
    val imagePath: String? = null,
) {
    fun toModel() = ChatMessage(id = id, author = Author.valueOf(author), text = text, imagePath = imagePath)
}
