package com.fahim.geminiApiComposeStarter.data

import kotlinx.coroutines.flow.Flow

enum class Category(val title: String, val subtitle: String, val suggestion: String) {
    GENERAL("AI Host", "A little clarity, whenever you need it.", ""),
    RESEARCH("AI for Research Paper", "Smarter research, faster results with AI-powered assistance. — AI makes every step easier.", "Help me develop a research question and an outline for my paper."),
    HEALTH("AI for Health", "Explore health concepts with intelligent assistance and clearer explanations.", "Explain the importance of healthy daily habits in simple terms.")
}
data class Conversation(val id: String, val title: String, val category: Category, val preview: String = "", val updatedAt: Long = 0)
enum class Role { USER, ASSISTANT }
enum class Delivery { PENDING, SENT, FAILED, INTERRUPTED }
data class ChatMessage(val id: String, val conversationId: String, val turnId: String, val sequence: Long, val role: Role, val text: String, val delivery: Delivery = Delivery.SENT, val feedback: Int = 0)
data class UserPreferences(val displayName: String = "", val category: Category = Category.GENERAL, val reducedMotion: Boolean = false, val modelId: String = "")

interface ChatStore {
    fun conversations(query: String = ""): Flow<List<Conversation>>
    fun messages(conversationId: String): Flow<List<ChatMessage>>
    suspend fun create(conversation: Conversation)
    suspend fun appendUser(message: ChatMessage)
    suspend fun setDelivery(messageId: String, delivery: Delivery)
    suspend fun complete(user: ChatMessage, answer: String)
    suspend fun feedback(messageId: String, value: Int)
    suspend fun recoverInterrupted()
}
interface PreferenceStore {
    val preferences: Flow<UserPreferences>
    suspend fun save(value: UserPreferences)
}
