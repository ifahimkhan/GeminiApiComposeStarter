package com.example.c001manavassignment1.data

import com.example.c001manavassignment1.data.local.ChatMessageDao
import com.example.c001manavassignment1.data.local.ChatMessageEntity
import com.example.c001manavassignment1.data.local.ConversationDao
import com.example.c001manavassignment1.data.local.ConversationEntity
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface IGeminiRepository {
    val allConversations: Flow<List<ConversationEntity>>
    fun getMessagesForConversation(conversationId: Long): Flow<List<GeminiRepository.ChatMessage>>
    suspend fun sendMessage(prompt: String, conversationId: Long): String
    suspend fun createConversation(title: String): Long
    suspend fun deleteConversation(conversation: ConversationEntity)
}

class GeminiRepository(
    private val generativeModel: GenerativeModel,
    private val chatMessageDao: ChatMessageDao,
    private val conversationDao: ConversationDao
) : IGeminiRepository {

    override val allConversations: Flow<List<ConversationEntity>> = conversationDao.getAllConversations()

    override fun getMessagesForConversation(conversationId: Long): Flow<List<ChatMessage>> =
        chatMessageDao.getMessagesForConversation(conversationId).map { entities ->
            entities.map { ChatMessage(it.id, it.text, it.isUser) }
        }

    override suspend fun createConversation(title: String): Long {
        return conversationDao.insertConversation(ConversationEntity(title = title))
    }

    override suspend fun deleteConversation(conversation: ConversationEntity) {
        conversationDao.deleteMessagesForConversation(conversation.id)
        conversationDao.deleteConversation(conversation)
    }

    override suspend fun sendMessage(prompt: String, conversationId: Long): String {
        chatMessageDao.insertMessage(ChatMessageEntity(conversationId = conversationId, text = prompt, isUser = true))

        return try {
            val response = generativeModel.generateContent(prompt)
            val responseText = response.text ?: "No response from Gemini"
            chatMessageDao.insertMessage(ChatMessageEntity(conversationId = conversationId, text = responseText, isUser = false))
            
            // Update conversation timestamp
            conversationDao.updateConversation(conversationId, prompt.take(30), System.currentTimeMillis())
            
            responseText
        } catch (e: Exception) {
            throw e
        }
    }

    data class ChatMessage(val id: Long, val text: String, val isUser: Boolean)
}
