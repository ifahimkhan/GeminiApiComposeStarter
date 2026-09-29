package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Chat",
    val messages: List<ChatMessage> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis(),
)

open class ChatStorage(context: Context? = null) {
    private val prefs = context?.getSharedPreferences("gemini_chat_store", Context.MODE_PRIVATE)

    open fun loadSessions(): List<ChatSession> {
        val jsonString = prefs?.getString(KEY_SESSIONS, null)
        if (jsonString == null) {
            val legacyMessages = loadLegacyHistory()
            return if (legacyMessages.isNotEmpty()) {
                val firstTitle = legacyMessages.firstOrNull { it.isUser }?.text?.take(30) ?: "Chat 1"
                val session = ChatSession(title = firstTitle, messages = legacyMessages)
                saveSessions(listOf(session))
                listOf(session)
            } else {
                emptyList()
            }
        }

        return try {
            val jsonArray = JSONArray(jsonString)
            val sessions = mutableListOf<ChatSession>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val title = obj.optString("title", "New Chat")
                val updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())

                val msgArray = obj.optJSONArray("messages") ?: JSONArray()
                val messages = mutableListOf<ChatMessage>()
                for (j in 0 until msgArray.length()) {
                    val msgObj = msgArray.getJSONObject(j)
                    val text = msgObj.optString("text", "")
                    val isUser = msgObj.optBoolean("isUser", false)

                    val attArray = msgObj.optJSONArray("attachments") ?: JSONArray()
                    val attachments = mutableListOf<ChatAttachment>()
                    for (k in 0 until attArray.length()) {
                        val attObj = attArray.getJSONObject(k)
                        attachments.add(
                            ChatAttachment(
                                uriString = attObj.optString("uriString", ""),
                                name = attObj.optString("name", "attachment"),
                                isImage = attObj.optBoolean("isImage", true),
                                mimeType = attObj.optString("mimeType", ""),
                            )
                        )
                    }

                    messages.add(ChatMessage(text = text, isUser = isUser, attachments = attachments))
                }
                sessions.add(ChatSession(id = id, title = title, messages = messages, updatedAt = updatedAt))
            }
            sessions.sortedByDescending { it.updatedAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    open fun saveSessions(sessions: List<ChatSession>) {
        val p = prefs ?: return
        try {
            val jsonArray = JSONArray()
            for (session in sessions) {
                val sessionObj = JSONObject().apply {
                    put("id", session.id)
                    put("title", session.title)
                    put("updatedAt", session.updatedAt)

                    val msgArray = JSONArray()
                    for (msg in session.messages) {
                        val msgObj = JSONObject().apply {
                            put("text", msg.text)
                            put("isUser", msg.isUser)

                            val attArray = JSONArray()
                            for (att in msg.attachments) {
                                val attObj = JSONObject().apply {
                                    put("uriString", att.uriString)
                                    put("name", att.name)
                                    put("isImage", att.isImage)
                                    put("mimeType", att.mimeType)
                                }
                                attArray.put(attObj)
                            }
                            put("attachments", attArray)
                        }
                        msgArray.put(msgObj)
                    }
                    put("messages", msgArray)
                }
                jsonArray.put(sessionObj)
            }
            p.edit().putString(KEY_SESSIONS, jsonArray.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun loadLegacyHistory(): List<ChatMessage> {
        val jsonString = prefs?.getString("key_chat_history", null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val messages = mutableListOf<ChatMessage>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                messages.add(
                    ChatMessage(
                        text = obj.optString("text", ""),
                        isUser = obj.optBoolean("isUser", false),
                    )
                )
            }
            messages
        } catch (e: Exception) {
            emptyList()
        }
    }

    open fun loadSelectedModel(default: String = "gemini-3.8-flash"): String {
        return prefs?.getString(KEY_SELECTED_MODEL, default) ?: default
    }

    open fun saveSelectedModel(model: String) {
        prefs?.edit()?.putString(KEY_SELECTED_MODEL, model)?.apply()
    }

    open fun loadActiveSessionId(): String? {
        return prefs?.getString(KEY_ACTIVE_SESSION_ID, null)
    }

    open fun saveActiveSessionId(id: String) {
        prefs?.edit()?.putString(KEY_ACTIVE_SESSION_ID, id)?.apply()
    }

    companion object {
        private const val KEY_SESSIONS = "key_chat_sessions_v2"
        private const val KEY_SELECTED_MODEL = "key_selected_model"
        private const val KEY_ACTIVE_SESSION_ID = "key_active_session_id"
    }
}
