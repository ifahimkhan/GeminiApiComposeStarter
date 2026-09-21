package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

interface ChatStore {
    val messages: Flow<List<ChatMessageEntity>>
    suspend fun insert(message: ChatMessageEntity)
    suspend fun clear()
}
class RoomChatStore(private val dao: ChatDao) : ChatStore {
    override val messages = dao.getAllMessages()
    override suspend fun insert(message: ChatMessageEntity) = dao.insertMessage(message)
    override suspend fun clear() = dao.clearHistory()
}
