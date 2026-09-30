package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.RecentConversation
import com.fahim.geminiApiComposeStarter.data.local.toChatMessage
import com.fahim.geminiApiComposeStarter.data.local.toEntity
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ChatRepositoryImpl(
    private val geminiRepository: GeminiRepository,
    private val chatMessageDao: ChatMessageDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ChatRepository {

    override suspend fun generateText(prompt: String): Result<String> {
        return geminiRepository.generateText(prompt)
    }

    override suspend fun generateText(prompt: String, history: List<ChatMessage>): Result<String> {
        return geminiRepository.generateText(prompt, history)
    }

    override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> {
        return chatMessageDao.observeMessages(conversationId).map { entities ->
            entities.map { it.toChatMessage() }
        }
    }

    override fun observeRecentConversations(): Flow<List<RecentConversation>> {
        return chatMessageDao.observeRecentConversations()
    }

    override suspend fun getMostRecentConversationId(): String? = withContext(ioDispatcher) {
        chatMessageDao.getMostRecentConversationId()
    }

    override suspend fun saveMessage(message: ChatMessage, conversationId: String): Unit = withContext(ioDispatcher) {
        chatMessageDao.insertMessage(message.toEntity(conversationId = conversationId))
    }

    override suspend fun deleteConversation(conversationId: String): Unit = withContext(ioDispatcher) {
        chatMessageDao.deleteConversation(conversationId)
    }

    override suspend fun clearAllMessages(): Unit = withContext(ioDispatcher) {
        chatMessageDao.deleteAllMessages()
    }
}
