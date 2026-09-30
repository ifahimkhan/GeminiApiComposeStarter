package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Fake GeminiRepository for robust unit testing of ChatViewModel.
 */
class FakeGeminiRepository(
    private val shouldSucceed: Boolean = true,
    private val responseText: String = "Hello from Fake Gemini!",
    private val errorMessage: String = "Network error",
    private var isKeyConfigured: Boolean = true,
) : GeminiRepository {

    val messages = mutableListOf<ChatMessage>()
    private val historyFlow = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val prefsFlow = MutableStateFlow(
        UserPreferences(
            userName = "Aarya",
            selectedModel = "gemini-1.5-flash",
            tone = "Balanced",
            isKeyConfigured = isKeyConfigured,
        )
    )

    override suspend fun generateText(prompt: String): Result<String> {
        return if (shouldSucceed) {
            Result.success(responseText)
        } else {
            Result.failure(RuntimeException(errorMessage))
        }
    }

    override fun getChatHistory(): Flow<List<ChatMessage>> = historyFlow.asStateFlow()

    override suspend fun saveMessage(message: ChatMessage): Long {
        messages.add(message)
        historyFlow.value = messages.toList()
        return message.id
    }

    override suspend fun clearHistory() {
        messages.clear()
        historyFlow.value = emptyList()
    }

    override fun getUserPreferences(): Flow<UserPreferences> = prefsFlow.asStateFlow()

    override suspend fun updatePreferences(userName: String, model: String, tone: String) {
        prefsFlow.value = prefsFlow.value.copy(
            userName = userName,
            selectedModel = model,
            tone = tone,
        )
    }

    override suspend fun saveCustomApiKey(key: String) {
        isKeyConfigured = key.isNotBlank()
        prefsFlow.value = prefsFlow.value.copy(isKeyConfigured = isKeyConfigured)
    }

    override suspend fun hasApiKey(): Boolean = isKeyConfigured
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onPromptChange_updatesPromptAndClearsError() = runTest(testDispatcher) {
        val fakeRepo = FakeGeminiRepository()
        val viewModel = ChatViewModel(fakeRepo)
        advanceUntilIdle()

        viewModel.onPromptChange("What is Jetpack Compose?")
        assertEquals("What is Jetpack Compose?", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onSend_withEmptyPrompt_setsPromptError() = runTest(testDispatcher) {
        val fakeRepo = FakeGeminiRepository()
        val viewModel = ChatViewModel(fakeRepo)
        advanceUntilIdle()

        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun onSend_withoutApiKey_setsErrorMessage() = runTest(testDispatcher) {
        val fakeRepo = FakeGeminiRepository(isKeyConfigured = false)
        val viewModel = ChatViewModel(fakeRepo)
        advanceUntilIdle()

        viewModel.onPromptChange("Hello")
        viewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun onSend_success_persistsUserAndAiMessagesAndResetsLoading() = runTest(testDispatcher) {
        val fakeRepo = FakeGeminiRepository(shouldSucceed = true, responseText = "Jetpack Compose is modern UI toolkit.")
        val viewModel = ChatViewModel(fakeRepo)
        advanceUntilIdle()

        viewModel.onPromptChange("Explain Compose")
        viewModel.onSend()

        // Prompt input is immediately cleared
        assertEquals("", viewModel.uiState.value.prompt)
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(2, viewModel.uiState.value.messages.size)
        assertEquals("Explain Compose", viewModel.uiState.value.messages[0].text)
        assertTrue(viewModel.uiState.value.messages[0].isUser)
        assertEquals("Jetpack Compose is modern UI toolkit.", viewModel.uiState.value.messages[1].text)
        assertFalse(viewModel.uiState.value.messages[1].isUser)
    }

    @Test
    fun onSend_failure_setsErrorMessageAndStopsLoading() = runTest(testDispatcher) {
        val fakeRepo = FakeGeminiRepository(shouldSucceed = false, errorMessage = "Quota exceeded")
        val viewModel = ChatViewModel(fakeRepo)
        advanceUntilIdle()

        viewModel.onPromptChange("Hello")
        viewModel.onSend()

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Quota exceeded", viewModel.uiState.value.errorMessage)
        // User message was persisted, but AI message was not
        assertEquals(1, viewModel.uiState.value.messages.size)
    }

    @Test
    fun onClearHistory_clearsMessages() = runTest(testDispatcher) {
        val fakeRepo = FakeGeminiRepository()
        val viewModel = ChatViewModel(fakeRepo)
        advanceUntilIdle()

        viewModel.onPromptChange("Test prompt")
        viewModel.onSend()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.onClearHistory()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun onVoiceInput_appendsOrSetsTranscription() = runTest(testDispatcher) {
        val fakeRepo = FakeGeminiRepository()
        val viewModel = ChatViewModel(fakeRepo)
        advanceUntilIdle()

        viewModel.onVoiceInput("Tell me a joke")
        assertEquals("Tell me a joke", viewModel.uiState.value.prompt)

        viewModel.onVoiceInput("about Android")
        assertEquals("Tell me a joke about Android", viewModel.uiState.value.prompt)
    }

    @Test
    fun onSelectPromptSuggestion_updatesPrompt() = runTest(testDispatcher) {
        val fakeRepo = FakeGeminiRepository()
        val viewModel = ChatViewModel(fakeRepo)
        advanceUntilIdle()

        viewModel.onSelectPromptSuggestion("Compose best practices")
        assertEquals("Compose best practices", viewModel.uiState.value.prompt)
    }

    @Test
    fun onToggleSettings_updatesState() = runTest(testDispatcher) {
        val fakeRepo = FakeGeminiRepository()
        val viewModel = ChatViewModel(fakeRepo)
        advanceUntilIdle()

        viewModel.onToggleSettings(true)
        assertTrue(viewModel.uiState.value.isSettingsOpen)

        viewModel.onToggleSettings(false)
        assertFalse(viewModel.uiState.value.isSettingsOpen)
    }

    @Test
    fun onUpdatePreferences_updatesPreferencesInState() = runTest(testDispatcher) {
        val fakeRepo = FakeGeminiRepository()
        val viewModel = ChatViewModel(fakeRepo)
        advanceUntilIdle()

        viewModel.onUpdatePreferences("Aarya Bhoye", "gemini-2.0-flash", "Creative")
        advanceUntilIdle()

        assertEquals("Aarya Bhoye", viewModel.uiState.value.userName)
        assertEquals("gemini-2.0-flash", viewModel.uiState.value.selectedModel)
        assertEquals("Creative", viewModel.uiState.value.tone)
    }
}
