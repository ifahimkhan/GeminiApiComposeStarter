package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
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
    private val chatDao: ChatDao,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        observeMessages()
    }

    private fun observeMessages() {
        viewModelScope.launch {
            chatDao.getAllMessages().collect { list ->
                if (list.isEmpty()) {
                    seedInitialMessages()
                } else {
                    _uiState.update { it.copy(messages = list) }
                }
            }
        }
    }

    private suspend fun seedInitialMessages() {
        val msg1 = ChatMessageEntity(
            text = "Hey Gemini! Can you summarize the key advantages of Jetpack Compose with Material 3 for mobile UI development?",
            isFromUser = true,
            formattedTime = "10:24 AM",
        )
        val msg2 = ChatMessageEntity(
            text = "Jetpack Compose combined with Material 3 (Material You) delivers a modern, reactive toolkit engineered for modern Android architecture:\n\n" +
                    "• Declarative UI Architecture: Code natively mirrors mutable state transitions with zero repetitive imperative boilerplate.\n\n" +
                    "• Material You Dynamic Theming: Instant algorithmic extraction for dynamic color schemes, tonal elevation, and adaptive typography tokens.\n\n" +
                    "• Built-in Compiler Performance: Smart incremental recomposition with a flat layout hierarchy that avoids traditional deep view nesting.",
            isFromUser = false,
            formattedTime = "10:24 AM",
        )
        val msg3 = ChatMessageEntity(
            text = "Can you generate a quick code snippet demonstrating a dynamic floating input bar?",
            isFromUser = true,
            formattedTime = "10:25 AM"
        )
        chatDao.insertMessage(msg1)
        chatDao.insertMessage(msg2)
        chatDao.insertMessage(msg3)
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onSend(promptText: String = _uiState.value.prompt) {
        val prompt = promptText.trim()
        if (prompt.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return

        val currentTime = getCurrentFormattedTime()
        _uiState.update { it.copy(prompt = "", isLoading = true, errorMessage = null, promptError = null) }

        viewModelScope.launch {
            // Save user message
            val userMsg = ChatMessageEntity(
                text = prompt,
                isFromUser = true,
                formattedTime = currentTime
            )
            chatDao.insertMessage(userMsg)

            // Request Gemini response
            repository.generateText(prompt).fold(
                onSuccess = { responseText ->
                    val modelMsg = ChatMessageEntity(
                        text = responseText,
                        isFromUser = false,
                        formattedTime = getCurrentFormattedTime()
                    )
                    chatDao.insertMessage(modelMsg)
                    _uiState.update { it.copy(isLoading = false) }
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

    fun onClearChat() {
        viewModelScope.launch {
            chatDao.clearAllMessages()
        }
    }

    private fun getCurrentFormattedTime(): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(Date())
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(repository: GeminiRepository, chatDao: ChatDao, hasApiKey: Boolean) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChatViewModel(repository, chatDao, hasApiKey) as T
            }
    }
}
