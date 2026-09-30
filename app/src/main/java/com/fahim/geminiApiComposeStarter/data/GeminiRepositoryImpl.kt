package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException

import com.fahim.geminiApiComposeStarter.BuildConfig

private const val DEFAULT_MODEL = "gemini-3.5-flash"

class GeminiRepositoryImpl(
    private val secureApiKeyStore: SecureApiKeyStore,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    override suspend fun generateText(prompt: String): Result<String> {
        return try {
        var apiKey = secureApiKeyStore.getApiKey()
        if (apiKey.isNullOrEmpty()) {
            val buildConfigKey = BuildConfig.GEMINI_API_KEY
            if (buildConfigKey.isNotEmpty()) {
                secureApiKeyStore.saveApiKey(buildConfigKey)
                apiKey = buildConfigKey
            }
        }

        if (apiKey.isNullOrEmpty()) {
            return Result.failure(IllegalStateException("GEMINI_API_KEY is missing. Add it to local.properties and rebuild."))
        }

        val model = GenerativeModel(modelName = modelName, apiKey = apiKey)
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
        Result.failure(e)
    }
}
}

