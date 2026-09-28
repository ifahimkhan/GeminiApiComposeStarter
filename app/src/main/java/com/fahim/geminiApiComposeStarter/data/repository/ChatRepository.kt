package com.fahim.geminiApiComposeStarter.data.repository

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.room.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.room.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.room.MessageDeliveryStatus
import com.fahim.geminiApiComposeStarter.data.local.room.SenderRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository abstraction coordinating persistent conversation history (Room)
 * and AI generation requests (Gemini).
 */
interface ChatRepository {
    fun getMessages(): Flow<List<ChatMessageEntity>>
    suspend fun sendUserMessage(prompt: String): Result<String>
    suspend fun clearChat()
    suspend fun deleteMessage(id: Long)
}

/**
 * Default production implementation of [ChatRepository].
 */
class ChatRepositoryImpl(
    private val chatDao: ChatDao,
    private val geminiRepository: GeminiRepository,
) : ChatRepository {

    override fun getMessages(): Flow<List<ChatMessageEntity>> {
        return chatDao.getAllMessages()
    }

    override suspend fun sendUserMessage(prompt: String): Result<String> {
        val trimmedPrompt = prompt.trim()
        if (trimmedPrompt.isBlank()) {
            return Result.failure(IllegalArgumentException("Prompt cannot be empty"))
        }

        // 1. Immediately persist user's message
        val userEntity = ChatMessageEntity(
            senderRole = SenderRole.USER,
            content = trimmedPrompt,
            timestamp = System.currentTimeMillis(),
            status = MessageDeliveryStatus.SENT
        )
        chatDao.insertMessage(userEntity)

        // 2. Request generation from Gemini
        val result = geminiRepository.generateText(trimmedPrompt)

        // 3. Persist AI response or error message
        result.fold(
            onSuccess = { responseText ->
                val assistantEntity = ChatMessageEntity(
                    senderRole = SenderRole.ASSISTANT,
                    content = responseText,
                    timestamp = System.currentTimeMillis(),
                    status = MessageDeliveryStatus.DELIVERED
                )
                chatDao.insertMessage(assistantEntity)
            },
            onFailure = { error ->
                val errorEntity = ChatMessageEntity(
                    senderRole = SenderRole.ASSISTANT,
                    content = "Error: ${error.message ?: "Failed to receive response"}",
                    timestamp = System.currentTimeMillis(),
                    status = MessageDeliveryStatus.ERROR
                )
                chatDao.insertMessage(errorEntity)
            }
        )

        return result
    }

    override suspend fun clearChat() {
        chatDao.clearAllMessages()
    }

    override suspend fun deleteMessage(id: Long) {
        chatDao.deleteMessageById(id)
    }
}
