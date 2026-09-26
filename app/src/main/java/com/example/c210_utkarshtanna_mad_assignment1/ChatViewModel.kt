package com.example.c210_utkarshtanna_mad_assignment1

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val chatDao = db.chatDao()
    private val prefs = application.getSharedPreferences("gemini_prefs", Context.MODE_PRIVATE)

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Restore last query
        val lastQuery = prefs.getString("last_query", "") ?: ""
        if (lastQuery.isNotEmpty()) {
            _inputText.value = lastQuery
        }

        // Load chat history from Room DB
        viewModelScope.launch {
            try {
                val history = chatDao.getAllChats()
                _messages.value = history.map { entity ->
                    ChatMessage(
                        id = entity.id.toLong(),
                        text = entity.message,
                        isUser = entity.isUser
                    )
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error loading chat history", e)
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _inputText.value = text
        if (_errorMessage.value != null) {
            _errorMessage.value = null
        }
    }

    fun setVoicePrompt(prompt: String) {
        _inputText.value = prompt
        _errorMessage.value = null
    }

    fun sendMessage() {
        val prompt = _inputText.value.trim()
        if (prompt.isEmpty()) {
            _errorMessage.value = "This field cannot be empty"
            return
        }

        // Save last query in SharedPreferences
        prefs.edit().putString("last_query", prompt).apply()

        // Clear error and input
        _errorMessage.value = null
        _inputText.value = ""

        // Add user message to state
        val userMsg = ChatMessage(text = prompt, isUser = true)
        _messages.value += userMsg

        _isLoading.value = true

        viewModelScope.launch {
            // Persist user message to DB
            try {
                chatDao.insert(ChatEntity(message = prompt, isUser = true))
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to save user message", e)
            }

            // Generate content with Gemini
            try {
                val response = generativeModel.generateContent(prompt)
                val responseText = response.text ?: "No response from Gemini"
                Log.d("Response", responseText)

                val aiMsg = ChatMessage(text = responseText, isUser = false)
                _messages.value += aiMsg

                // Persist AI message to DB
                chatDao.insert(ChatEntity(message = responseText, isUser = false))
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Gemini API error", e)
                val errorMsg = ChatMessage(
                    text = "Error generating response: ${e.localizedMessage ?: "Unknown error"}",
                    isUser = false
                )
                _messages.value += errorMsg
            } finally {
                _isLoading.value = false
            }
        }
    }
}
