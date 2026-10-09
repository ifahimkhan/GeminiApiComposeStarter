package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.BuildConfig
import com.fahim.geminiApiComposeStarter.security.SecureKeyStore
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Abstraction over the Gemini SDK so the ViewModel can be tested with a fake. */
interface GeminiRepository {
    /** @param history earlier messages (oldest first), not including [prompt]. */
    suspend fun generateReply(history: List<ChatMessage>, prompt: String): Result<String>
}

class MissingApiKeyException :
    IllegalStateException("No Gemini API key found. Put GEMINI_API_KEY in local.properties and rebuild.")

class GeminiRepositoryImpl(
    private val secureKeyStore: SecureKeyStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val modelName: String = BuildConfig.GEMINI_MODEL,
) : GeminiRepository {

    private val lock = Mutex()

    @Volatile
    private var cachedModel: GenerativeModel? = null

    /** The key is decrypted only here, in memory, when the model is first built. */
    private suspend fun model(): GenerativeModel =
        cachedModel ?: lock.withLock {
            cachedModel ?: GenerativeModel(
                modelName = modelName,
                apiKey = secureKeyStore.decryptApiKey() ?: throw MissingApiKeyException(),
            ).also { cachedModel = it }
        }

    override suspend fun generateReply(history: List<ChatMessage>, prompt: String): Result<String> =
        withContext(ioDispatcher) {
            try {
                val chat = model().startChat(history = history.asContents())
                val reply = withBackoff { chat.sendMessage(prompt) }.text?.trim()
                if (reply.isNullOrEmpty()) {
                    Result.failure(IllegalStateException("Gemini sent back an empty answer."))
                } else {
                    Result.success(reply)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** Retries busy / rate-limited responses (503, 429) after 1s, 2s and 4s. */
    private suspend fun <T> withBackoff(call: suspend () -> T): T {
        var wait = 1_000L
        repeat(3) {
            try {
                return call()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val text = e.message.orEmpty()
                val transient = listOf("503", "UNAVAILABLE", "429", "RESOURCE_EXHAUSTED").any { it in text }
                if (!transient) throw e
                delay(wait)
                wait *= 2
            }
        }
        return call()
    }

    private fun List<ChatMessage>.asContents(): List<Content> =
        takeLast(HISTORY_LIMIT)
            .dropWhile { !it.isFromUser }
            .map { m -> content(role = if (m.isFromUser) "user" else "model") { text(m.text) } }

    private companion object {
        const val HISTORY_LIMIT = 20
    }
}
