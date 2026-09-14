package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.security.ApiKeyStore
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

/**
 * [apiKeyStore] is decrypted only at the moment the [GenerativeModel] is first built (by
 * the `model` lazy delegate below) — never earlier, never logged. The plaintext key then
 * lives only inside the generativeai SDK's own in-memory client.
 */
class GeminiRepositoryImpl(
    private val apiKeyStore: ApiKeyStore,
    modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    private val model by lazy {
        GenerativeModel(modelName = modelName, apiKey = apiKeyStore.decryptedKey().orEmpty())
    }

    override suspend fun generateText(prompt: String): Result<String> = try {
        val response = model.generateContent(prompt)
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
