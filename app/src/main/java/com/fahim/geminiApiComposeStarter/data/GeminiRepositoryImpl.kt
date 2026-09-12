package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.data.local.MessageDao
import com.fahim.geminiApiComposeStarter.data.local.MessageEntity
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    private val messageDao: MessageDao,
    private val apiKeyProvider: () -> String,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    private val model by lazy {
        GenerativeModel(modelName = modelName, apiKey = apiKeyProvider())
    }

    override fun getMessagesFlow(): Flow<List<ChatMessage>> {
        return messageDao.getAllMessagesFlow().map { entities ->
            entities.map { it.toChatMessage() }
        }
    }

    override suspend fun saveMessage(message: ChatMessage) {
        messageDao.insertMessage(MessageEntity.fromChatMessage(message))
    }

    override suspend fun generateText(prompt: String): Result<String> = try {
        val response = model.generateContent(prompt)
        val text = response.text?.takeIf { it.isNotBlank() }
        if (text != null) {
            Result.success(text)
        } else {
            Result.failure(IllegalStateException("Empty response from Gemini"))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "generateContent failed", e)
        Result.failure(e)
    }
}
