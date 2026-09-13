package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private const val PREFS_NAME = "chat_store"
private const val CHATS_KEY = "chats"
private const val ACTIVE_CHAT_ID_KEY = "active_chat_id"

class ChatStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadChats(): List<StoredChat> {
        val rawChats = prefs.getString(CHATS_KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(rawChats)
            List(array.length()) { index -> array.getJSONObject(index).toStoredChat() }
        }.getOrElse { emptyList() }
    }

    fun saveChats(chats: List<StoredChat>, activeChatId: String) {
        val array = JSONArray()
        chats.forEach { chat -> array.put(chat.toJson()) }
        prefs.edit()
            .putString(CHATS_KEY, array.toString())
            .putString(ACTIVE_CHAT_ID_KEY, activeChatId)
            .apply()
    }

    fun loadActiveChatId(): String? = prefs.getString(ACTIVE_CHAT_ID_KEY, null)

    companion object {
        fun newChat(): StoredChat = StoredChat(
            id = UUID.randomUUID().toString(),
            title = "New chat",
            messages = emptyList(),
        )
    }
}

data class StoredChat(
    val id: String,
    val title: String,
    val messages: List<StoredMessage>,
)

data class StoredMessage(
    val id: Long,
    val text: String,
    val role: ConversationRole,
)

private fun StoredChat.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("title", title)
    .put(
        "messages",
        JSONArray().also { array ->
            messages.forEach { message -> array.put(message.toJson()) }
        },
    )

private fun StoredMessage.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("text", text)
    .put("role", role.name)

private fun JSONObject.toStoredChat(): StoredChat {
    val messagesJson = optJSONArray("messages") ?: JSONArray()
    return StoredChat(
        id = getString("id"),
        title = optString("title", "New chat"),
        messages = List(messagesJson.length()) { index -> messagesJson.getJSONObject(index).toStoredMessage() },
    )
}

private fun JSONObject.toStoredMessage(): StoredMessage = StoredMessage(
    id = getLong("id"),
    text = getString("text"),
    role = runCatching { ConversationRole.valueOf(getString("role")) }.getOrDefault(ConversationRole.USER),
)
