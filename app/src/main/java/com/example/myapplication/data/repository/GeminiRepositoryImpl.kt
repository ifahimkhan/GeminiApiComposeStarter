package com.example.myapplication.data.repository

import com.example.myapplication.data.local.ChatMessageDao
import com.example.myapplication.data.local.ChatMessageEntity
import com.example.myapplication.data.model.ChatMessage
import com.example.myapplication.data.model.ChatRole
import com.example.myapplication.security.SecureKeyStorage
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class GeminiRepositoryImpl(
    private val chatMessageDao: ChatMessageDao,
    private val secureKeyStorage: SecureKeyStorage,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : GeminiRepository {

    override fun getChatHistory(): Flow<List<ChatMessage>> {
        return chatMessageDao.getAllMessages().map { entities ->
            entities.map { entity ->
                ChatMessage(
                    id = entity.id,
                    role = if (entity.role == "USER") ChatRole.USER else ChatRole.MODEL,
                    text = entity.text,
                    timestamp = entity.timestamp
                )
            }
        }
    }

    override suspend fun sendMessage(prompt: String): Result<String> = withContext(ioDispatcher) {
        val userEntity = ChatMessageEntity(
            role = "USER",
            text = prompt
        )
        chatMessageDao.insertMessage(userEntity)

        val decryptedKey = secureKeyStorage.getDecryptedApiKey()
        if (decryptedKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please check local.properties.")
            )
        }

        try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = decryptedKey
            )
            val response = generativeModel.generateContent(prompt)
            val responseText = response.text ?: "No response generated."

            val modelEntity = ChatMessageEntity(
                role = "MODEL",
                text = responseText
            )
            chatMessageDao.insertMessage(modelEntity)

            Result.success(responseText)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearHistory() = withContext(ioDispatcher) {
        chatMessageDao.clearAllMessages()
    }
}
