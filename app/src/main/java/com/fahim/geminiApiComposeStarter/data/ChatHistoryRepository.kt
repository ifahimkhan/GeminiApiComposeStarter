package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Persists chat turns with Room so conversation history survives app restarts. */
class ChatHistoryRepository(private val dao: ChatMessageDao) {

    val messages: Flow<List<ChatMessage>> = dao.observeAll().map { entities ->
        entities.map { entity ->
            ChatMessage(
                id = entity.id,
                role = ChatRole.valueOf(entity.role),
                text = entity.text,
                timestamp = entity.timestamp,
            )
        }
    }

    suspend fun append(role: ChatRole, text: String) {
        dao.insert(
            ChatMessageEntity(
                role = role.name,
                text = text,
                timestamp = System.currentTimeMillis(),
            )
        )
    }

    suspend fun clear() = dao.clearAll()
}
