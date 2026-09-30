package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.security.ApiKeyManager
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Abstraction so ChatViewModel can be unit-tested with a fake. */
interface GeminiRepository {
    /** @param history previous messages (oldest first), NOT including [prompt]. */
    suspend fun generateReply(history: List<ChatMessage>, prompt: String): Result<String>
}

class MissingApiKeyException :
    IllegalStateException("No Gemini API key configured. Add GEMINI_API_KEY to local.properties and rebuild.")

class GeminiRepositoryImpl(
    private val apiKeyManager: ApiKeyManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    private val mutex = Mutex()

    @Volatile
    private var model: GenerativeModel? = null

    /**
     * The API key is decrypted here and only here, in memory, at the moment the
     * GenerativeModel is built. The plaintext is not stored in a field, logged or shown.
     */
    private suspend fun getModel(): GenerativeModel =
        model ?: mutex.withLock {
            model ?: GenerativeModel(
                modelName = modelName,
                apiKey = apiKeyManager.decryptApiKey() ?: throw MissingApiKeyException(),
            ).also { model = it }
        }

    override suspend fun generateReply(history: List<ChatMessage>, prompt: String): Result<String> =
        withContext(ioDispatcher) { // network always off the main thread
            try {
                val chat = getModel().startChat(history = history.toContents())
                val text = chat.sendMessage(prompt).text?.trim()
                if (text.isNullOrEmpty()) {
                    Result.failure(IllegalStateException("Gemini returned an empty response."))
                } else {
                    Result.success(text)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun List<ChatMessage>.toContents(): List<Content> =
        takeLast(MAX_HISTORY)
            .dropWhile { !it.isFromUser } // history must start with a user turn
            .map { msg -> content(role = if (msg.isFromUser) "user" else "model") { text(msg.text) } }

    companion object {
        const val DEFAULT_MODEL = "gemini-3.8-flash"
        private const val MAX_HISTORY = 20
    }
}
