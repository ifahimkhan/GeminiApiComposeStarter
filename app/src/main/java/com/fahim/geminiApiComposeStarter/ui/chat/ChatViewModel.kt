package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatViewModel(
    private val repository: GeminiRepository,
    private val hasApiKey: Boolean,
    private val storage: ChatStorage? = null,
    private val preferences: AppPreferences? = null,
    private val vault: ApiKeyVault? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState(activeId = UUID.randomUUID().toString(), isRestoring = storage != null))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    private var nextMessageId = 0L
    private var request: Job? = null
    private var historyReadable = true
    private val saves = Channel<List<Conversation>>(Channel.CONFLATED)

    init {
        viewModelScope.launch {
            for (snapshot in saves) {
                try { storage?.save(snapshot) }
                catch (error: CancellationException) { throw error }
                catch (_: Exception) { _uiState.update { it.copy(errorMessage = "Could not save chat history on this device.") } }
            }
        }
        viewModelScope.launch {
            if (storage == null && preferences == null && vault == null) return@launch
            try {
                val settings = preferences?.values?.first() ?: Settings()
                val chats = storage?.load().orEmpty()
                nextMessageId = (chats.flatMap { it.messages }.maxOfOrNull { it.id } ?: -1L) + 1
                val savedKey = if (vault == null) false else withContext(Dispatchers.IO) { vault.read().isNotBlank() }
                // Always open on a fresh empty chat — history stays visible in the sidebar.
                _uiState.update { it.copy(
                    conversations = chats,
                    activeId = UUID.randomUUID().toString(),
                    messages = emptyList(),
                    prompt = "",
                    openKeyboard = settings.openKeyboard,
                    modelName = settings.modelName,
                    hasSavedKey = savedKey,
                    isRestoring = false,
                ) }
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) {
                historyReadable = false
                _uiState.update { it.copy(isRestoring = false, errorMessage = "Could not restore local data. Existing files have been preserved; restart the app before making changes.") }
            }
        }
    }

    private fun persist() {
        if (!historyReadable || storage == null) return
        val state = _uiState.value
        val conversation = Conversation(state.activeId,
            state.messages.firstOrNull { it.role == ChatRole.USER }?.text?.take(80) ?: "New chat",
            state.messages, state.prompt)
        val chats = if (state.messages.isEmpty() && state.prompt.isBlank()) state.conversations
            else listOf(conversation) + state.conversations.filterNot { it.id == state.activeId }
        _uiState.update { it.copy(conversations = chats) }
        saves.trySend(chats)
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null, errorMessage = null) }
        if (!_uiState.value.isRestoring) persist()
    }

    fun newChat() {
        if (_uiState.value.isRestoring || !historyReadable) return
        request?.cancel()
        persist()
        val id = UUID.randomUUID().toString()
        _uiState.update { it.copy(activeId = id, messages = emptyList(), prompt = "", isLoading = false, errorMessage = null, promptError = null) }
        selectPreference(id)
    }

    fun selectChat(id: String) {
        if (_uiState.value.isRestoring || !historyReadable) return
        val chat = _uiState.value.conversations.find { it.id == id } ?: return
        request?.cancel()
        persist()
        _uiState.update { it.copy(activeId = id, messages = chat.messages, prompt = chat.draft, isLoading = false, errorMessage = null, promptError = null) }
        selectPreference(id)
    }

    fun deleteChat(id: String) {
        if (!historyReadable || _uiState.value.isRestoring) return
        if (id == _uiState.value.activeId) {
            request?.cancel()
            _uiState.update { it.copy(activeId = UUID.randomUUID().toString(), messages = emptyList(), prompt = "", isLoading = false) }
        }
        _uiState.update { it.copy(conversations = it.conversations.filterNot { chat -> chat.id == id }) }
        saves.trySend(_uiState.value.conversations)
    }

    private fun selectPreference(id: String) {
        viewModelScope.launch {
            try { preferences?.select(id) }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { showError("Could not save the selected conversation.") }
        }
    }

    fun showError(message: String) { _uiState.update { it.copy(errorMessage = message) } }

    fun saveSettings(key: String?, openKeyboard: Boolean, modelName: String) {
        if (modelName.isBlank()) { showError("Enter a Gemini model name."); return }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { if (key != null) vault?.save(key) }
                preferences?.save(openKeyboard, modelName)
                val savedKey = withContext(Dispatchers.IO) { vault?.read()?.isNotBlank() ?: false }
                _uiState.update { it.copy(openKeyboard = openKeyboard, modelName = modelName.trim(), hasSavedKey = savedKey, settingsMessage = "Settings saved on this device.") }
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { showError("Could not save settings securely. Please try again.") }
        }
    }

    fun onSend() {
        val state = _uiState.value
        if (state.isLoading || state.isRestoring || !historyReadable) return
        val prompt = state.prompt.trim()
        if (prompt.isEmpty()) { _uiState.update { it.copy(promptError = PromptError.EMPTY) }; return }
        if (!hasApiKey) { showError(MISSING_API_KEY_MESSAGE); return }
        val message = ChatMessage(nextMessageId++, ChatRole.USER, prompt)
        _uiState.update { it.copy(prompt = "", messages = it.messages + message, isLoading = true, errorMessage = null, promptError = null) }
        persist()
        selectPreference(state.activeId)
        val context = _uiState.value.messages
        request = viewModelScope.launch {
            try {
                repository.generateConversation(context).fold(
                    onSuccess = { answer ->
                        _uiState.update { it.copy(isLoading = false, messages = it.messages + ChatMessage(nextMessageId++, ChatRole.GEMINI, answer)) }
                    },
                    onFailure = { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Something went wrong") } },
                )
                persist()
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { _uiState.update { it.copy(isLoading = false, errorMessage = "The request failed. Please try again.") }; persist() }
        }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE = "Add your Gemini API key in Settings before sending."
        fun factory(repository: GeminiRepository, hasApiKey: Boolean, storage: ChatStorage? = null,
                    preferences: AppPreferences? = null, vault: ApiKeyVault? = null) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChatViewModel(repository, hasApiKey, storage, preferences, vault) as T
            }
    }
}
