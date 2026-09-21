package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.ChatStore
import com.fahim.geminiApiComposeStarter.data.Conversation
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatAppState(
    val conversations: List<Conversation> = emptyList(),
    val currentId: Long? = null,
    val input: String = "",
    val isLoading: Boolean = false,
    /** null = follow the system theme. */
    val darkMode: Boolean? = null,
    val error: String? = null,
) {
    val current: Conversation? get() = conversations.firstOrNull { it.id == currentId }
}

class ChatAppViewModel(
    private val store: ChatStore,
    private val repository: GeminiRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatAppState(darkMode = store.darkMode))
    val state: StateFlow<ChatAppState> = _state.asStateFlow()

    init {
        // Restore saved chats and reopen the most recent one.
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { store.load() }.sortedByDescending { it.updatedAt }
            _state.update { it.copy(conversations = saved, currentId = saved.firstOrNull()?.id) }
        }
    }

    fun onInputChange(value: String) = _state.update { it.copy(input = value) }

    fun appendVoiceText(text: String) = _state.update {
        it.copy(input = if (it.input.isBlank()) text else it.input.trimEnd() + " " + text)
    }

    fun onSend() {
        val s = _state.value
        val prompt = s.input.trim()
        if (prompt.isEmpty() || s.isLoading) return
        if (!hasApiKey) {
            _state.update { it.copy(error = MISSING_API_KEY_MESSAGE) }
            return
        }

        val now = System.currentTimeMillis()
        val existing = s.current
        val history = existing?.messages.orEmpty()
        val conversationId = existing?.id ?: now
        val userMessage = ChatMessage(nextId(history), prompt, isUser = true)
        val updated = Conversation(
            id = conversationId,
            title = existing?.title ?: prompt.take(30),
            updatedAt = now,
            messages = history + userMessage,
        )

        _state.update {
            it.copy(
                conversations = it.conversations.upsert(updated),
                currentId = conversationId,
                input = "",
                isLoading = true,
                error = null,
            )
        }
        persist()

        viewModelScope.launch {
            repository.chat(history, prompt).fold(
                onSuccess = { reply -> appendReply(conversationId, reply) },
                onFailure = { e ->
                    _state.update {
                        it.copy(isLoading = false, error = e.message ?: "Something went wrong")
                    }
                },
            )
        }
    }

    private fun appendReply(conversationId: Long, reply: String) {
        _state.update { s ->
            val conv = s.conversations.firstOrNull { it.id == conversationId }
            if (conv == null) {
                s.copy(isLoading = false)
            } else {
                val botMessage = ChatMessage(nextId(conv.messages), reply, isUser = false)
                val updated = conv.copy(
                    messages = conv.messages + botMessage,
                    updatedAt = System.currentTimeMillis(),
                )
                s.copy(conversations = s.conversations.upsert(updated), isLoading = false)
            }
        }
        persist()
    }

    fun startNewChat() = _state.update { it.copy(currentId = null, input = "", error = null) }

    fun selectConversation(id: Long) = _state.update { it.copy(currentId = id, error = null) }

    fun deleteConversation(id: Long) {
        _state.update {
            it.copy(
                conversations = it.conversations.filterNot { c -> c.id == id },
                currentId = if (it.currentId == id) null else it.currentId,
            )
        }
        persist()
    }

    fun setDarkMode(enabled: Boolean) {
        store.darkMode = enabled
        _state.update { it.copy(darkMode = enabled) }
    }

    fun showError(message: String) = _state.update { it.copy(error = message) }

    fun clearError() = _state.update { it.copy(error = null) }

    private fun persist() {
        val snapshot = _state.value.conversations
        viewModelScope.launch(Dispatchers.IO) { store.save(snapshot) }
    }

    private fun nextId(messages: List<ChatMessage>) = (messages.lastOrNull()?.id ?: 0L) + 1

    private fun List<Conversation>.upsert(c: Conversation) =
        (filterNot { it.id == c.id } + c).sortedByDescending { it.updatedAt }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(store: ChatStore, repository: GeminiRepository, hasApiKey: Boolean) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChatAppViewModel(store, repository, hasApiKey) as T
            }
    }
}
