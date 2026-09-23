package com.example.n089arieasampatassignment1.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.n089arieasampatassignment1.BuildConfig
import com.example.n089arieasampatassignment1.data.ChatDatabase
import com.example.n089arieasampatassignment1.data.GeminiRepository
import com.example.n089arieasampatassignment1.data.LocalChatRepository
import com.example.n089arieasampatassignment1.model.ChatMessage
import com.example.n089arieasampatassignment1.model.ChatUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val database =
        ChatDatabase.getDatabase(application)

    private val localRepository =
        LocalChatRepository(
            database.chatDao()
        )

    private val geminiRepository =
        GeminiRepository(
            BuildConfig.GEMINI_API_KEY
        )

    private val _uiState =
        MutableStateFlow(
            ChatUiState()
        )

    val uiState: StateFlow<ChatUiState> =
        _uiState.asStateFlow()

    init {

        observeMessages()
    }

    private fun observeMessages() {

        viewModelScope.launch {

            localRepository
                .getMessages()
                .collectLatest { messages ->

                    _uiState.value =
                        _uiState.value.copy(
                            messages = messages
                        )
                }
        }
    }

    fun updateInput(
        text: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                inputText = text
            )
    }

    fun sendMessage() {

        val text =
            _uiState.value.inputText.trim()

        if (text.isEmpty()) {
            return
        }

        if (_uiState.value.isLoading) {
            return
        }

        val userMessage =
            ChatMessage(
                text = text,
                isUser = true
            )

        _uiState.value =
            _uiState.value.copy(
                inputText = "",
                isLoading = true,
                errorMessage = null
            )

        viewModelScope.launch {

            try {

                localRepository.saveMessage(
                    userMessage
                )

                val response =
                    geminiRepository
                        .generateResponse(text)

                val geminiMessage =
                    ChatMessage(
                        text = response,
                        isUser = false
                    )

                localRepository.saveMessage(
                    geminiMessage
                )

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false
                    )

            } catch (exception: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        errorMessage =
                            exception.message
                                ?: "Unable to connect to Gemini."
                    )
            }
        }
    }

    fun clearHistory() {

        viewModelScope.launch {

            localRepository.clearMessages()
        }
    }

    fun clearError() {

        _uiState.value =
            _uiState.value.copy(
                errorMessage = null
            )
    }
}
