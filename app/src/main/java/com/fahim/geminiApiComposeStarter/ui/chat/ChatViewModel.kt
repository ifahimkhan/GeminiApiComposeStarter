package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.MessageSender
import com.fahim.geminiApiComposeStarter.data.preferences.AiTone
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val geminiRepository: GeminiRepository,
    private val chatHistoryRepository: ChatHistoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(hasApiKey = hasApiKey))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Collect persisted chat history from Room
        chatHistoryRepository.messages
            .onEach { persistedMessages ->
                _uiState.update { it.copy(messages = persistedMessages) }
            }
            .launchIn(viewModelScope)

        // Collect persisted user tone preference from DataStore
        userPreferencesRepository.selectedToneFlow
            .onEach { tone ->
                _uiState.update { it.copy(selectedTone = tone) }
            }
            .launchIn(viewModelScope)
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onVoiceInput(transcript: String) {
        if (transcript.isBlank()) return
        _uiState.update { current ->
            val newPrompt = if (current.prompt.isBlank()) {
                transcript
            } else {
                "${current.prompt.trim()} $transcript"
            }
            current.copy(prompt = newPrompt, promptError = null)
        }
    }

    fun onSelectTone(tone: AiTone) {
        viewModelScope.launch {
            userPreferencesRepository.setAiTone(tone)
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onClearHistory() {
        viewModelScope.launch {
            chatHistoryRepository.clearHistory()
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

        val userMessage = ChatMessage(
            sender = MessageSender.USER,
            text = promptText,
        )

        val currentToneInstruction = _uiState.value.selectedTone.promptInstruction

        _uiState.update {
            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }

        viewModelScope.launch {
            // Persist user prompt immediately to Room
            chatHistoryRepository.saveMessage(userMessage)

            geminiRepository.generateText(
                prompt = promptText,
                systemInstruction = currentToneInstruction,
            ).fold(
                onSuccess = { responseText ->
                    val geminiMessage = ChatMessage(
                        sender = MessageSender.GEMINI,
                        text = responseText,
                    )
                    chatHistoryRepository.saveMessage(geminiMessage)
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to generate response from Gemini",
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
            geminiRepository: GeminiRepository,
            chatHistoryRepository: ChatHistoryRepository,
            userPreferencesRepository: UserPreferencesRepository,
            hasApiKey: Boolean,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(
                    geminiRepository = geminiRepository,
                    chatHistoryRepository = chatHistoryRepository,
                    userPreferencesRepository = userPreferencesRepository,
                    hasApiKey = hasApiKey,
                ) as T
        }
    }
}
