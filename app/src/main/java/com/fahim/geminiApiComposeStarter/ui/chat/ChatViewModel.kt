package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.SettingsRepository
import com.fahim.geminiApiComposeStarter.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ChatViewModel(
    private val repository: GeminiRepository,
    private val chatHistory: ChatHistoryRepository,
    private val settings: SettingsRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var nextMessageId: Long = System.currentTimeMillis()
    private var didInitialSelection = false

    init {
        viewModelScope.launch {
            chatHistory.observeConversations().collect { conversations ->
                _uiState.update { it.copy(conversations = conversations) }
                if (!didInitialSelection) {
                    didInitialSelection = true
                    conversations.firstOrNull()?.let { selectConversation(it.id) }
                }
            }
        }
        viewModelScope.launch {
            settings.compactBubbles.collect { compact ->
                _uiState.update { it.copy(compactBubbles = compact) }
            }
        }
        viewModelScope.launch {
            settings.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onToggleCompactBubbles() {
        val next = !_uiState.value.compactBubbles
        viewModelScope.launch {
            runCatching { settings.setCompactBubbles(next) }
        }
    }

    fun onSetThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            runCatching { settings.setThemeMode(mode) }
        }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onNewChat() {
        _uiState.update {
            it.copy(
                prompt = "",
                messages = emptyList(),
                currentConversationId = null,
                promptError = null,
                errorMessage = null,
            )
        }
    }

    fun onSelectConversation(id: Long) {
        if (id == _uiState.value.currentConversationId) return
        viewModelScope.launch { selectConversation(id) }
    }

    fun onDeleteConversation(id: Long) {
        viewModelScope.launch {
            runCatching { chatHistory.deleteConversation(id) }
            if (_uiState.value.currentConversationId == id) {
                _uiState.update {
                    it.copy(currentConversationId = null, messages = emptyList())
                }
            }
        }
    }

    private suspend fun selectConversation(id: Long) {
        val saved = runCatching { chatHistory.loadMessages(id) }.getOrDefault(emptyList())
        saved.maxOfOrNull { it.id }?.let { maxId ->
            if (maxId >= nextMessageId) nextMessageId = maxId + 1
        }
        _uiState.update { it.copy(currentConversationId = id, messages = saved) }
    }

    fun onSend() {
        val prompt = _uiState.value.prompt.trim()
        if (prompt.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return

        val now = System.currentTimeMillis()
        val userMessage = ChatMessage(
            id = newId(now),
            text = prompt,
            author = ChatAuthor.USER,
            timestamp = now,
        )
        val conversation = _uiState.value.messages + userMessage

        _uiState.update {
            it.copy(
                prompt = "",
                messages = conversation,
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }

        viewModelScope.launch {
            val conversationId = ensureConversation(prompt) ?: run {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not save conversation. Please try again.",
                    )
                }
                return@launch
            }

            runCatching { chatHistory.save(conversationId, userMessage) }

            repository.generateReply(conversation).fold(
                onSuccess = { text ->
                    val replyNow = System.currentTimeMillis()
                    val geminiMessage = ChatMessage(
                        id = newId(replyNow),
                        text = text,
                        author = ChatAuthor.GEMINI,
                        timestamp = replyNow,
                    )
                    _uiState.update {
                        it.copy(isLoading = false, messages = it.messages + geminiMessage)
                    }
                    runCatching { chatHistory.save(conversationId, geminiMessage) }
                    runCatching { chatHistory.touchConversation(conversationId) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.toUserFriendlyMessage(),
                        )
                    }
                },
            )
        }
    }

    private suspend fun ensureConversation(firstPrompt: String): Long? {
        _uiState.value.currentConversationId?.let { return it }
        val title = firstPrompt
            .take(40)
            .let { if (firstPrompt.length > 40) "$it…" else it }
            .ifBlank { "New chat" }
        return runCatching { chatHistory.createConversation(title) }
            .getOrNull()
            ?.also { newId ->
                _uiState.update { it.copy(currentConversationId = newId) }
            }
    }

    private fun newId(preferred: Long): Long {
        nextMessageId = if (preferred > nextMessageId) preferred else nextMessageId + 1
        return nextMessageId
    }

    private fun Throwable.toUserFriendlyMessage(): String = when (this) {
        is UnknownHostException, is ConnectException ->
            "No internet connection. Please check your network and try again."
        is SocketTimeoutException ->
            "The request timed out. Please try again."
        is IOException ->
            "Network error. Please check your connection and try again."
        else -> message?.takeIf { it.isNotBlank() }
            ?: "Something went wrong. Please try again."
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            chatHistory: ChatHistoryRepository,
            settings: SettingsRepository,
            hasApiKey: Boolean,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, chatHistory, settings, hasApiKey) as T
        }
    }
}