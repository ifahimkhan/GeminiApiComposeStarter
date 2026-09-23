package com.example.n075nishantassgn1.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.n075nishantassgn1.data.ChatHistoryDao
import com.example.n075nishantassgn1.data.ChatHistoryEntity
import com.example.n075nishantassgn1.data.FirebaseGeminiRepository
import com.example.n075nishantassgn1.data.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository = FirebaseGeminiRepository(),
    private val historyDao: ChatHistoryDao? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        historyDao?.let { dao ->
            viewModelScope.launch {
                dao.getAllMessages().collect { history ->
                    _uiState.value = _uiState.value.copy(
                        messages = history.map {
                            ChatMessage(
                                id = it.id,
                                text = it.text,
                                isUser = it.isUser
                            )
                        }
                    )
                }
            }
        }
    }

    fun sendMessage(message: String) {
        if (message.isBlank() || _uiState.value.isLoading) return

        val userMessage = ChatMessage(
            id = System.currentTimeMillis(),
            text = message,
            isUser = true
        )

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage,
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            try {
                historyDao?.insert(
                    ChatHistoryEntity(
                        text = message,
                        isUser = true
                    )
                )

                val response = repository.sendMessage(message)

                val geminiMessage = ChatMessage(
                    id = System.currentTimeMillis(),
                    text = response,
                    isUser = false
                )

                historyDao?.insert(
                    ChatHistoryEntity(
                        text = response,
                        isUser = false
                    )
                )

                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + geminiMessage,
                    isLoading = false
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Something went wrong."
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearChat() {
        viewModelScope.launch {
            historyDao?.clearHistory()
            _uiState.value = _uiState.value.copy(
                messages = emptyList(),
                error = null
            )
        }
    }
}