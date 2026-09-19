package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

/**
 * @param apiKeyProvider returns the decrypted key. It is called lazily, the first time a request
 * is made, so the plain-text key is only held in memory by the GenerativeModel.
 */
class GeminiRepositoryImpl(
    private val apiKeyProvider: suspend () -> String,
    private val modelName: String = DEFAULT_MODEL,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : GeminiRepository {

    private val modelLock = Mutex()
    private var model: GenerativeModel? = null

    private suspend fun getModel(): GenerativeModel? = modelLock.withLock {
        model ?: apiKeyProvider()
            .takeIf { it.isNotBlank() }
            ?.let { GenerativeModel(modelName = modelName, apiKey = it) }
            .also { model = it }
    }

    // Network call runs on the IO dispatcher, never on the main thread.
    override suspend fun generateText(prompt: String): Result<String> = withContext(ioDispatcher) {
        try {
            val model = getModel()
                ?: return@withContext Result.failure(IllegalStateException(MISSING_API_KEY_MESSAGE))
            val text = model.generateContent(prompt).text?.takeIf { it.isNotBlank() }
            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Empty response from Gemini"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "generateContent failed", e)
            Result.failure(e)
        }
    }
}
