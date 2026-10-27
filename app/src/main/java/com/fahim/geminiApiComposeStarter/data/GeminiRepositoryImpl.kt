package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.data.local.MessageDao
import com.fahim.geminiApiComposeStarter.data.local.MessageEntity
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    private val keyProvider: () -> String,
    private val messageDao: MessageDao? = null,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    constructor(
        apiKey: String,
        messageDao: MessageDao? = null,
        modelName: String = DEFAULT_MODEL,
    ) : this(keyProvider = { apiKey }, messageDao = messageDao, modelName = modelName)

    private val model: GenerativeModel by lazy {
        GenerativeModel(modelName = modelName, apiKey = keyProvider())
    }

    override fun observeConversation(): Flow<List<MessageEntity>> {
        return messageDao?.observeMessages() ?: flowOf(emptyList())
    }

    override suspend fun sendPrompt(prompt: String): Result<String> {
        messageDao?.insert(MessageEntity(content = prompt, fromUser = true))

        val responseResult = generateText(prompt)
        responseResult.onSuccess { reply ->
            messageDao?.insert(MessageEntity(content = reply, fromUser = false))
        }
        return responseResult
    }

    override suspend fun generateText(prompt: String): Result<String> = try {
        val response = model.generateContent(prompt)
        Result.success(response.text.orEmpty())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "generateContent failed", e)
        Result.failure(e)
    }

    override suspend fun clearConversation() {
        messageDao?.deleteAll()
    }
}
