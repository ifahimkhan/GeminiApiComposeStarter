package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.InMemoryChatHistoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val repository: GeminiRepository,
    private val hasApiKey: Boolean,
    private val chatHistoryRepository: ChatHistoryRepository = InMemoryChatHistoryRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            chatHistoryRepository.getMessages().collect { persistedMessages ->
                _uiState.update { state ->
                    if (state.isLoading) {
                        val pendingAssistant = state.messages.find { it.isPending }
                        val merged = if (pendingAssistant != null && persistedMessages.none { it.id == pendingAssistant.id }) {
                            persistedMessages + pendingAssistant
                        } else {
                            persistedMessages
                        }
                        state.copy(messages = merged)
                    } else {
                        state.copy(messages = persistedMessages)
                    }
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onVoiceInputResult(spokenText: String) {
        val text = spokenText.trim()
        if (text.isEmpty()) return
        _uiState.update { state ->
            val updatedPrompt = if (state.prompt.isBlank()) {
                text
            } else {
                "${state.prompt} $text"
            }
            state.copy(
                prompt = updatedPrompt,
                promptError = null,
            )
        }
    }

    fun onVoiceInputError(errorMessage: String) {
        _uiState.update { it.copy(errorMessage = errorMessage) }
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

        val userMessage = ChatMessage(
            text = prompt,
            participant = Participant.USER,
        )
        val assistantMessageId = UUID.randomUUID().toString()
        val pendingAssistantMessage = ChatMessage(
            id = assistantMessageId,
            text = "",
            participant = Participant.ASSISTANT,
            isPending = true,
        )

        _uiState.update { state ->
            state.copy(
                prompt = "",
                promptError = null,
                errorMessage = null,
                isLoading = true,
                messages = state.messages + userMessage + pendingAssistantMessage,
            )
        }

        viewModelScope.launch {
            chatHistoryRepository.saveMessage(userMessage)

            val responseBuilder = StringBuilder()
            try {
                repository.sendMessageStream(prompt).collect { chunk ->
                    responseBuilder.append(chunk)
                    val currentText = responseBuilder.toString()
                    _uiState.update { state ->
                        val updatedMessages = state.messages.map { msg ->
                            if (msg.id == assistantMessageId) {
                                msg.copy(text = currentText, isPending = true)
                            } else {
                                msg
                            }
                        }
                        state.copy(
                            messages = updatedMessages,
                            response = currentText,
                        )
                    }
                }

                val finalText = responseBuilder.toString()
                val completedAssistantMessage = ChatMessage(
                    id = assistantMessageId,
                    text = finalText,
                    participant = Participant.ASSISTANT,
                    isPending = false,
                )

                _uiState.update { state ->
                    val updatedMessages = state.messages.map { msg ->
                        if (msg.id == assistantMessageId) completedAssistantMessage else msg
                    }
                    state.copy(
                        messages = updatedMessages,
                        isLoading = false,
                    )
                }
                chatHistoryRepository.saveMessage(completedAssistantMessage)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Failed to generate response."
                val hasPartialResponse = responseBuilder.isNotEmpty()
                if (hasPartialResponse) {
                    val partialMessage = ChatMessage(
                        id = assistantMessageId,
                        text = responseBuilder.toString(),
                        participant = Participant.ASSISTANT,
                        isPending = false,
                    )
                    chatHistoryRepository.saveMessage(partialMessage)
                }
                _uiState.update { state ->
                    val updatedMessages = if (hasPartialResponse) {
                        state.messages.map { msg ->
                            if (msg.id == assistantMessageId) msg.copy(isPending = false) else msg
                        }
                    } else {
                        state.messages.filterNot { it.id == assistantMessageId }
                    }
                    state.copy(
                        messages = updatedMessages,
                        isLoading = false,
                        errorMessage = errorMsg,
                    )
                }
            }
        }
    }

    fun onClearChat() {
        viewModelScope.launch {
            chatHistoryRepository.clearHistory()
            repository.resetChatSession()
            _uiState.update {
                it.copy(
                    prompt = "",
                    promptError = null,
                    errorMessage = null,
                    response = "",
                    messages = emptyList(),
                )
            }
        }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            hasApiKey: Boolean,
            chatHistoryRepository: ChatHistoryRepository = InMemoryChatHistoryRepository(),
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, hasApiKey, chatHistoryRepository) as T
        }
    }
}
