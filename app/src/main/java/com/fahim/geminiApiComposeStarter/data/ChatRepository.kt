package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.MessageRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository coordinating persistent chat history in Room and AI text generation via Gemini.
 */
class ChatRepository(
    private val geminiRepository: GeminiRepository,
    private val chatMessageDao: ChatMessageDao,
) {

    val messages: Flow<List<ChatMessage>> = chatMessageDao.getAllMessages().map { entities ->
        entities.map { entity ->
            ChatMessage(
                id = entity.id,
                role = if (entity.role == "USER") MessageRole.USER else MessageRole.GEMINI,
                text = entity.text,
                timestamp = entity.timestamp,
            )
        }
    }

    suspend fun sendUserPrompt(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        // Save user query to Room database
        chatMessageDao.insertMessage(
            ChatMessageEntity(
                role = "USER",
                text = prompt,
                timestamp = System.currentTimeMillis(),
            ),
        )

        // Query Gemini API
        val result = geminiRepository.generateText(prompt)

        result.onSuccess { responseText ->
            chatMessageDao.insertMessage(
                ChatMessageEntity(
                    role = "GEMINI",
                    text = responseText,
                    timestamp = System.currentTimeMillis(),
                ),
            )
        }

        result
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        chatMessageDao.clearMessages()
    }
}
