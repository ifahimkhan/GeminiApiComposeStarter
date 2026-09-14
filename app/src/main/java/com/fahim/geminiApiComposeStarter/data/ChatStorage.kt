package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import android.util.AtomicFile
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.runtime.Immutable
import java.io.File

@Immutable
data class Conversation(
    val id: String,
    val title: String,
    val messages: List<ChatMessage>,
    val draft: String = "",
)

interface ChatStorage {
    suspend fun load(): List<Conversation>
    suspend fun save(conversations: List<Conversation>)
}

/** Atomic, app-private files. No server or Android cloud backup. */
class FileChatStorage(context: Context) : ChatStorage {
    private val file = AtomicFile(File(context.noBackupFilesDir, "conversations.json"))
    private val mutex = Mutex()
    override suspend fun load(): List<Conversation> = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!file.baseFile.exists()) return@withLock emptyList()
            val root = JSONObject(file.openRead().bufferedReader().use { it.readText() })
            require(root.getInt("version") == 1) { "Unsupported chat history format" }
            val chats = root.getJSONArray("chats")
            List(chats.length()) { index ->
                val chat = chats.getJSONObject(index)
                val messages = chat.getJSONArray("messages")
                Conversation(chat.getString("id"), chat.getString("title"),
                    List(messages.length()) { i ->
                        val message = messages.getJSONObject(i)
                        ChatMessage(
                            id = message.getLong("id"),
                            role = ChatRole.valueOf(message.getString("role")),
                            text = message.getString("text"),
                            kind = runCatching { com.fahim.geminiApiComposeStarter.ui.chat.MessageKind.valueOf(message.optString("kind", "TEXT")) }
                                .getOrDefault(com.fahim.geminiApiComposeStarter.ui.chat.MessageKind.TEXT),
                            imagePath = message.optString("imagePath").takeIf { it.isNotBlank() },
                            imageState = message.optString("imageState").takeIf { it.isNotBlank() }?.let {
                                runCatching { com.fahim.geminiApiComposeStarter.ui.chat.ImageState.valueOf(it) }.getOrNull()
                            },
                            attachments = message.optJSONArray("attachments")?.let { attachments ->
                                List(attachments.length()) { attachmentIndex ->
                                    val attachment = attachments.getJSONObject(attachmentIndex)
                                    com.fahim.geminiApiComposeStarter.ui.chat.PendingAttachment(
                                        uri = attachment.getString("uri"),
                                        name = attachment.getString("name"),
                                        mimeType = attachment.getString("mimeType"),
                                    )
                                }
                            }.orEmpty(),
                        )
                    }, "")
            }
        }
    }
    override suspend fun save(conversations: List<Conversation>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val chats = JSONArray()
            conversations.forEach { chat ->
                val messages = JSONArray()
                chat.messages.forEach { message ->
                    val attachments = JSONArray()
                    message.attachments.forEach { attachment ->
                        attachments.put(
                            JSONObject()
                                .put("uri", attachment.uri)
                                .put("name", attachment.name)
                                .put("mimeType", attachment.mimeType)
                        )
                    }
                    messages.put(JSONObject().put("id", message.id).put("role", message.role.name)
                        .put("text", message.text).put("kind", message.kind.name)
                        .put("imagePath", message.imagePath ?: "").put("imageState", message.imageState?.name ?: "")
                        .put("attachments", attachments))
                }
                chats.put(JSONObject().put("id", chat.id).put("title", chat.title).put("draft", "").put("messages", messages))
            }
            val bytes = JSONObject().put("version", 1).put("chats", chats).toString().toByteArray(Charsets.UTF_8)
            val output = file.startWrite()
            try {
                output.write(bytes)
                file.finishWrite(output)
            } catch (error: Exception) {
                file.failWrite(output)
                throw error
            }
        }
    }
}
