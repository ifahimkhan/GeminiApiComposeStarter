package com.fahim.geminiApiComposeStarter.data

import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ProviderHttpException(val provider: AiProvider, val status: Int) :
    IllegalStateException("${provider.displayName} request failed (HTTP $status).")

class OpenAiCompatibleRepository(private val vault: ApiKeyVault) {
    suspend fun generateText(provider: AiProvider, prompt: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val key = vault.readForModel(provider)
                ?: return@withContext Result.failure(IllegalStateException("Add ${provider.keyProperty} to local.properties and rebuild."))
            val endpoint = when (provider) {
                AiProvider.OPENAI -> "https://api.openai.com/v1/chat/completions"
                AiProvider.DEEPSEEK -> "https://api.deepseek.com/chat/completions"
                AiProvider.GEMINI -> error("Gemini uses its own API")
            }
            val connection = URL(endpoint).openConnection() as HttpsURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 15_000
                connection.readTimeout = 60_000
                connection.doOutput = true
                connection.setRequestProperty("Authorization", "Bearer $key")
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("Accept", "application/json")
                val request = JSONObject()
                    .put("model", provider.modelId)
                    .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
                connection.outputStream.use { it.write(request.toString().toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                if (status !in 200..299) {
                    Result.failure(ProviderHttpException(provider, status))
                } else {
                    val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val answer = JSONObject(response).getJSONArray("choices")
                        .getJSONObject(0).getJSONObject("message").optString("content")
                    if (answer.isBlank()) Result.failure(IllegalStateException("${provider.displayName} returned an empty response."))
                    else Result.success(answer)
                }
            } finally {
                connection.disconnect()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}
