package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.toDomain
import com.fahim.geminiApiComposeStarter.data.local.toEntity
import com.fahim.geminiApiComposeStarter.domain.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatHistoryRepositoryImpl(private val dao: ChatMessageDao) : ChatHistoryRepository {
    override fun observeMessages(): Flow<List<ChatMessage>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun addMessage(message: ChatMessage) {
        dao.insert(message.toEntity())
    }
}
