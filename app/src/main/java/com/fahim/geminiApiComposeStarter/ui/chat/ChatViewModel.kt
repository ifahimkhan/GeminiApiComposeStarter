package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.Participant
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(
    private val repository: GeminiRepository,
    private val hasApiKey: Boolean,
    private val chatDao: ChatMessageDao? = null,
    private val preferencesRepository: UserPreferencesRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Collect past conversations from Room database
        chatDao?.let { dao ->
            viewModelScope.launch(ioDispatcher) {
                dao.getAllMessages().collect { messageEntities ->
                    _uiState.update { it.copy(messages = messageEntities) }
                }
            }
        }

        // Collect user preferences from Preferences DataStore
        preferencesRepository?.let { prefs ->
            viewModelScope.launch(ioDispatcher) {
                prefs.userPreferencesFlow.collect { userPrefs ->
                    _uiState.update {
                        it.copy(
                            userName = userPrefs.userName,
                            systemPromptTone = userPrefs.systemPromptTone
                        )
                    }
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onVoiceInputResult(spokenText: String) {
        if (spokenText.isNotBlank()) {
            _uiState.update {
                val current = it.prompt
                val updated = if (current.isBlank()) spokenText else "$current $spokenText"
                it.copy(prompt = updated, promptError = null)
            }
        }
    }

    fun onSend() {
        val promptText = _uiState.value.prompt.trim()
        if (promptText.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return

        val userMessage = ChatMessageEntity(
            text = promptText,
            participant = Participant.USER
        )

        // Clear input bar and show loading state
        _uiState.update {
            val updatedMessages = if (chatDao == null) it.messages + userMessage else it.messages
            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null,
                messages = updatedMessages
            )
        }

        viewModelScope.launch {
            withContext(ioDispatcher) {
                chatDao?.insertMessage(userMessage)
            }

            val fullPrompt = if (_uiState.value.systemPromptTone.isNotBlank()) {
                "[System: Please act as ${_uiState.value.systemPromptTone}]\n$promptText"
            } else {
                promptText
            }

            repository.generateText(fullPrompt).fold(
                onSuccess = { responseText ->
                    val geminiMessage = ChatMessageEntity(
                        text = responseText,
                        participant = Participant.GEMINI
                    )
                    withContext(ioDispatcher) {
                        chatDao?.insertMessage(geminiMessage)
                    }
                    _uiState.update {
                        val messages = if (chatDao == null) it.messages + geminiMessage else it.messages
                        it.copy(
                            isLoading = false,
                            messages = messages
                        )
                    }
                },
                onFailure = { error ->
                    val errorMsg = error.message ?: "Something went wrong"
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = errorMsg
                        )
                    }
                }
            )
        }
    }

    fun onClearHistory() {
        viewModelScope.launch(ioDispatcher) {
            chatDao?.clearAll()
            _uiState.update { it.copy(messages = emptyList()) }
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onUpdateUserName(name: String) {
        viewModelScope.launch(ioDispatcher) {
            preferencesRepository?.updateUserName(name)
            _uiState.update { it.copy(userName = name) }
        }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            hasApiKey: Boolean = true,
            chatDao: ChatMessageDao? = null,
            preferencesRepository: UserPreferencesRepository? = null,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(
                    repository = repository,
                    hasApiKey = hasApiKey,
                    chatDao = chatDao,
                    preferencesRepository = preferencesRepository,
                    ioDispatcher = ioDispatcher
                ) as T
        }
    }
}
