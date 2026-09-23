package com.fahim.geminiApiComposeStarter.data.repository

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.ConversationDao
import com.fahim.geminiApiComposeStarter.data.local.ConversationEntity
import com.fahim.geminiApiComposeStarter.data.local.QuizAttemptDao
import com.fahim.geminiApiComposeStarter.data.local.QuizAttemptEntity
import com.fahim.geminiApiComposeStarter.model.ChatMessage
import com.fahim.geminiApiComposeStarter.model.Conversation
import com.fahim.geminiApiComposeStarter.model.MessageRole
import com.fahim.geminiApiComposeStarter.model.QuizAttempt
import com.fahim.geminiApiComposeStarter.model.StudyMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatHistoryRepository(
    private val messageDao: ChatMessageDao,
    private val conversationDao: ConversationDao? = null,
    private val quizDao: QuizAttemptDao? = null,
) {

    // ── Messages ─────────────────────────────────────────────────────────────

    fun getAllMessages(): Flow<List<ChatMessage>> = messageDao.getAllMessages().map { entities ->
        entities.map { it.toDomain() }
    }

    fun getMessagesForConversation(conversationId: String): Flow<List<ChatMessage>> =
        messageDao.getMessagesForConversation(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }

    fun getSavedMessages(): Flow<List<ChatMessage>> =
        messageDao.getSavedMessages().map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun insertMessage(message: ChatMessage) {
        messageDao.insertMessage(message.toEntity())
        conversationDao?.updateTimestamp(message.conversationId)
    }

    suspend fun setSavedStatus(messageId: String, isSaved: Boolean) {
        messageDao.updateSavedStatus(messageId, isSaved)
    }

    suspend fun clearConversation(conversationId: String) {
        messageDao.clearConversation(conversationId)
    }

    suspend fun clearAll() {
        messageDao.clearAll()
    }

    // ── Sessions / Conversations ─────────────────────────────────────────────

    fun getAllConversations(): Flow<List<Conversation>> {
        return conversationDao?.getAllConversations()?.map { entities ->
            entities.map { it.toDomain() }
        } ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    suspend fun createConversation(
        title: String,
        mode: StudyMode = StudyMode.CHAT,
    ): Conversation {
        val conversation = Conversation(title = title, mode = mode)
        conversationDao?.insertConversation(conversation.toEntity())
        return conversation
    }

    suspend fun updateConversationTitle(id: String, newTitle: String) {
        conversationDao?.updateTitle(id, newTitle)
    }

    suspend fun deleteConversation(id: String) {
        messageDao.clearConversation(id)
        conversationDao?.deleteConversation(id)
    }

    // ── Quiz Attempts & Study Insights ───────────────────────────────────────

    fun getAllQuizAttempts(): Flow<List<QuizAttempt>> {
        return quizDao?.getAllQuizAttempts()?.map { entities ->
            entities.map { it.toDomain() }
        } ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    suspend fun recordQuizAttempt(attempt: QuizAttempt) {
        quizDao?.insertQuizAttempt(attempt.toEntity())
    }
}

// ── Mappers ──────────────────────────────────────────────────────────────────

private fun ChatMessageEntity.toDomain(): ChatMessage = ChatMessage(
    id = id,
    conversationId = conversationId,
    role = MessageRole.valueOf(role),
    content = content,
    timestamp = timestamp,
    isSaved = isSaved,
    imageUri = imageUri,
)

private fun ChatMessage.toEntity(): ChatMessageEntity = ChatMessageEntity(
    id = id,
    conversationId = conversationId,
    role = role.name,
    content = content,
    timestamp = timestamp,
    isSaved = isSaved,
    imageUri = imageUri,
)

private fun ConversationEntity.toDomain(): Conversation = Conversation(
    id = id,
    title = title,
    createdAt = createdAt,
    updatedAt = updatedAt,
    mode = try { StudyMode.valueOf(mode) } catch (_: Exception) { StudyMode.CHAT },
)

private fun Conversation.toEntity(): ConversationEntity = ConversationEntity(
    id = id,
    title = title,
    createdAt = createdAt,
    updatedAt = updatedAt,
    mode = mode.name,
)

private fun QuizAttemptEntity.toDomain(): QuizAttempt = QuizAttempt(
    id = id,
    conversationId = conversationId,
    topic = topic,
    score = score,
    totalQuestions = totalQuestions,
    percentage = percentage,
    timestamp = timestamp,
    strongAreas = strongAreas,
    revisionTopics = revisionTopics,
)

private fun QuizAttempt.toEntity(): QuizAttemptEntity = QuizAttemptEntity(
    id = id,
    conversationId = conversationId,
    topic = topic,
    score = score,
    totalQuestions = totalQuestions,
    percentage = percentage,
    timestamp = timestamp,
    strongAreas = strongAreas,
    revisionTopics = revisionTopics,
)
