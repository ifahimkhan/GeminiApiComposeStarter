package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.BuildConfig
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.ResponseStyle
import com.fahim.geminiApiComposeStarter.data.model.Role
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"
/** Only the most recent turns are sent, keeping requests small and latency low. */
private const val MAX_HISTORY_MESSAGES = 20

/**
 * @param apiKeyProvider decrypts the key on demand (see SecureApiKeyStore). It is invoked only
 *   when a GenerativeModel has to be created, and the plaintext is never logged.
 */
class GeminiRepositoryImpl(
    private val apiKeyProvider: suspend () -> String?,
    private val modelName: String = DEFAULT_MODEL,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : GeminiRepository {

    private val mutex = Mutex()
    private val models = mutableMapOf<ResponseStyle, GenerativeModel>()

    private suspend fun modelFor(style: ResponseStyle): GenerativeModel = mutex.withLock {
        models[style] ?: run {
            val apiKey = apiKeyProvider()?.takeIf { it.isNotBlank() }
                ?: throw MissingApiKeyException()
            GenerativeModel(
                modelName = modelName,
                apiKey = apiKey,
                systemInstruction = content { text(style.instruction) },
            ).also { models[style] = it }
        }
    }

    override suspend fun generateReply(
        history: List<ChatMessage>,
        prompt: String,
        style: ResponseStyle,
    ): Result<String> = withContext(ioDispatcher) {
        try {
            val model = modelFor(style)
            val contents = history.takeLast(MAX_HISTORY_MESSAGES).map { message ->
                content(role = if (message.role == Role.USER) "user" else "model") {
                    text(message.text)
                }
            } + content(role = "user") { text(prompt) }

            val text = model.generateContent(*contents.toTypedArray()).text?.takeIf { it.isNotBlank() }
            if (text != null) Result.success(text)
            else Result.failure(IllegalStateException("Empty response from Gemini"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "generateContent failed: ${e.javaClass.simpleName}")
            Result.failure(e)
        }
    }
}
