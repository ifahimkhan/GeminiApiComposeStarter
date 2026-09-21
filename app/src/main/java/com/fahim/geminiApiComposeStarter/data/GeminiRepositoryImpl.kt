package com.fahim.geminiApiComposeStarter.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GeminiRepositoryImpl(
    private val apiKeyProvider: suspend () -> String,
    private val modelName: String = "gemini-3.6-flash",
) : GeminiRepository {
    override suspend fun generateText(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = apiKeyProvider()
            val endpoint = "https://generativelanguage.googleapis.com/v1alpha/models/$modelName:generateContent?key=$apiKey"

            val body = JSONObject().put(
                "contents", JSONArray().put(
                    JSONObject().put(
                        "parts", JSONArray().put(
                            JSONObject().put("text", prompt)
                        )
                    )
                )
            ).toString()

            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 30_000
                readTimeout = 45_000
                doOutput = true
                outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }

            val code = conn.responseCode
            if (code != 200) {
                val err = conn.errorStream?.bufferedReader()?.readText()?.take(300) ?: "HTTP $code"
                return@withContext Result.failure(IllegalStateException("Gemini $code: $err"))
            }

            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            val text = json
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .takeIf { it.isNotBlank() }

            if (text == null) Result.failure(IllegalStateException("Empty response. Try again."))
            else Result.success(text)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Gemini error: ${e.message?.take(200) ?: e.javaClass.simpleName}"))
        }
    }
}
