package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.net.UnknownHostException

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"
private const val MAX_HISTORY = 20

class GeminiRepositoryImpl(
    private val apiKey: String,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    // Lazy: nothing is built until the first real request.
    private val model by lazy { GenerativeModel(modelName = modelName, apiKey = apiKey) }

    override suspend fun generateText(prompt: String): Result<String> =
        runGemini { model.generateContent(prompt).text }

    override suspend fun chat(history: List<ChatMessage>, prompt: String): Result<String> =
        runGemini {
            val session = model.startChat(history = history.takeLast(MAX_HISTORY).toContentHistory())
            session.sendMessage(prompt).text
        }

    private suspend fun runGemini(call: suspend () -> String?): Result<String> = try {
        val text = withRetry { call() }?.takeIf { it.isNotBlank() }
        if (text != null) {
            Result.success(text)
        } else {
            Result.failure(IllegalStateException("Empty response from Gemini"))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Gemini request failed", e)
        Result.failure(IllegalStateException(e.toFriendlyMessage(), e))
    }

    /** Retries a couple of times when Google answers 503 "model overloaded". */
    private suspend fun <T> withRetry(attempts: Int = 3, block: suspend () -> T): T {
        var wait = 2000L
        repeat(attempts - 1) {
            try {
                return block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                if ("503" !in msg && "UNAVAILABLE" !in msg) throw e
                delay(wait)
                wait *= 2
            }
        }
        return block()
    }
}

/** Only complete user->model pairs are sent as context (Gemini requires alternating turns). */
private fun List<ChatMessage>.toContentHistory(): List<Content> {
    val result = mutableListOf<Content>()
    var i = 0
    while (i < size - 1) {
        val a = this[i]
        val b = this[i + 1]
        if (a.isUser && !b.isUser) {
            result += content("user") { text(a.text) }
            result += content("model") { text(b.text) }
            i += 2
        } else {
            i++
        }
    }
    return result
}

private fun Throwable.toFriendlyMessage(): String {
    val msg = message.orEmpty()
    return when {
        this is UnknownHostException || "Unable to resolve host" in msg ->
            "No internet connection."
        "503" in msg || "UNAVAILABLE" in msg ->
            "Gemini is busy right now. Please try again in a moment."
        "429" in msg || "RESOURCE_EXHAUSTED" in msg ->
            "Rate limit reached. Wait a bit and try again."
        "API key" in msg || "API_KEY_INVALID" in msg ->
            "The API key was rejected. Check GEMINI_API_KEY in local.properties."
        else -> msg.lineSequence().firstOrNull { it.isNotBlank() }?.take(200)
            ?: "Something went wrong"
    }
}
