package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatViewModel(
    private val repository: GeminiRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    init {
        // Load persisted messages from Room on start
        viewModelScope.launch {
            repository.getMessages().collect { entities ->
                val messages = entities.map { entity ->
                    ChatMessage(
                        id = entity.id,
                        sender = entity.sender,
                        text = entity.text,
                        timestamp = entity.timestamp,
                        isLoading = entity.isPending,
                        isError = entity.isError
                    )
                }
                _uiState.update { state ->
                    val loadingBubbles = state.messages.filter { it.isLoading }
                    state.copy(messages = messages + loadingBubbles)
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
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

        val userTimestamp = timeFormatter.format(Date())

        // Persist + add user message
        viewModelScope.launch {
            val userEntity = ChatMessageEntity(
                sender = "DRIVER",
                text = prompt,
                timestamp = userTimestamp
            )
            val userId = repository.saveMessage(userEntity)

            val userMsg = ChatMessage(
                id = userId,
                sender = "DRIVER",
                text = prompt,
                timestamp = userTimestamp
            )

            // Add a loading placeholder for PITWALL response
            val loadingMsg = ChatMessage(
                sender = "PITWALL",
                text = "",
                timestamp = timeFormatter.format(Date()),
                isLoading = true
            )

            _uiState.update {
                it.copy(
                    messages = it.messages + userMsg + loadingMsg,
                    prompt = "",
                    isLoading = true,
                    errorMessage = null,
                    promptError = null
                )
            }

            // Fetch response from Gemini
            repository.generateText(prompt).fold(
                onSuccess = { responseText ->
                    val responseTimestamp = timeFormatter.format(Date())
                    val responseEntity = ChatMessageEntity(
                        sender = "PITWALL",
                        text = responseText,
                        timestamp = responseTimestamp
                    )
                    val responseId = repository.saveMessage(responseEntity)

                    val responseMsg = ChatMessage(
                        id = responseId,
                        sender = "PITWALL",
                        text = responseText,
                        timestamp = responseTimestamp
                    )

                    _uiState.update { state ->
                        // Remove the loading bubble and add real response
                        val withoutLoading = state.messages.filter { !it.isLoading }
                        state.copy(
                            messages = withoutLoading + responseMsg,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        val withoutLoading = state.messages.filter { !it.isLoading }
                        state.copy(
                            messages = withoutLoading,
                            isLoading = false,
                            errorMessage = "RADIO FAILURE · ${error.message ?: "Unknown error"}"
                        )
                    }
                }
            )
        }
    }

    fun onClearChat() {
        viewModelScope.launch {
            repository.clearMessages()
            _uiState.update {
                it.copy(messages = emptyList(), errorMessage = null, isLoading = false)
            }
        }
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onChipSelected(text: String) {
        _uiState.update { it.copy(prompt = text) }
        onSend()
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(repository: GeminiRepository, hasApiKey: Boolean) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChatViewModel(repository, hasApiKey) as T
            }
    }
}
