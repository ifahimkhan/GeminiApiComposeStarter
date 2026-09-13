package com.example.c001manavassignment1.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001manavassignment1.data.IGeminiRepository
import com.example.c001manavassignment1.data.IUserPreferencesRepository
import com.example.c001manavassignment1.data.local.ConversationEntity
import com.example.c001manavassignment1.security.IApiKeyManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val apiKeyManager: IApiKeyManager,
    private val userPreferencesRepository: IUserPreferencesRepository,
    private val repositoryFactory: (String) -> IGeminiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var repository: IGeminiRepository? = null
    private var messagesJob: Job? = null

    init {
        initRepository()
        observePreferences()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            userPreferencesRepository.userPreferencesFlow.collect { preferences ->
                _uiState.update { it.copy(isDarkMode = preferences.isDarkMode) }
            }
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            userPreferencesRepository.updateDarkMode(!_uiState.value.isDarkMode)
        }
    }

    private fun initRepository() {
        viewModelScope.launch {
            try {
                apiKeyManager.initializeApiKey()
                val apiKey = apiKeyManager.getApiKey()
                if (apiKey.isNotEmpty()) {
                    val repo = repositoryFactory(apiKey)
                    repository = repo
                    observeConversations(repo)
                } else {
                    _uiState.update { it.copy(error = "API Key not configured") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to initialize: ${e.localizedMessage}") }
            }
        }
    }

    private fun observeConversations(repo: IGeminiRepository) {
        viewModelScope.launch {
            repo.allConversations.collect { conversations ->
                _uiState.update { it.copy(conversations = conversations) }
            }
        }
    }

    fun selectConversation(conversationId: Long) {
        _uiState.update { it.copy(activeConversationId = conversationId, messages = emptyList()) }
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repository?.getMessagesForConversation(conversationId)?.collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    fun startNewChat() {
        _uiState.update { it.copy(activeConversationId = null, messages = emptyList(), inputText = "") }
        messagesJob?.cancel()
    }

    fun deleteConversation(conversation: ConversationEntity) {
        viewModelScope.launch {
            repository?.deleteConversation(conversation)
            if (_uiState.value.activeConversationId == conversation.id) {
                startNewChat()
            }
        }
    }

    fun onInputTextChange(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun sendMessage() {
        val prompt = _uiState.value.inputText.trim()
        if (prompt.isEmpty() || _uiState.value.isLoading) return

        val repo = repository ?: run {
            _uiState.update { it.copy(error = "Gemini not ready") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, inputText = "") }
            try {
                var conversationId = _uiState.value.activeConversationId
                if (conversationId == null) {
                    conversationId = repo.createConversation(prompt.take(30))
                    _uiState.update { it.copy(activeConversationId = conversationId) }
                    selectConversation(conversationId)
                }
                
                repo.sendMessage(prompt, conversationId)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Gemini Error: ${e.localizedMessage}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun onError(message: String) {
        _uiState.update { it.copy(error = message) }
    }
}
