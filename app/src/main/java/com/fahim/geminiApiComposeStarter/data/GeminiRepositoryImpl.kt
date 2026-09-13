package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.security.KeyStoreHelper
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-2.5-flash"

class GeminiRepositoryImpl(
    apiKey: String,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    private val keyStoreHelper = KeyStoreHelper()

    // Encrypt the API key in memory upon initialization using KeyStore
    private val encryptedData: Pair<ByteArray, ByteArray> = keyStoreHelper.encrypt(apiKey)

    private fun getGenerativeModel(): GenerativeModel {
        // Decrypt in memory only at the moment the GenerativeModel is created[cite: 1]
        val decryptedKey = keyStoreHelper.decrypt(encryptedData.first, encryptedData.second)
        return GenerativeModel(modelName = modelName, apiKey = decryptedKey)
    }

    override suspend fun generateText(prompt: String): Result<String> = try {
        val model = getGenerativeModel()
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