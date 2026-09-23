package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.Participant
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.toDomain
import com.fahim.geminiApiComposeStarter.data.local.toEntity
import com.fahim.geminiApiComposeStarter.data.preferences.ThemePreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing chat interaction state, Room persistence, and theme preference.
 *
 * Requirements satisfied:
 * - Automatically loads existing conversation history on initialization.
 * - Room database serves as the persistent source of truth.
 * - Persists user messages and successful model responses to Room.
 * - Database operations execute off the main thread (ioDispatcher).
 * - Avoids duplicate messages through Room primary-key deduplication.
 * - Rejects blank prompts and preserves existing UI state behavior.
 * - Reactively observes and toggles theme preferences persisted via DataStore.
 */
class ChatViewModel(
    private val repository: GeminiRepository,
    private val hasApiKey: Boolean,
    private val chatMessageDao: ChatMessageDao? = null,
    private val themePreferencesRepository: ThemePreferencesRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        chatMessageDao?.let { dao ->
            viewModelScope.launch(ioDispatcher) {
                dao.getAllMessages().collect { entities ->
                    val domainMessages = entities.map { it.toDomain() }
                    _uiState.update { current ->
                        current.copy(messages = domainMessages)
                    }
                }
            }
        }

        themePreferencesRepository?.let { repo ->
            viewModelScope.launch(ioDispatcher) {
                repo.isDarkMode.collect { isDark ->
                    if (isDark != null) {
                        _uiState.update { current ->
                            current.copy(isDarkTheme = isDark)
                        }
                    }
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onVoiceInputResult(text: String) {
        if (text.isNotBlank()) {
            onPromptChange(text)
        }
    }

    fun onVoiceRecognitionError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
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

        val userMessage = ChatMessage(text = prompt, participant = Participant.USER)

        _uiState.update { current ->
            current.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null,
                messages = if (chatMessageDao == null) current.messages + userMessage else current.messages,
            )
        }

        viewModelScope.launch(ioDispatcher) {
            chatMessageDao?.insertMessage(userMessage.toEntity())

            repository.generateText(prompt).fold(
                onSuccess = { text ->
                    val modelMessage = ChatMessage(text = text, participant = Participant.MODEL)
                    chatMessageDao?.insertMessage(modelMessage.toEntity())
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            messages = if (chatMessageDao == null) current.messages + modelMessage else current.messages,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Something went wrong",
                        )
                    }
                },
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch(ioDispatcher) {
            chatMessageDao?.clearAll()
            if (chatMessageDao == null) {
                _uiState.update { it.copy(messages = emptyList()) }
            }
        }
    }

    fun toggleTheme() {
        val newTheme = !_uiState.value.isDarkTheme
        _uiState.update { it.copy(isDarkTheme = newTheme) }
        viewModelScope.launch(ioDispatcher) {
            themePreferencesRepository?.setDarkMode(newTheme)
        }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            hasApiKey: Boolean,
            chatMessageDao: ChatMessageDao? = null,
            themePreferencesRepository: ThemePreferencesRepository? = null,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(
                    repository = repository,
                    hasApiKey = hasApiKey,
                    chatMessageDao = chatMessageDao,
                    themePreferencesRepository = themePreferencesRepository,
                    ioDispatcher = ioDispatcher,
                ) as T
        }
    }
}
