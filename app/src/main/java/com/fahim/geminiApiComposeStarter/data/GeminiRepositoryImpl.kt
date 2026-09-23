package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.MessageRole
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

private const val TAG = "GeminiRepository"

/**
 * Concrete implementation of [GeminiRepository].
 * Decrypts the API key in memory only for the duration of the GenerativeModel call.
 * Saves prompts and responses to the Room database.
 * Preserves multi-turn chat history across sessions.
 */
class GeminiRepositoryImpl(
    private val apiKeyProvider: () -> String?,
    private val chatMessageDao: ChatMessageDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : GeminiRepository {

    override val messages: Flow<List<ChatMessageEntity>> = chatMessageDao.getAllMessages()

    override fun hasApiKey(): Boolean {
        val key = apiKeyProvider()
        return !key.isNullOrBlank()
    }

    override suspend fun sendMessage(prompt: String, modelName: String): Result<String> =
        withContext(ioDispatcher) {
            // 1. Retrieve previous conversation turns from Room before saving current prompt
            val pastEntities = chatMessageDao.getSuccessfulHistory()

            // 2. Persist the user message
            val userEntity = ChatMessageEntity(
                role = MessageRole.USER.name,
                content = prompt,
                timestamp = System.currentTimeMillis(),
            )
            chatMessageDao.insertMessage(userEntity)

            // 3. Resolve decrypted API key strictly in memory
            val apiKey = apiKeyProvider()
            if (apiKey.isNullOrBlank()) {
                val errorMsg = "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."
                chatMessageDao.insertMessage(
                    ChatMessageEntity(
                        role = MessageRole.MODEL.name,
                        content = errorMsg,
                        timestamp = System.currentTimeMillis(),
                        isError = true,
                    )
                )
                return@withContext Result.failure(IllegalStateException(errorMsg))
            }

            // 4. Invoke Gemini Generative AI SDK with chat history & retry on transient 503 high demand
            val history = buildGeminiHistory(pastEntities)
            val maxRetries = 2
            var lastException: Exception? = null

            for (attempt in 0..maxRetries) {
                try {
                    val model = GenerativeModel(modelName = modelName, apiKey = apiKey)
                    val chat = model.startChat(history = history)
                    val response = chat.sendMessage(prompt)
                    val text = response.text?.takeIf { it.isNotBlank() }
                    if (text != null) {
                        chatMessageDao.insertMessage(
                            ChatMessageEntity(
                                role = MessageRole.MODEL.name,
                                content = text,
                                timestamp = System.currentTimeMillis(),
                                isError = false,
                            )
                        )
                        return@withContext Result.success(text)
                    } else {
                        val emptyError = "Empty response received from Gemini."
                        chatMessageDao.insertMessage(
                            ChatMessageEntity(
                                role = MessageRole.MODEL.name,
                                content = emptyError,
                                timestamp = System.currentTimeMillis(),
                                isError = true,
                            )
                        )
                        return@withContext Result.failure(IllegalStateException(emptyError))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastException = e
                    val raw = e.message.orEmpty()
                    val isTransient = raw.contains("503") ||
                            raw.contains("high demand", ignoreCase = true) ||
                            raw.contains("UNAVAILABLE", ignoreCase = true)

                    if (isTransient && attempt < maxRetries) {
                        Log.w(TAG, "Transient 503 error on attempt $attempt, retrying after backoff...")
                        delay((attempt + 1) * 1500L)
                        continue
                    }
                    break
                }
            }

            val finalError = lastException ?: IllegalStateException("Failed to generate content")
            Log.e(TAG, "Gemini API request failed: ${finalError.localizedMessage}")
            val rawMsg = finalError.message.orEmpty()
            val friendlyError = when {
                rawMsg.contains("503") || rawMsg.contains("high demand", ignoreCase = true) || rawMsg.contains("UNAVAILABLE", ignoreCase = true) ->
                    "This model ($modelName) is currently experiencing temporary high demand on Google's servers. Please try again in a moment, or switch to another model (e.g. gemini-1.5-flash) from the top bar."
                rawMsg.contains("429") || rawMsg.contains("quota", ignoreCase = true) || rawMsg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ->
                    "Rate limit reached. Please wait a moment before sending another prompt."
                rawMsg.contains("403") || rawMsg.contains("API_KEY_INVALID", ignoreCase = true) ->
                    "Invalid API key. Please check your GEMINI_API_KEY in local.properties."
                rawMsg.contains("Unable to resolve host") || rawMsg.contains("ConnectException") ->
                    "Network error. Please check your internet connection and try again."
                else -> rawMsg.ifBlank { "Something unexpected happened. Please try again." }
            }

            chatMessageDao.insertMessage(
                ChatMessageEntity(
                    role = MessageRole.MODEL.name,
                    content = friendlyError,
                    timestamp = System.currentTimeMillis(),
                    isError = true,
                )
            )
            Result.failure(Exception(friendlyError, finalError))
        }

    override suspend fun clearHistory() = withContext(ioDispatcher) {
        chatMessageDao.clearAllMessages()
        Unit
    }

    override suspend fun deleteMessage(id: Long) = withContext(ioDispatcher) {
        chatMessageDao.deleteMessageById(id)
        Unit
    }
}

/**
 * Transforms stored database messages into a properly structured, alternating list of [Content]
 * suitable for [GenerativeModel.startChat].
 * Consecutive messages from the same role are combined, and the history starts with a 'user' turn.
 */
internal fun buildGeminiHistory(entities: List<ChatMessageEntity>): List<Content> {
    val nonError = entities.filter { !it.isError && it.content.isNotBlank() }
    val history = mutableListOf<Content>()
    var currentRole: String? = null
    val currentParts = StringBuilder()

    for (entity in nonError) {
        val role = if (entity.role == MessageRole.USER.name) "user" else "model"
        if (role == currentRole) {
            currentParts.append("\n\n").append(entity.content.trim())
        } else {
            if (currentRole != null && currentParts.isNotEmpty()) {
                val capturedRole = currentRole
                val capturedText = currentParts.toString()
                history.add(content(role = capturedRole) { text(capturedText) })
                currentParts.clear()
            }
            currentRole = role
            currentParts.append(entity.content.trim())
        }
    }
    if (currentRole != null && currentParts.isNotEmpty()) {
        val capturedRole = currentRole
        val capturedText = currentParts.toString()
        history.add(content(role = capturedRole) { text(capturedText) })
    }

    // History for startChat must start with 'user' and end with 'model' (since the current turn is 'user')
    val validHistory = history
        .dropWhile { it.role != "user" }
        .let { list ->
            if (list.isNotEmpty() && list.last().role == "user") {
                list.dropLast(1)
            } else {
                list
            }
        }
    return validHistory
}

