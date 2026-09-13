package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.data.security.ApiKeyStore
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-2.5-flash"

class GeminiRepositoryImpl(
    private val apiKeyStore: ApiKeyStore,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    private var model: GenerativeModel? = null
    private var chat: com.google.ai.client.generativeai.Chat? = null

    private suspend fun getChat(): com.google.ai.client.generativeai.Chat {

        if (chat != null) {
            return chat!!
        }

        val apiKey = apiKeyStore.getApiKey()
            ?: throw IllegalStateException(
                "Gemini API key is missing."
            )

        model = GenerativeModel(
            modelName = modelName,
            apiKey = apiKey
        )

        chat = model!!.startChat()

        return chat!!
    }

    override suspend fun generateText(
        prompt: String
    ): Result<String> = try {

        val response = getChat().sendMessage(prompt)

        val text = response.text
            ?.takeIf { it.isNotBlank() }

        if (text != null) {
            Result.success(text)
        } else {
            Result.failure(
                IllegalStateException(
                    "Empty response from Gemini"
                )
            )
        }

    } catch (e: CancellationException) {

        throw e

    } catch (e: Exception) {

        Log.e(
            TAG,
            "generateText failed",
            e
        )

        Result.failure(e)
    }
}