package com.fahim.geminiApiComposeStarter.data.repository

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.toDomain
import com.fahim.geminiApiComposeStarter.data.local.toEntity
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.Participant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ChatRepository {
    val messagesStream: Flow<List<ChatMessage>>
    suspend fun sendMessage(prompt: String): Result<ChatMessage>
    suspend fun clearHistory()
}

class ChatRepositoryImpl(
    private val chatDao: ChatDao,
    private val geminiRepository: GeminiRepository
) : ChatRepository {

    override val messagesStream: Flow<List<ChatMessage>> =
        chatDao.getAllMessages().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun sendMessage(prompt: String): Result<ChatMessage> {
        val userMessage = ChatMessage(
            text = prompt,
            participant = Participant.USER
        )
        chatDao.insertMessage(userMessage.toEntity())

        val result = geminiRepository.generateText(prompt)
        return result.fold(
            onSuccess = { responseText ->
                val geminiMessage = ChatMessage(
                    text = responseText,
                    participant = Participant.GEMINI
                )
                chatDao.insertMessage(geminiMessage.toEntity())
                Result.success(geminiMessage)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    override suspend fun clearHistory() {
        chatDao.clearAllMessages()
    }
}
