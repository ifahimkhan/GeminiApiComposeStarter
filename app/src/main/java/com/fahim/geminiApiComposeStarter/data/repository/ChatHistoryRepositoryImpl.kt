package com.fahim.geminiApiComposeStarter.data.repository

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.Participant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ChatHistoryRepositoryImpl(
    private val chatMessageDao: ChatMessageDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ChatHistoryRepository {

    override fun getMessages(): Flow<List<ChatMessage>> {
        return chatMessageDao.getAllMessages()
            .map { entities ->
                entities.map { entity ->
                    ChatMessage(
                        id = entity.messageId,
                        text = entity.text,
                        participant = if (entity.participant == Participant.USER.name) {
                            Participant.USER
                        } else {
                            Participant.MODEL
                        },
                        timestamp = entity.timestamp,
                    )
                }
            }
            .catch { emit(emptyList()) }
            .flowOn(ioDispatcher)
    }

    override suspend fun saveMessage(message: ChatMessage): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val entity = ChatMessageEntity(
                    messageId = message.id,
                    text = message.text,
                    participant = message.participant.name,
                    timestamp = message.timestamp,
                )
                chatMessageDao.insertMessage(entity)
                Unit
            }
        }

    override suspend fun clearHistory(): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                chatMessageDao.clearAllMessages()
                Unit
            }
        }
}
