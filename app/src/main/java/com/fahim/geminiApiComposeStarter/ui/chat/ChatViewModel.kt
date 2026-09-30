package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ChatViewModel(
    private val repository: ChatRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _currentConversationId = MutableStateFlow(UUID.randomUUID().toString())
    private var lastSentPrompt: String = ""

    init {
        // 1. Restore most recent conversation if one exists in Room
        viewModelScope.launch {
            val mostRecentId = repository.getMostRecentConversationId()
            if (mostRecentId != null) {
                _currentConversationId.value = mostRecentId
            }
        }

        // 2. Observe messages for the currently active conversation
        viewModelScope.launch {
            _currentConversationId.collectLatest { convId ->
                _uiState.update { it.copy(conversationId = convId) }
                repository.observeMessages(convId).collect { messages ->
                    _uiState.update { it.copy(messages = messages) }
                }
            }
        }

        // 3. Observe all recent conversations for the navigation drawer
        viewModelScope.launch {
            repository.observeRecentConversations().collect { recentList ->
                _uiState.update { it.copy(recentConversations = recentList) }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onSend(customPrompt: String? = null) {
        val promptText = (customPrompt ?: _uiState.value.prompt).trim()
        if (promptText.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return

        lastSentPrompt = promptText
        val activeConvId = _currentConversationId.value
        val previousHistory = _uiState.value.messages.let { msgs ->
            if (msgs.isNotEmpty() && msgs.last().participant == Participant.USER && msgs.last().text == promptText) {
                msgs.dropLast(1)
            } else {
                msgs
            }
        }
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = activeConvId,
            text = promptText,
            participant = Participant.USER,
            timestamp = currentTimeString(),
        )

        // Clear prompt input & set loading state
        _uiState.update {
            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }

        // Save user message to Room immediately (Room Flow emits and updates UI)
        viewModelScope.launch {
            try {
                repository.saveMessage(userMessage, activeConvId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to save message",
                    )
                }
                return@launch
            }

            // Call Gemini API text generation with conversational history
            repository.generateText(promptText, previousHistory).fold(
                onSuccess = { responseText ->
                    val geminiMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        conversationId = activeConvId,
                        text = responseText,
                        participant = Participant.GEMINI,
                        timestamp = currentTimeString(),
                    )
                    try {
                        repository.saveMessage(geminiMessage, activeConvId)
                    } catch (_: Exception) {
                        // Room write failure logged/handled safely
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            response = responseText,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Couldn't generate a response",
                        )
                    }
                },
            )
        }
    }

    fun onRetry() {
        val promptToRetry = lastSentPrompt.ifBlank {
            _uiState.value.messages.lastOrNull { it.participant == Participant.USER }?.text.orEmpty()
        }
        if (promptToRetry.isNotBlank()) {
            _uiState.update { it.copy(errorMessage = null) }
            onSend(promptToRetry)
        }
    }

    fun onRegenerate() {
        val lastUserMessage = _uiState.value.messages.lastOrNull { it.participant == Participant.USER }
        if (lastUserMessage != null && !_uiState.value.isLoading) {
            onSend(lastUserMessage.text)
        }
    }

    fun onSuggestionClick(suggestionText: String) {
        onSend(suggestionText)
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onNewChat() {
        val newId = UUID.randomUUID().toString()
        _currentConversationId.value = newId
        _uiState.update {
            it.copy(
                conversationId = newId,
                prompt = "",
                response = "",
                isLoading = false,
                promptError = null,
                errorMessage = null,
            )
        }
    }

    fun onSelectConversation(conversationId: String) {
        if (_currentConversationId.value != conversationId) {
            _currentConversationId.value = conversationId
            _uiState.update {
                it.copy(
                    conversationId = conversationId,
                    prompt = "",
                    response = "",
                    isLoading = false,
                    promptError = null,
                    errorMessage = null,
                )
            }
        }
    }

    fun onNavigateToSettings() {
        _uiState.update { it.copy(currentScreen = AppScreen.SETTINGS) }
    }

    fun onNavigateToChat() {
        _uiState.update { it.copy(currentScreen = AppScreen.CHAT) }
    }

    fun onThemeToggle(isDark: Boolean) {
        _uiState.update {
            it.copy(settings = it.settings.copy(isDarkTheme = isDark))
        }
    }

    fun onSaveHistoryToggle(save: Boolean) {
        _uiState.update {
            it.copy(settings = it.settings.copy(saveHistory = save))
        }
    }

    fun onClearHistory() {
        viewModelScope.launch {
            try {
                repository.clearAllMessages()
            } catch (_: Exception) {
            }
            val newId = UUID.randomUUID().toString()
            _currentConversationId.value = newId
            _uiState.update {
                it.copy(
                    conversationId = newId,
                    messages = emptyList(),
                    response = "",
                    prompt = "",
                    errorMessage = null,
                    promptError = null,
                )
            }
        }
    }

    private fun currentTimeString(): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(repository: ChatRepository, hasApiKey: Boolean) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChatViewModel(repository, hasApiKey) as T
            }

        fun factory(repository: GeminiRepository, hasApiKey: Boolean) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val chatRepo = (repository as? ChatRepository) ?: object : ChatRepository {
                        override suspend fun generateText(prompt: String) = repository.generateText(prompt)
                        override suspend fun generateText(prompt: String, history: List<ChatMessage>) =
                            repository.generateText(prompt, history)
                        override fun observeMessages(conversationId: String) =
                            kotlinx.coroutines.flow.emptyFlow<List<ChatMessage>>()
                        override fun observeRecentConversations() =
                            kotlinx.coroutines.flow.emptyFlow<List<com.fahim.geminiApiComposeStarter.data.local.RecentConversation>>()
                        override suspend fun getMostRecentConversationId(): String? = null
                        override suspend fun saveMessage(message: ChatMessage, conversationId: String) {}
                        override suspend fun deleteConversation(conversationId: String) {}
                        override suspend fun clearAllMessages() {}
                    }
                    return ChatViewModel(chatRepo, hasApiKey) as T
                }
            }
    }
}
