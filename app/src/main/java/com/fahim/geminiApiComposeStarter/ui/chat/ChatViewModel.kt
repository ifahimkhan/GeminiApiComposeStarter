package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatAttachment
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.ChatSession
import com.fahim.geminiApiComposeStarter.data.ChatStorage
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    application: Application,
    private val repository: GeminiRepository,
    private val storage: ChatStorage,
    private val hasApiKey: Boolean,
) : AndroidViewModel(application) {

    private val _uiState: MutableStateFlow<ChatUiState>

    init {
        val loadedSessions = storage.loadSessions()
        val savedSessionId = storage.loadActiveSessionId()
        val activeSession = loadedSessions.firstOrNull { it.id == savedSessionId }
            ?: loadedSessions.firstOrNull()
            ?: ChatSession()

        val allSessions = if (loadedSessions.any { it.id == activeSession.id }) {
            loadedSessions
        } else {
            listOf(activeSession) + loadedSessions
        }

        _uiState = MutableStateFlow(
            ChatUiState(
                sessions = allSessions,
                currentSessionId = activeSession.id,
                messages = activeSession.messages,
                selectedModel = storage.loadSelectedModel("gemini-3.8-flash"),
            )
        )
    }

    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onShowModelDialog(show: Boolean) {
        _uiState.update { it.copy(showModelDialog = show) }
    }

    fun onShowAttachmentPicker(show: Boolean) {
        _uiState.update { it.copy(showAttachmentPicker = show) }
    }

    fun onAddAttachments(attachments: List<ChatAttachment>) {
        if (attachments.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    pendingAttachments = it.pendingAttachments + attachments,
                    showAttachmentPicker = false,
                    promptError = null,
                )
            }
        }
    }

    fun onRemoveAttachment(attachment: ChatAttachment) {
        _uiState.update {
            it.copy(pendingAttachments = it.pendingAttachments.filterNot { a -> a.uriString == attachment.uriString })
        }
    }

    fun onSelectModel(modelName: String) {
        val trimmed = modelName.trim()
        if (trimmed.isNotBlank()) {
            storage.saveSelectedModel(trimmed)
            _uiState.update { it.copy(selectedModel = trimmed, showModelDialog = false) }
        }
    }

    fun onNewChat() {
        val newSession = ChatSession(
            id = UUID.randomUUID().toString(),
            title = "New Chat",
            messages = emptyList(),
            updatedAt = System.currentTimeMillis(),
        )
        val updatedSessions = listOf(newSession) + _uiState.value.sessions.filter { it.messages.isNotEmpty() }
        storage.saveSessions(updatedSessions)
        storage.saveActiveSessionId(newSession.id)

        _uiState.update {
            it.copy(
                sessions = updatedSessions,
                currentSessionId = newSession.id,
                messages = emptyList(),
                prompt = "",
                pendingAttachments = emptyList(),
                errorMessage = null,
                promptError = null,
            )
        }
    }

    fun onSwitchSession(sessionId: String) {
        val target = _uiState.value.sessions.firstOrNull { it.id == sessionId } ?: return
        storage.saveActiveSessionId(sessionId)
        _uiState.update {
            it.copy(
                currentSessionId = target.id,
                messages = target.messages,
                prompt = "",
                pendingAttachments = emptyList(),
                errorMessage = null,
                promptError = null,
            )
        }
    }

    fun onDeleteSession(sessionId: String) {
        val remaining = _uiState.value.sessions.filterNot { it.id == sessionId }
        val nextActive = if (_uiState.value.currentSessionId == sessionId) {
            remaining.firstOrNull() ?: ChatSession()
        } else {
            remaining.firstOrNull { it.id == _uiState.value.currentSessionId } ?: ChatSession()
        }
        val finalSessions = if (remaining.any { it.id == nextActive.id }) remaining else listOf(nextActive) + remaining

        storage.saveSessions(finalSessions)
        storage.saveActiveSessionId(nextActive.id)

        _uiState.update {
            it.copy(
                sessions = finalSessions,
                currentSessionId = nextActive.id,
                messages = nextActive.messages,
                prompt = "",
                pendingAttachments = emptyList(),
                errorMessage = null,
                promptError = null,
            )
        }
    }

    fun onClearChat() {
        onDeleteSession(_uiState.value.currentSessionId)
    }

    fun onSend() {
        val prompt = _uiState.value.prompt.trim()
        val pendingAttachments = _uiState.value.pendingAttachments

        if (prompt.isEmpty() && pendingAttachments.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return

        val effectivePrompt = if (prompt.isEmpty()) "Analyze this attachment" else prompt
        val currentModel = _uiState.value.selectedModel
        val currentSessionId = _uiState.value.currentSessionId
        val currentHistory = _uiState.value.messages
        val userMessage = ChatMessage(text = effectivePrompt, isUser = true, attachments = pendingAttachments)
        val updatedHistoryWithUser = currentHistory + userMessage

        val title = if (currentHistory.isEmpty()) {
            if (prompt.isNotBlank()) {
                if (prompt.length > 30) prompt.take(30) + "..." else prompt
            } else if (pendingAttachments.isNotEmpty()) {
                pendingAttachments.first().name
            } else {
                "Chat"
            }
        } else {
            _uiState.value.sessions.firstOrNull { it.id == currentSessionId }?.title ?: "Chat"
        }

        val updatedSessionsWithUser = _uiState.value.sessions.map { session ->
            if (session.id == currentSessionId) {
                session.copy(title = title, messages = updatedHistoryWithUser, updatedAt = System.currentTimeMillis())
            } else {
                session
            }
        }.sortedByDescending { it.updatedAt }

        storage.saveSessions(updatedSessionsWithUser)
        storage.saveActiveSessionId(currentSessionId)

        _uiState.update {
            it.copy(
                prompt = "",
                pendingAttachments = emptyList(),
                sessions = updatedSessionsWithUser,
                messages = updatedHistoryWithUser,
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }

        val context = getApplication<Application>().applicationContext

        viewModelScope.launch {
            repository.generateChatResponse(
                context = context,
                history = currentHistory,
                prompt = effectivePrompt,
                attachments = pendingAttachments,
                modelName = currentModel,
            ).fold(
                onSuccess = { modelResponseText ->
                    val modelMessage = ChatMessage(text = modelResponseText, isUser = false)
                    val fullHistory = updatedHistoryWithUser + modelMessage

                    val finalSessions = _uiState.value.sessions.map { session ->
                        if (session.id == currentSessionId) {
                            session.copy(title = title, messages = fullHistory, updatedAt = System.currentTimeMillis())
                        } else {
                            session
                        }
                    }.sortedByDescending { it.updatedAt }

                    storage.saveSessions(finalSessions)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            sessions = finalSessions,
                            messages = fullHistory,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Something went wrong",
                        )
                    }
                },
            )
        }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            application: Application,
            repository: GeminiRepository,
            storage: ChatStorage,
            hasApiKey: Boolean,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(application, repository, storage, hasApiKey) as T
        }
    }
}
