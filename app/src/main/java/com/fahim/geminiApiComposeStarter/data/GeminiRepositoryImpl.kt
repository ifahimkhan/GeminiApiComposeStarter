package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.data.model.Author
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-2.5-flash"

/** Newest turns kept as context. Trimming the tail keeps request size, cost and latency down. */
private const val MAX_HISTORY_TURNS = 20

class GeminiRepositoryImpl(
    private val modelName: String = DEFAULT_MODEL,
    private val apiKeyProvider: suspend () -> String,
) : GeminiRepository {

    private val lock = Mutex()
    private var cachedModel: GenerativeModel? = null

    override suspend fun generateReply(
        history: List<ChatMessage>,
        prompt: String,
    ): Result<String> = try {
        val chat = model().startChat(history = history.toAlternatingContents())
        val text = chat.sendMessage(prompt).text?.takeIf { it.isNotBlank() }
        if (text != null) Result.success(text) else Result.failure(EmptyResponseException())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Log the type only. The message can echo back request details, and nothing that
        // could contain the API key should ever reach logcat.
        Log.e(TAG, "Gemini call failed: ${e.javaClass.simpleName}")
        Result.failure(e)
    }

    /**
     * Decrypts the key and builds the model the first time it is needed, then reuses it.
     * The plaintext key stays in this function's scope and in the SDK's own memory.
     */
    private suspend fun model(): GenerativeModel = lock.withLock {
        cachedModel ?: run {
            val apiKey = apiKeyProvider().trim()
            if (apiKey.isEmpty()) throw MissingApiKeyException()
            GenerativeModel(modelName = modelName, apiKey = apiKey).also { cachedModel = it }
        }
    }

    /**
     * Gemini requires history to alternate user/model turns and to start with a user turn.
     * A failed request can leave a trailing user turn in the local database, so consecutive
     * same-role turns are merged and any trailing user turn is dropped: the caller is about
     * to send that text as the new prompt anyway.
     */
    private fun List<ChatMessage>.toAlternatingContents(): List<Content> {
        val turns = mutableListOf<ChatMessage>()
        for (message in takeLast(MAX_HISTORY_TURNS)) {
            if (turns.isEmpty() && message.author != Author.USER) continue
            val previous = turns.lastOrNull()
            if (previous != null && previous.author == message.author) {
                turns[turns.lastIndex] = previous.copy(text = previous.text + "\n\n" + message.text)
            } else {
                turns += message
            }
        }
        while (turns.lastOrNull()?.author == Author.USER) {
            turns.removeAt(turns.lastIndex)
        }
        return turns.map { message ->
            content(role = if (message.author == Author.USER) "user" else "model") {
                text(message.text)
            }
        }
    }
}
