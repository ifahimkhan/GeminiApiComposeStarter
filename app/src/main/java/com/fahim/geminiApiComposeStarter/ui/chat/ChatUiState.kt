package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.*

enum class Destination { HOME, CHAT, VOICE, HISTORY, SEARCH, SETTINGS }
enum class VoiceStatus { READY, WAITING, TRANSCRIPT, CANCELLED, UNAVAILABLE }
enum class Sheet { NONE, MODEL, HOST, ADD, NOTICES, ASSISTANT }
data class ChatUiState(
    val destination: Destination = Destination.HOME,
    val conversationId: String? = null,
    val category: Category = Category.GENERAL,
    val prompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val conversations: List<Conversation> = emptyList(),
    val searchResults: List<Conversation> = emptyList(),
    val searchQuery: String = "",
    val preferences: UserPreferences = UserPreferences(),
    val isInitializing: Boolean = true,
    val isLoading: Boolean = false,
    val pendingTurnId: String? = null,
    val promptError: String? = null,
    val errorMessage: String? = null,
    val voiceStatus: VoiceStatus = VoiceStatus.READY,
    val transcript: String = "",
    val sheet: Sheet = Sheet.NONE,
)
