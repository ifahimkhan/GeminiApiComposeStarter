package com.fahim.geminiApiComposeStarter.data.local

import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.ui.chat.ChatAuthor
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.Conversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomChatHistoryRepository(
    private val messageDao: ChatMessageDao,
    private val conversationDao: ConversationDao,
) : ChatHistoryRepository {

    override fun observeConversations(): Flow<List<Conversation>> =
        conversationDao.observeAll().map { list -> list.map { it.toUiModel() } }

    override suspend fun createConversation(title: String): Long {
        val now = System.currentTimeMillis()
        return conversationDao.insert(
            ConversationEntity(
                title = title.ifBlank { "New chat" },
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun loadMessages(conversationId: Long): List<ChatMessage> =
        messageDao.getForConversation(conversationId).map { it.toUiModel() }

    override suspend fun save(conversationId: Long, message: ChatMessage) {
        messageDao.insert(message.toEntity(conversationId))
    }

    override suspend fun touchConversation(conversationId: Long) {
        conversationDao.touch(conversationId, System.currentTimeMillis())
    }

    override suspend fun deleteConversation(conversationId: Long) {
        messageDao.clearForConversation(conversationId)
        conversationDao.delete(conversationId)
    }
}

private fun ConversationEntity.toUiModel(): Conversation = Conversation(
    id = id,
    title = title,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun ChatMessageEntity.toUiModel(): ChatMessage = ChatMessage(
    id = id,
    text = text,
    author = runCatching { ChatAuthor.valueOf(author) }.getOrDefault(ChatAuthor.GEMINI),
    timestamp = timestamp,
)

private fun ChatMessage.toEntity(conversationId: Long): ChatMessageEntity = ChatMessageEntity(
    id = id,
    conversationId = conversationId,
    text = text,
    author = author.name,
    timestamp = timestamp,
)