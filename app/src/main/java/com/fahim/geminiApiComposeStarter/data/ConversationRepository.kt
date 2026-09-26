package com.fahim.geminiApiComposeStarter.data

import androidx.room.withTransaction
import com.fahim.geminiApiComposeStarter.data.local.*
import kotlinx.coroutines.flow.map

class ConversationRepository(private val database: ChatDatabase) : ChatStore {
    private val dao = database.chats()
    override fun conversations(query: String) = dao.conversations(query).map { list -> list.map {
        Conversation(it.id, it.title, Category.entries.firstOrNull { c -> c.name == it.category } ?: Category.GENERAL, it.preview, it.updatedAt)
    } }
    override fun messages(conversationId: String) = dao.messages(conversationId).map { list -> list.map {
        ChatMessage(it.id, it.conversationId, it.turnId, it.sequence, Role.valueOf(it.role), it.text, Delivery.valueOf(it.delivery), it.feedback)
    } }
    override suspend fun create(conversation: Conversation) { dao.create(ConversationEntity(conversation.id, conversation.title, conversation.category.name, conversation.preview, conversation.updatedAt)) }
    override suspend fun appendUser(message: ChatMessage) { database.withTransaction {
        dao.put(message.entity()); dao.touch(message.conversationId, message.text.take(160), System.currentTimeMillis())
    } }
    override suspend fun setDelivery(messageId: String, delivery: Delivery) = dao.delivery(messageId, delivery.name)
    override suspend fun complete(user: ChatMessage, answer: String) { database.withTransaction {
        dao.delivery(user.id, Delivery.SENT.name)
        dao.put(ChatMessage(user.turnId + "-answer", user.conversationId, user.turnId, user.sequence + 1, Role.ASSISTANT, answer).entity())
        dao.touch(user.conversationId, answer.take(160), System.currentTimeMillis())
    } }
    override suspend fun feedback(messageId: String, value: Int) = dao.feedback(messageId, value)
    override suspend fun recoverInterrupted() = dao.recover()
    private fun ChatMessage.entity() = MessageEntity(id, conversationId, turnId, sequence, role.name, text, delivery.name, feedback)
}
