package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.chatHistoryDataStore by preferencesDataStore(name = "chat_history")
private val MESSAGES_KEY = stringPreferencesKey("messages_json")

/**
 * Persists the conversation as a JSON array in Preferences DataStore, so it survives
 * app restarts without needing a database annotation-processing toolchain.
 */
class ChatHistoryRepositoryImpl(private val context: Context) : ChatHistoryRepository {

    override fun observeMessages(): Flow<List<ChatMessage>> =
        context.chatHistoryDataStore.data.map { prefs -> decode(prefs[MESSAGES_KEY]) }

    override suspend fun addMessage(text: String, isFromUser: Boolean) {
        context.chatHistoryDataStore.edit { prefs ->
            val current = decode(prefs[MESSAGES_KEY])
            val nextId = (current.maxOfOrNull { it.id } ?: -1L) + 1L
            val updated = current + ChatMessage(id = nextId, text = text, isFromUser = isFromUser)
            prefs[MESSAGES_KEY] = encode(updated)
        }
    }

    private fun decode(json: String?): List<ChatMessage> {
        if (json.isNullOrBlank()) return emptyList()
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            ChatMessage(
                id = obj.getLong("id"),
                text = obj.getString("text"),
                isFromUser = obj.getBoolean("isFromUser"),
            )
        }
    }

    private fun encode(messages: List<ChatMessage>): String {
        val array = JSONArray()
        messages.forEach { msg ->
            array.put(
                JSONObject()
                    .put("id", msg.id)
                    .put("text", msg.text)
                    .put("isFromUser", msg.isFromUser)
            )
        }
        return array.toString()
    }
}
