package com.example.n075nishantassgn1.data

import com.example.n075nishantassgn1.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

interface GeminiRepository {
    suspend fun sendMessage(message: String): String
}

class FirebaseGeminiRepository(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY
) : GeminiRepository {

    override suspend fun sendMessage(message: String): String = withContext(Dispatchers.IO) {
        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 25000

            val jsonPayload = JSONObject().apply {
                val parts = JSONArray().put(JSONObject().put("text", message))
                val contentObj = JSONObject().put("parts", parts)
                put("contents", JSONArray().put(contentObj))
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(jsonPayload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
                val jsonResponse = JSONObject(responseText)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "No response text.")
                    }
                }
                "No response received."
            } else {
                val errorStream = conn.errorStream
                val errorText = if (errorStream != null) {
                    BufferedReader(InputStreamReader(errorStream, Charsets.UTF_8)).use { it.readText() }
                } else {
                    "HTTP $responseCode"
                }
                "Error ($responseCode): $errorText"
            }
        } catch (e: Exception) {
            "Error: ${e.message ?: "Failed to connect to Gemini API"}"
        }
    }
}