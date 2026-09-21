package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    /**
     * Resolves the plaintext API key only at the moment it's needed. In production this
     * decrypts the Keystore-encrypted key (see [UserPreferencesRepository.resolveApiKey]);
     * the decrypted value lives only in this in-memory [GenerativeModel] instance and is
     * never logged or displayed.
     */
    private val resolveApiKey: suspend () -> String,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    @Volatile private var cachedModel: GenerativeModel? = null

    private suspend fun getOrCreateModel(): GenerativeModel {
        cachedModel?.let { return it }
        val model = GenerativeModel(modelName = modelName, apiKey = resolveApiKey())
        cachedModel = model
        return model
    }

    override suspend fun generateText(prompt: String): Result<String> = try {
        val response = getOrCreateModel().generateContent(prompt)
        val text = response.text?.takeIf { it.isNotBlank() }
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
