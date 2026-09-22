package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.InvalidAPIKeyException
import com.google.ai.client.generativeai.type.PromptBlockedException
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.ai.client.generativeai.type.UnsupportedUserLocationException
import kotlinx.coroutines.CancellationException
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "GeminiRepository"
private const val REQUEST_TIMEOUT_MS = 45_000L

/** Preferred model first; the rest are tried in order when it is overloaded, slow or unavailable. */
val DEFAULT_MODELS = listOf("gemini-3.6-flash", "gemini-3.5-flash", "gemini-3.1-flash-lite")

/** Sends [prompt] to the named model and returns its text (null if the reply had none). */
typealias TextGenerator = suspend (modelName: String, prompt: String) -> String?

class GeminiRepositoryImpl internal constructor(
    private val modelNames: List<String>,
    private val generator: TextGenerator,
) : GeminiRepository {

    constructor(apiKey: String, modelNames: List<String> = DEFAULT_MODELS) :
        this(modelNames, SdkTextGenerator(apiKey)::generate)

    override suspend fun generateText(prompt: String): Result<String> {
        var lastError: Throwable? = null
        for ((index, modelName) in modelNames.withIndex()) {
            try {
                val text = generator(modelName, prompt)?.takeIf { it.isNotBlank() }
                if (text != null) return Result.success(text)
                lastError = IllegalStateException("Empty response from Gemini")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                if (!e.isWorthRetryingOnAnotherModel()) break
            }
            if (index < modelNames.lastIndex) {
                Log.w(TAG, "$modelName did not answer, trying ${modelNames[index + 1]}")
            }
        }
        val error = lastError ?: IllegalStateException("No Gemini model configured")
        Log.e(TAG, "generateContent failed", error)
        return Result.failure(error)
    }
}

/**
 * Overload (503), rate limits, timeouts and unknown models are model-specific, so another model may succeed.
 * A bad API key, a blocked prompt or an unsupported region would fail identically everywhere.
 */
internal fun Throwable.isWorthRetryingOnAnotherModel(): Boolean = when {
    this is InvalidAPIKeyException -> false
    this is PromptBlockedException -> false
    this is UnsupportedUserLocationException -> false
    message?.contains("API key", ignoreCase = true) == true -> false
    else -> true
}

private class SdkTextGenerator(private val apiKey: String) {
    private val models = ConcurrentHashMap<String, GenerativeModel>()

    suspend fun generate(modelName: String, prompt: String): String? {
        val model = models.getOrPut(modelName) {
            GenerativeModel(
                modelName = modelName,
                apiKey = apiKey,
                requestOptions = RequestOptions(timeout = REQUEST_TIMEOUT_MS),
            )
        }
        return model.generateContent(prompt).text
    }
}
