package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Saves all conversations to a JSON file in the app's private storage so chats survive
 * closing the app / restarting the phone. Also remembers the dark-mode choice.
 */
class ChatStore(context: Context) {

    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, "conversations.json")
    private val prefs = appContext.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)

    /** null = follow the system setting. */
    var darkMode: Boolean?
        get() = if (prefs.contains(KEY_DARK)) prefs.getBoolean(KEY_DARK, false) else null
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_DARK) else putBoolean(KEY_DARK, value)
            }.apply()
        }

    @Synchronized
    fun load(): List<Conversation> = try {
        if (!file.exists()) {
            emptyList()
        } else {
            val arr = JSONArray(file.readText())
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                val msgs = o.getJSONArray("messages")
                Conversation(
                    id = o.getLong("id"),
                    title = o.getString("title"),
                    updatedAt = o.getLong("updatedAt"),
                    messages = List(msgs.length()) { j ->
                        val m = msgs.getJSONObject(j)
                        ChatMessage(m.getLong("id"), m.getString("text"), m.getBoolean("isUser"))
                    },
                )
            }
        }
    } catch (e: Exception) {
        emptyList()
    }

    @Synchronized
    fun save(conversations: List<Conversation>) {
        val arr = JSONArray()
        conversations.forEach { c ->
            val msgs = JSONArray()
            c.messages.forEach { m ->
                msgs.put(
                    JSONObject()
                        .put("id", m.id)
                        .put("text", m.text)
                        .put("isUser", m.isUser)
                )
            }
            arr.put(
                JSONObject()
                    .put("id", c.id)
                    .put("title", c.title)
                    .put("updatedAt", c.updatedAt)
                    .put("messages", msgs)
            )
        }
        // Write to a temp file first so a crash mid-write can't corrupt the saved chats.
        val tmp = File(appContext.filesDir, "conversations.json.tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(file)
    }

    private companion object {
        const val KEY_DARK = "dark_mode"
    }
}
