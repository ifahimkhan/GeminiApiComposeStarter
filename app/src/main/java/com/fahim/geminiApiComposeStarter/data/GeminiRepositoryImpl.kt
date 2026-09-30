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
 * The API key is NOT passed in as a String. [apiKeyProvider] decrypts it only at the moment
 * the GenerativeModel is created. All network work runs on [ioDispatcher], off the main thread.
 */
class GeminiRepositoryImpl(
    private val apiKeyProvider: suspend () -> String,
    private val modelName: String = DEFAULT_MODEL,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : GeminiRepository {

    private val mutex = Mutex()
    private var model: GenerativeModel? = null

    private suspend fun model(): GenerativeModel = mutex.withLock {
        model ?: GenerativeModel(modelName = modelName, apiKey = apiKeyProvider()).also { model = it }
    }

    override suspend fun generateText(prompt: String): Result<String> = withContext(ioDispatcher) {
        try {
            val text = model().generateContent(prompt).text?.takeIf { it.isNotBlank() }
            if (text != null) Result.success(text)
            else Result.failure(IllegalStateException("Empty response from Gemini"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "generateContent failed: ${e.javaClass.simpleName}") // never log the key
            Result.failure(e)
        }
    }
}
