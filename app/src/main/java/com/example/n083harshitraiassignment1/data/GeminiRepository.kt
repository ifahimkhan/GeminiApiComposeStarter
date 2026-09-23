package com.example.n083harshitraiassignment1.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

open class GeminiRepository(
    private val apiKey: String
) {

    companion object {
        private const val TAG = "GeminiRepository"
    }

    open suspend fun generateResponse(
        prompt: String
    ): String = withContext(Dispatchers.IO) {

        val url = URL(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=$apiKey"
        )

        Log.d(TAG, "Requesting Gemini response for: $prompt")

        val connection =
            url.openConnection() as HttpURLConnection

        try {

            connection.requestMethod = "POST"

            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )

            connection.setRequestProperty(
                "X-goog-api-key",
                apiKey
            )

            connection.doOutput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 60000

            val requestBody = JSONObject().apply {

                put(
                    "contents",
                    org.json.JSONArray().apply {

                        put(
                            JSONObject().apply {

                                put(
                                    "parts",
                                    org.json.JSONArray().apply {

                                        put(
                                            JSONObject().apply {
                                                put(
                                                    "text",
                                                    prompt
                                                )
                                            }
                                        )
                                    }
                                )
                            }
                        )
                    }
                )
            }

            connection.outputStream.use { outputStream ->

                outputStream.write(
                    requestBody
                        .toString()
                        .toByteArray(Charsets.UTF_8)
                )
            }

            val responseCode =
                connection.responseCode

            Log.d(TAG, "Gemini HTTP response code: $responseCode")

            val responseText =
                if (responseCode in 200..299) {

                    connection.inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }

                } else {

                    connection.errorStream
                        ?.bufferedReader()
                        ?.use {
                            it.readText()
                        }
                        ?: "Unknown API error"
                }

            if (responseCode !in 200..299) {
                Log.e(TAG, "Gemini API error ($responseCode): $responseText")
                throw Exception(
                    "Gemini API Error $responseCode: $responseText"
                )
            }

            val json =
                JSONObject(responseText)

            val candidates =
                json.optJSONArray("candidates")

            if (
                candidates == null ||
                candidates.length() == 0
            ) {
                return@withContext "Gemini returned no response."
            }

            val firstCandidate =
                candidates.getJSONObject(0)

            val content =
                firstCandidate.optJSONObject("content")

            val parts =
                content?.optJSONArray("parts")

            if (
                parts == null ||
                parts.length() == 0
            ) {
                return@withContext "Gemini returned no text."
            }

            var text = ""
            for (i in 0 until parts.length()) {
                val candidateText = parts.getJSONObject(i).optString("text")
                if (candidateText.isNotBlank()) {
                    text = candidateText
                    break
                }
            }

            Log.d(TAG, "Gemini response text: $text")

            if (text.isBlank()) {
                "Gemini returned an empty response."
            } else {
                text
            }

        } finally {

            connection.disconnect()
        }
    }
}