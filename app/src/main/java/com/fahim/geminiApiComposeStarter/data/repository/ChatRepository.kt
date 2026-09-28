package com.fahim.geminiApiComposeStarter.data.repository

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.room.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.room.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.room.MessageRole
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    val messagesFlow: Flow<List<ChatMessageEntity>>
    suspend fun sendMessage(prompt: String): Result<String>
    suspend fun clearHistory()
}

class ChatRepositoryImpl(
    private val geminiRepository: GeminiRepository,
    private val chatDao: ChatDao,
) : ChatRepository {

    override val messagesFlow: Flow<List<ChatMessageEntity>> = chatDao.getAllMessages()

    override suspend fun sendMessage(prompt: String): Result<String> {
        val trimmedPrompt = prompt.trim()
        if (trimmedPrompt.isEmpty()) {
            return Result.failure(IllegalArgumentException("Prompt cannot be empty"))
        }

        // 1. Immediately persist the user message in Room database
        chatDao.insertMessage(
            ChatMessageEntity(
                content = trimmedPrompt,
                role = MessageRole.USER,
                timestamp = System.currentTimeMillis(),
            )
        )

        // 2. Request generation from Gemini API
        val result = geminiRepository.generateText(trimmedPrompt)

        // 3. Persist response or failure
        result.fold(
            onSuccess = { responseText ->
                chatDao.insertMessage(
                    ChatMessageEntity(
                        content = responseText,
                        role = MessageRole.GEMINI,
                        timestamp = System.currentTimeMillis(),
                        isError = false,
                    )
                )
            },
            onFailure = { error ->
                chatDao.insertMessage(
                    ChatMessageEntity(
                        content = error.message ?: "Failed to generate response.",
                        role = MessageRole.GEMINI,
                        timestamp = System.currentTimeMillis(),
                        isError = true,
                    )
                )
            }
        )

        return result
    }

    override suspend fun clearHistory() {
        chatDao.clearAll()
    }
}
