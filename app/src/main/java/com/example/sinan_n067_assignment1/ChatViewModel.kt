package com.example.sinan_n067_assignment1

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 👉 PASTE YOUR GEMINI API KEY HERE DIRECTLY OR IN local.properties:
private const val GEMINI_API_KEY = ""

class ChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val effectiveApiKey: String
        get() = BuildConfig.GEMINI_API_KEY.ifEmpty { GEMINI_API_KEY }

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-3.6-flash",
            apiKey = effectiveApiKey,
            systemInstruction = content {
                text("You are a helpful, versatile, and clear general AI assistant.")
            }
        )
    }

    private val chatSession by lazy {
        generativeModel.startChat()
    }

    init {
        val initialGreeting = ChatMessage(
            text = "Hello! I am your AI assistant. How can I help you today?",
            participant = Participant.MODEL
        )
        _uiState.update { it.copy(messages = listOf(initialGreeting)) }
    }

    fun sendMessage(userPrompt: String) {
        val trimmed = userPrompt.trim()
        if (trimmed.isEmpty() || _uiState.value.isLoading) return

        val userMessage = ChatMessage(
            text = trimmed,
            participant = Participant.USER
        )

        _uiState.update { currentState ->
            currentState.copy(
                messages = currentState.messages + userMessage,
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (effectiveApiKey.isBlank() || effectiveApiKey.startsWith("AIzaSyYourActual")) {
                    throw IllegalStateException("Please replace the placeholder with your actual Gemini API key.")
                }

                val response = chatSession.sendMessage(trimmed)
                val responseText = response.text ?: "No response received."

                val modelMessage = ChatMessage(
                    text = responseText,
                    participant = Participant.MODEL
                )

                _uiState.update { currentState ->
                    currentState.copy(
                        messages = currentState.messages + modelMessage,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Network/API error occurred. Please try again."
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}