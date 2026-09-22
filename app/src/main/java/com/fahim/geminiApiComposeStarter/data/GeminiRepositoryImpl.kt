package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "GeminiRepository"
// The starter shipped "gemini-3.6-flash", which ListModels does not return -- every call failed
// with a 404. This is the newest flash model the API actually serves.
private const val DEFAULT_MODEL = "gemini-3.5-flash"

/**
 * [apiKeyProvider] is a suspending lambda rather than a plain string so the key is only decrypted
 * out of the Android Keystore at the moment the [GenerativeModel] is constructed. The plaintext
 * key is never held in a field, never logged and never surfaced to the UI.
 */
class GeminiRepositoryImpl(
    private val apiKeyProvider: suspend () -> String,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    @Volatile
    private var model: GenerativeModel? = null
    private val modelLock = Mutex()

    private suspend fun model(): GenerativeModel =
        model ?: modelLock.withLock {
            model ?: GenerativeModel(modelName = modelName, apiKey = apiKeyProvider())
                .also { model = it }
        }

    override suspend fun generateText(
        prompt: String,
        history: List<ChatMessage>,
    ): Result<String> = try {
        // Gemini requires the history to start with a user turn and alternate. A prompt whose
        // reply failed leaves a dangling user message, so trim those off before replaying.
        val replay = history.dropLastWhile { it.author == Author.USER }
        val chat = model().startChat(
            history = replay.map { message ->
                content(role = if (message.author == Author.USER) "user" else "model") {
                    text(message.text)
                }
            },
        )
        val text = chat.sendMessage(prompt).text?.takeIf { it.isNotBlank() }
        if (text != null) {
            Result.success(text)
        } else {
            Result.failure(IllegalStateException("Empty response from Gemini"))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "generateContent failed", e)
        // The SDK's exception messages are user-readable ("model not found", "quota exceeded"),
        // so surface them rather than a generic string.
        Result.failure(e)
    }
}
