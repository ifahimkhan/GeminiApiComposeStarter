package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatDao
import com.fahim.geminiApiComposeStarter.data.ChatEntity
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val chatDao: ChatDao,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Assignment Step 5: Load past conversations from Room Database
        viewModelScope.launch {
            chatDao.getAllMessages().collect { entities ->
                val history = entities.map {
                    ChatMessage(
                        id = it.id.toLong(), // Converted to Long to match ChatMessage
                        text = it.text,
                        participant = Participant.valueOf(it.participant),
                        timestamp = it.timestamp
                    )
                }
                _uiState.update { it.copy(messages = history) }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onSend() {
        val promptText = _uiState.value.prompt.trim()
        if (promptText.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = "GEMINI_API_KEY is missing. Add it to local.properties.") }
            return
        }
        if (_uiState.value.isLoading) return

        _uiState.update { it.copy(isLoading = true, prompt = "", errorMessage = null) }

        viewModelScope.launch {
            // 1. Insert user message into Room
            chatDao.insertMessage(ChatEntity(text = promptText, participant = "USER"))

            // 2. Query Gemini API
            repository.generateText(promptText).fold(
                onSuccess = { text ->
                    // 3. Insert model response into Room
                    chatDao.insertMessage(ChatEntity(text = text, participant = "MODEL"))
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to generate response"
                        )
                    }
                }
            )
        }
    }

    companion object {
        fun factory(repository: GeminiRepository, chatDao: ChatDao, hasApiKey: Boolean) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChatViewModel(repository, chatDao, hasApiKey) as T
            }
    }
}