package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.ConversationDao
import com.fahim.geminiApiComposeStarter.data.local.ConversationEntity
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatHistoryRepository(
    private val messageDao: ChatMessageDao,
    private val conversationDao: ConversationDao
) {

    fun observeConversations(): Flow<List<ConversationEntity>> {
        return conversationDao.observeConversations()
    }

    fun observeMessages(conversationId: Long): Flow<List<ChatMessage>> {
        return messageDao.observeMessages(conversationId).map { entities ->
            entities.map { entity ->
                ChatMessage(
                    id = entity.id,
                    text = entity.text,
                    isUser = entity.isUser
                )
            }
        }
    }

    suspend fun createConversation(title: String = "New Chat"): Long {
        return conversationDao.insertConversation(
            ConversationEntity(
                title = title
            )
        )
    }

    suspend fun saveMessage(
        conversationId: Long,
        message: ChatMessage
    ) {
        messageDao.insertMessage(
            ChatMessageEntity(
                conversationId = conversationId,
                text = message.text,
                isUser = message.isUser
            )
        )

        updateConversation(
            conversationId = conversationId,
            title = if (message.isUser) {
                message.text.take(40)
            } else {
                null
            }
        )
    }

    private suspend fun updateConversation(
        conversationId: Long,
        title: String?
    ) {
        if (title != null) {
            conversationDao.updateConversation(
                conversationId = conversationId,
                title = title
            )
        }
    }

    suspend fun deleteConversation(conversation: ConversationEntity) {
        messageDao.deleteMessagesForConversation(conversation.id)
        conversationDao.deleteConversation(conversation)
    }

    suspend fun clearAll() {
        conversationDao.deleteAllConversations()
    }
}