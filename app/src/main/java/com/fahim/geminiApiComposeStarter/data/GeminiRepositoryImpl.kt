package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.data.local.dao.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.entity.ChatMessageEntity
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

private const val PITWALL_SYSTEM_INSTRUCTION =
    "You are PITWALL, an F1 race-engineer style assistant. You are an expert on Formula 1: history, drivers, teams, regulations, strategy, technology, circuits and statistics. Speak concise and confident, with occasional light radio-style phrasing only when it fits. You can still answer any general question helpfully. If unsure about recent results, say so instead of guessing."

class GeminiRepositoryImpl(
    apiKey: String,
    private val chatDao: ChatDao? = null,
    modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    private val model: GenerativeModel by lazy {
        GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            systemInstruction = content { text(PITWALL_SYSTEM_INSTRUCTION) }
        )
    }

    override suspend fun generateText(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = model.generateContent(prompt)
            val text = response.text?.takeIf { it.isNotBlank() }
            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("RADIO FAILURE · Empty response from PITWALL"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "generateContent failed", e)
            Result.failure(e)
        }
    }

    override fun getMessages(): Flow<List<ChatMessageEntity>> {
        return chatDao?.getAllMessages() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    override suspend fun saveMessage(message: ChatMessageEntity): Long = withContext(Dispatchers.IO) {
        chatDao?.insertMessage(message) ?: -1L
    }

    override suspend fun deleteMessage(id: Long) {
        withContext(Dispatchers.IO) {
            chatDao?.deleteMessageById(id)
        }
    }

    override suspend fun clearMessages() {
        withContext(Dispatchers.IO) {
            chatDao?.clearAllMessages()
        }
    }
}
