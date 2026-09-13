package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.BuildConfig
import com.fahim.geminiApiComposeStarter.GeminiChatApplication
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.SecureKeyStore
import com.fahim.geminiApiComposeStarter.data.db.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.db.ConversationEntity
import com.fahim.geminiApiComposeStarter.data.prefs.ThemeMode
import com.fahim.geminiApiComposeStarter.data.prefs.UserPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private fun ChatMessageEntity.toUi() = ChatMessage(
    id = id,
    content = content,
    isFromUser = isFromUser,
    timestamp = timestamp,
)

/** Transient, in-memory-only slice of state that does not need to survive process death. */
private data class TransientState(
    val prompt: String = "",
    val promptError: PromptError? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showSettingsDialog: Boolean = false,
    val currentConversationId: Long? = null,
    val maskedApiKey: String = "",
    val isUsingCustomKey: Boolean = false,
    val hasApiKey: Boolean = false,
)

private data class PersistedPrefs(
    val username: String?,
    val themeMode: ThemeMode,
    val dynamicColorEnabled: Boolean,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val database = (application as GeminiChatApplication).database
    private val conversationDao = database.conversationDao()
    private val chatDao = database.chatDao()
    private val preferences = UserPreferences(application)
    private val secureKeyStore = SecureKeyStore(application)
    private val defaultApiKey = BuildConfig.GEMINI_API_KEY

    private var repository: GeminiRepository = GeminiRepositoryImpl(apiKey = defaultApiKey)

    private val transient = MutableStateFlow(TransientState())

    init {
        viewModelScope.launch {
            secureKeyStore.persistDefaultKeyIfEmpty(defaultApiKey)
            refreshRepository()
        }
    }

    private val persistedPrefsFlow = combine(
        preferences.username,
        preferences.themeMode,
        preferences.dynamicColorEnabled,
    ) { username, theme, dynamicColor -> PersistedPrefs(username, theme, dynamicColor) }

    private val conversationsFlow = conversationDao.observeAll().map { entities ->
        entities.map { ConversationSummary(it.id, it.title, it.updatedAt) }
    }

    private val messagesFlow = transient
        .map { it.currentConversationId }
        .distinctUntilChanged()
        .flatMapLatest { conversationId ->
            if (conversationId == null) {
                flowOf(emptyList())
            } else {
                chatDao.observeMessages(conversationId).map { list -> list.map { it.toUi() } }
            }
        }

    val uiState: StateFlow<ChatUiState> = combine(
        transient,
        persistedPrefsFlow,
        conversationsFlow,
        messagesFlow,
    ) { t, prefs, conversations, messages ->
        ChatUiState(
            isReady = true,
            username = prefs.username,
            conversations = conversations,
            currentConversationId = t.currentConversationId,
            messages = messages,
            prompt = t.prompt,
            promptError = t.promptError,
            isLoading = t.isLoading,
            errorMessage = t.errorMessage,
            themeMode = prefs.themeMode,
            dynamicColorEnabled = prefs.dynamicColorEnabled,
            showSettingsDialog = t.showSettingsDialog,
            maskedApiKey = t.maskedApiKey,
            isUsingCustomKey = t.isUsingCustomKey,
            hasApiKey = t.hasApiKey,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChatUiState(),
    )

    // ---- Onboarding ----

    fun setUsername(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { preferences.setUsername(trimmed) }
    }

    // ---- Prompt / send ----

    fun onPromptChange(value: String) {
        transient.update { it.copy(prompt = value, promptError = null) }
    }

    fun onErrorMessageShown() {
        transient.update { it.copy(errorMessage = null) }
    }

    fun onSend() {
        val current = transient.value
        val prompt = current.prompt.trim()
        if (prompt.isEmpty()) {
            transient.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!current.hasApiKey) {
            transient.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (current.isLoading) return

        transient.update { it.copy(isLoading = true, errorMessage = null, promptError = null, prompt = "") }

        viewModelScope.launch {
            val conversationId = ensureConversation(prompt)
            val now = System.currentTimeMillis()
            chatDao.insert(
                ChatMessageEntity(
                    conversationId = conversationId,
                    content = prompt,
                    isFromUser = true,
                    timestamp = now,
                )
            )
            conversationDao.touch(conversationId, now)

            repository.generateText(prompt).fold(
                onSuccess = { reply ->
                    val replyTime = System.currentTimeMillis()
                    chatDao.insert(
                        ChatMessageEntity(
                            conversationId = conversationId,
                            content = reply,
                            isFromUser = false,
                            timestamp = replyTime,
                        )
                    )
                    conversationDao.touch(conversationId, replyTime)
                    transient.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    transient.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "Something went wrong")
                    }
                },
            )
        }
    }

    private suspend fun ensureConversation(firstPrompt: String): Long {
        val existing = transient.value.currentConversationId
        if (existing != null) return existing
        val now = System.currentTimeMillis()
        val title = firstPrompt.take(40).let { if (firstPrompt.length > 40) "$it…" else it }
        val newId = conversationDao.insert(ConversationEntity(title = title, createdAt = now, updatedAt = now))
        transient.update { it.copy(currentConversationId = newId) }
        return newId
    }

    // ---- Sidebar / conversation history ----

    fun onNewConversation() {
        transient.update { it.copy(currentConversationId = null, prompt = "", promptError = null) }
    }

    fun onSelectConversation(id: Long) {
        transient.update { it.copy(currentConversationId = id, prompt = "", promptError = null) }
    }

    fun onDeleteConversation(id: Long) {
        viewModelScope.launch {
            conversationDao.delete(id)
            if (transient.value.currentConversationId == id) {
                transient.update { it.copy(currentConversationId = null) }
            }
        }
    }

    // ---- Voice input ----

    fun onVoiceResult(text: String) {
        if (text.isBlank()) return
        transient.update { it.copy(prompt = text, promptError = null) }
    }

    // ---- Settings dialog / API key ----

    fun openSettings() {
        viewModelScope.launch {
            val masked = secureKeyStore.getMaskedPreview(defaultApiKey)
            val isCustom = secureKeyStore.isCustomKey.first()
            transient.update { it.copy(showSettingsDialog = true, maskedApiKey = masked, isUsingCustomKey = isCustom) }
        }
    }

    fun closeSettings() {
        transient.update { it.copy(showSettingsDialog = false) }
    }

    fun saveApiKey(key: String) {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            secureKeyStore.saveCustomKey(trimmed)
            refreshRepository()
            transient.update {
                it.copy(
                    maskedApiKey = secureKeyStore.getMaskedPreview(defaultApiKey),
                    isUsingCustomKey = true,
                )
            }
        }
    }

    fun resetApiKey() {
        viewModelScope.launch {
            secureKeyStore.resetToDefaultKey(defaultApiKey)
            refreshRepository()
            transient.update {
                it.copy(
                    maskedApiKey = secureKeyStore.getMaskedPreview(defaultApiKey),
                    isUsingCustomKey = false,
                )
            }
        }
    }

    private suspend fun refreshRepository() {
        val key = secureKeyStore.getDecryptedApiKey(defaultApiKey)
        repository = GeminiRepositoryImpl(apiKey = key)
        transient.update { it.copy(hasApiKey = key.isNotBlank()) }
    }

    // ---- Theme ----

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setDynamicColorEnabled(enabled) }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties, or paste one in Settings, then rebuild."

        fun factory(application: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(application) as T
        }
    }
}
