package com.n149.geminichat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.n149.geminichat.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ChatViewModel — single source of truth for chat sessions and messages.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: GeminiRepositoryInterface,
    private val messageDao: MessageDao,
    private val feedbackDao: FeedbackDao,
    private val personalisationEngine: PersonalisationEngine
) : ViewModel() {

    private val currentSessionIdFlow = MutableStateFlow("default")
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Observe all messages and combine with currentSessionId to produce:
        // 1. messages for the active conversation
        // 2. session summaries for the slidebar history
        viewModelScope.launch {
            combine(
                messageDao.getAllMessages(),
                currentSessionIdFlow,
                feedbackDao.getAllFeedback()
            ) { allMessages, activeSessionId, allFeedback ->
                // Compute session summaries
                val grouped = allMessages.groupBy { it.sessionId }
                val sessionList = grouped.map { (sessId, msgs) ->
                    val firstUserMsg = msgs.firstOrNull { it.role == "user" }?.content
                    val title = if (!firstUserMsg.isNullOrBlank()) {
                        if (firstUserMsg.length > 36) firstUserMsg.take(34) + "…" else firstUserMsg
                    } else "New Conversation"

                    val latestTimestamp = msgs.maxOfOrNull { it.timestamp } ?: System.currentTimeMillis()
                    ChatSessionItem(
                        sessionId = sessId,
                        title = title,
                        timestamp = latestTimestamp,
                        messageCount = msgs.size
                    )
                }.sortedByDescending { it.timestamp }

                // Filter messages for current session
                val activeMessages = allMessages.filter { it.sessionId == activeSessionId }
                val feedbackMap = allFeedback.associateBy { it.messageId }

                // Detect topic from recent messages
                val userMsgs = activeMessages.filter { it.role == "user" }.map { it.content }
                val topicLabel = personalisationEngine.detectTopicLabel(userMsgs)

                _uiState.update { s ->
                    s.copy(
                        messages = activeMessages,
                        sessions = sessionList,
                        currentSessionId = activeSessionId,
                        feedbackMap = feedbackMap,
                        detectedTopic = topicLabel
                    )
                }
            }.collect()
        }
    }

    // ── Session Management for Slidebar ─────────────────────────────────────

    /** Starts a fresh new chat session */
    fun startNewChat() {
        val newSessionId = "session_" + System.currentTimeMillis()
        currentSessionIdFlow.value = newSessionId
        _uiState.update { it.copy(currentSessionId = newSessionId, inputText = "", errorMessage = null) }
    }

    /** Switches active chat to a chosen conversation from the slidebar */
    fun selectSession(sessionId: String) {
        currentSessionIdFlow.value = sessionId
        _uiState.update { it.copy(currentSessionId = sessionId, errorMessage = null) }
    }

    /** Deletes an entire conversation session */
    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            messageDao.deleteSession(sessionId)
            if (currentSessionIdFlow.value == sessionId) {
                // If active session was deleted, switch to the newest remaining or start fresh
                val remaining = _uiState.value.sessions.filter { it.sessionId != sessionId }
                if (remaining.isNotEmpty()) {
                    currentSessionIdFlow.value = remaining.first().sessionId
                } else {
                    startNewChat()
                }
            }
        }
    }

    /** Clears all chat history across all sessions */
    fun clearAllHistory() {
        viewModelScope.launch {
            messageDao.clearAll()
            feedbackDao.clearAll()
            startNewChat()
        }
    }

    // ── Input ──────────────────────────────────────────────────────────────

    fun onInputChanged(text: String) = _uiState.update { it.copy(inputText = text) }

    // ── Send message ───────────────────────────────────────────────────────

    fun sendMessage(text: String = _uiState.value.inputText) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _uiState.value.isLoading) return

        val activeSession = currentSessionIdFlow.value

        viewModelScope.launch {
            // 1. Persist user message in current session
            messageDao.insertMessage(
                MessageEntity(
                    role = "user",
                    content = trimmed,
                    sessionId = activeSession
                )
            )
            _uiState.update { it.copy(isLoading = true, inputText = "", errorMessage = null) }

            try {
                // 2. Gather recent user questions + typed feedback texts
                val recentUserMsgs = messageDao.getRecentMessagesForSession(activeSession, 20)
                    .filter { it.role == "user" }
                    .map { it.content }

                val recentFeedbackTexts = feedbackDao.getRecentFeedbackTexts(limit = 10)

                // 3. Build personalised system prompt
                val systemPrompt = personalisationEngine.buildSystemPrompt(
                    recentUserMessages  = recentUserMsgs,
                    recentFeedbackTexts = recentFeedbackTexts
                )

                // 4. Update topic label
                val topicLabel = personalisationEngine.detectTopicLabel(recentUserMsgs)
                _uiState.update { it.copy(detectedTopic = topicLabel) }

                // 5. Call Gemini with history of current session + personalised prompt
                val history = messageDao.getRecentMessagesForSession(activeSession, 30).reversed()
                val reply = repository.sendMessage(history, trimmed, systemPrompt)

                messageDao.insertMessage(
                    MessageEntity(
                        role = "model",
                        content = reply,
                        sessionId = activeSession
                    )
                )
                _uiState.update { it.copy(isLoading = false) }

            } catch (e: Exception) {
                val errorMsg = e.message ?: ""
                val friendlyError = when {
                    errorMsg.contains("429") || errorMsg.contains("Quota") || errorMsg.contains("RESOURCE_EXHAUSTED") ->
                        "Quota limit reached (429). Please wait a moment or create a new key."
                    errorMsg.contains("API_KEY_INVALID") ->
                        "Invalid API Key. Please verify GEMINI_API_KEY in local.properties."
                    else -> errorMsg.ifBlank { "Failed to get response from Gemini." }
                }

                messageDao.insertMessage(
                    MessageEntity(
                        role = "model",
                        content = "",
                        errorMessage = friendlyError,
                        sessionId = activeSession
                    )
                )
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = friendlyError)
                }
            }
        }
    }

    // ── Feedback ───────────────────────────────────────────────────────────

    fun openFeedback(messageId: Long) =
        _uiState.update { it.copy(pendingFeedbackMessageId = messageId) }

    fun dismissFeedback() =
        _uiState.update { it.copy(pendingFeedbackMessageId = null) }

    fun submitFeedback(messageId: Long, feedbackText: String) {
        val trimmed = feedbackText.trim()
        if (trimmed.isBlank()) { dismissFeedback(); return }
        viewModelScope.launch {
            feedbackDao.insertFeedback(
                FeedbackEntity(messageId = messageId, userText = trimmed)
            )
            _uiState.update { it.copy(pendingFeedbackMessageId = null) }
        }
    }

    fun deleteFeedback(messageId: Long) {
        viewModelScope.launch { feedbackDao.deleteFeedbackForMessage(messageId) }
    }

    // ── Misc ───────────────────────────────────────────────────────────────

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }

    fun onVoiceResult(spokenText: String) {
        _uiState.update { it.copy(inputText = spokenText, isListening = false) }
        sendMessage(spokenText)
    }

    fun setListening(l: Boolean) = _uiState.update { it.copy(isListening = l) }
}
