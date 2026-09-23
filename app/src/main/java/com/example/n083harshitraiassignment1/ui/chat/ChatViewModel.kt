package com.example.n083harshitraiassignment1.ui.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.n083harshitraiassignment1.BuildConfig
import com.example.n083harshitraiassignment1.data.ChatDatabase
import com.example.n083harshitraiassignment1.data.GeminiRepository
import com.example.n083harshitraiassignment1.data.LocalChatRepository
import com.example.n083harshitraiassignment1.data.SecureApiKeyStorage
import com.example.n083harshitraiassignment1.data.UserPreferencesRepository
import com.example.n083harshitraiassignment1.model.ChatMessage
import com.example.n083harshitraiassignment1.model.ChatUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatViewModel @JvmOverloads constructor(
    application: Application,
    private val localRepository: LocalChatRepository =
        LocalChatRepository(ChatDatabase.getDatabase(application).chatDao()),
    private val secureStorage: SecureApiKeyStorage =
        SecureApiKeyStorage(application),
    private val userPreferencesRepository: UserPreferencesRepository =
        UserPreferencesRepository(application),
    private val geminiRepository: GeminiRepository =
        GeminiRepository(
            secureStorage.getDecryptedApiKey(BuildConfig.GEMINI_API_KEY)
        )
) : AndroidViewModel(application) {

    private val _uiState =
        MutableStateFlow(
            ChatUiState()
        )

    val uiState: StateFlow<ChatUiState> =
        _uiState.asStateFlow()

    init {
        observeMessages()
        observePreferences()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            userPreferencesRepository.userPreferencesFlow.collectLatest { prefs ->
                _uiState.value = _uiState.value.copy(
                    userName = prefs.userName,
                    tone = prefs.preferredTone
                )
            }
        }
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

                Log.e("ChatViewModel", "Error in sendMessage", exception)

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