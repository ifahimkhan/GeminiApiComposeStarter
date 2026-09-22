package com.fahim.geminiApiComposeStarter

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.preferences.PreferencesRepository
import com.fahim.geminiApiComposeStarter.security.SecurityManager
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.chat.PromptError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeGeminiRepository : GeminiRepository {
    var shouldSucceed: Boolean = true
    var responseText: String = "Fake response from Gemini"
    var errorMessage: String = "API Network Error"

    override suspend fun generateText(prompt: String): Result<String> {
        return if (shouldSucceed) {
            Result.success(responseText)
        } else {
            Result.failure(RuntimeException(errorMessage))
        }
    }
}

class FakeChatMessageDao : ChatMessageDao {
    private val messagesFlow = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    private var idCounter = 1L

    override fun getAllMessages(): Flow<List<ChatMessageEntity>> = messagesFlow

    override suspend fun insertMessage(message: ChatMessageEntity): Long {
        val current = messagesFlow.value.toMutableList()
        val newMessage = message.copy(id = idCounter++)
        current.add(newMessage)
        messagesFlow.value = current
        return newMessage.id
    }

    override suspend fun clearAllMessages() {
        messagesFlow.value = emptyList()
    }
}

class FakePreferencesRepository : PreferencesRepository {
    override val themePreference = MutableStateFlow("SYSTEM")
    override val isVoiceEnabled = MutableStateFlow(true)
    override val encryptedApiKey = MutableStateFlow<SecurityManager.EncryptedResult?>(null)

    override suspend fun setThemePreference(theme: String) {
        themePreference.value = theme
    }

    override suspend fun setVoiceEnabled(enabled: Boolean) {
        isVoiceEnabled.value = enabled
    }

    override suspend fun saveEncryptedApiKey(ciphertext: String, iv: String) {
        encryptedApiKey.value = SecurityManager.EncryptedResult(ciphertext, iv)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeGeminiRepository
    private lateinit var fakeDao: FakeChatMessageDao
    private lateinit var fakePreferences: FakePreferencesRepository
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeGeminiRepository()
        fakeDao = FakeChatMessageDao()
        fakePreferences = FakePreferencesRepository()
        viewModel = ChatViewModel(
            repository = fakeRepository,
            chatMessageDao = fakeDao,
            preferencesRepository = fakePreferences,
            hasApiKey = true
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is clean`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertEquals("", state.prompt)
        assertFalse(state.isLoading)
        assertNull(state.promptError)
        assertNull(state.errorMessage)
        assertTrue(state.hasApiKey)
    }

    @Test
    fun `onPromptChange updates prompt and clears prompt error`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onPromptChange("Hello Gemini")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Hello Gemini", state.prompt)
        assertNull(state.promptError)
    }

    @Test
    fun `onSend with empty prompt sets promptError`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onPromptChange("   ")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PromptError.EMPTY, state.promptError)
    }

    @Test
    fun `onSend with missing API key sets errorMessage`() = runTest(testDispatcher) {
        val noKeyViewModel = ChatViewModel(
            repository = fakeRepository,
            chatMessageDao = fakeDao,
            preferencesRepository = fakePreferences,
            hasApiKey = false
        )
        backgroundScope.launch { noKeyViewModel.uiState.collect() }
        advanceUntilIdle()

        noKeyViewModel.onPromptChange("Hello")
        noKeyViewModel.onSend()
        advanceUntilIdle()

        val state = noKeyViewModel.uiState.value
        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, state.errorMessage)
    }

    @Test
    fun `onSend successfully persists prompt and response in Room`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onPromptChange("What is Kotlin?")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals("What is Kotlin?", state.messages[0].text)
        assertEquals("USER", state.messages[0].role)
        assertEquals("Fake response from Gemini", state.messages[1].text)
        assertEquals("GEMINI", state.messages[1].role)
        assertFalse(state.isLoading)
        assertEquals("", state.prompt)
    }

    @Test
    fun `onSend handles Gemini failure gracefully`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        fakeRepository.shouldSucceed = false
        fakeRepository.errorMessage = "Quota exceeded"

        viewModel.onPromptChange("Test prompt")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.messages.size)
        assertEquals("Quota exceeded", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun `onVoiceResult updates prompt`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onVoiceResult("Spoken command text")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Spoken command text", state.prompt)
    }

    @Test
    fun `onClearHistory clears stored messages`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onPromptChange("Hi")
        viewModel.onSend()
        advanceUntilIdle()

        viewModel.onClearHistory()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
    }
}
