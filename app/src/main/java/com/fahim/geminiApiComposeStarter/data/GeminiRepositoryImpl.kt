package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"
private const val API_BASE_URL =
    "https://generativelanguage.googleapis.com/v1beta/models"

class GeminiRepositoryImpl(
    private val apiKey: String,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    override suspend fun generateText(prompt: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                requestText(prompt)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "generateContent failed", e)
                Result.failure(
                    IllegalStateException(
                        e.message ?: "Could not connect to Gemini. Check your internet connection.",
                        e,
                    )
                )
            }
        }

    private fun requestText(prompt: String): Result<String> {
        val connection = URL("$API_BASE_URL/$modelName:generateContent")
            .openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 60_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("x-goog-api-key", apiKey)

            val requestBody = JSONObject()
                .put(
                    "contents",
                    org.json.JSONArray().put(
                        JSONObject().put(
                            "parts",
                            org.json.JSONArray().put(
                                JSONObject().put("text", prompt)
                            )
                        )
                    )
                )
                .toString()

            connection.outputStream.bufferedWriter(Charsets.UTF_8).use {
                it.write(requestBody)
            }

            val statusCode = connection.responseCode
            val responseBody = (
                if (statusCode in 200..299) connection.inputStream
                else connection.errorStream
            )?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (statusCode !in 200..299) {
                val apiMessage = runCatching {
                    JSONObject(responseBody).getJSONObject("error").getString("message")
                }.getOrNull()

                return Result.failure(
                    IllegalStateException(
                        apiMessage ?: "Gemini request failed (HTTP $statusCode)."
                    )
                )
            }

            val responseJson = JSONObject(responseBody)
            val parts = responseJson
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")

            val text = buildString {
                for (index in 0 until parts.length()) {
                    parts.getJSONObject(index).optString("text")
                        .takeIf { it.isNotBlank() }
                        ?.let { append(it) }
                }
            }

            if (text.isBlank()) {
                Result.failure(IllegalStateException("Gemini returned an empty response."))
            } else {
                Result.success(text)
            }
        } finally {
            connection.disconnect()
        }
    }
}
