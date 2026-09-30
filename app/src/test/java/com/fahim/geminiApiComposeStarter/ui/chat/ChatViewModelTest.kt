package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.MissingApiKeyException
import com.fahim.geminiApiComposeStarter.data.SafetyBlockedException
import com.fahim.geminiApiComposeStarter.data.security.ApiKeyStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeGeminiRepository
    private lateinit var fakeApiKeyStorage: FakeApiKeyStorage

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeGeminiRepository()
        fakeApiKeyStorage = FakeApiKeyStorage(key = "test_key")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `empty prompt triggers EMPTY promptError`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(fakeRepository, fakeApiKeyStorage)
        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertNull(fakeRepository.lastPrompt)
    }

    @Test
    fun `prompt exceeding MAX_PROMPT_LENGTH triggers TOO_LONG promptError`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(fakeRepository, fakeApiKeyStorage)
        val oversizedPrompt = "a".repeat(ChatViewModel.MAX_PROMPT_LENGTH + 1)

        viewModel.onPromptChange(oversizedPrompt)
        assertEquals(PromptError.TOO_LONG, viewModel.uiState.value.promptError)

        viewModel.onSend()
        assertEquals(PromptError.TOO_LONG, viewModel.uiState.value.promptError)
        assertNull(fakeRepository.lastPrompt)
    }

    @Test
    fun `missing API key prevents send and opens API key dialog`() = runTest(testDispatcher) {
        fakeApiKeyStorage.clearApiKey()
        val viewModel = ChatViewModel(fakeRepository, fakeApiKeyStorage)

        viewModel.onPromptChange("Hello")
        viewModel.onSend()

        assertTrue(viewModel.uiState.value.showApiKeyDialog)
        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
        assertNull(fakeRepository.lastPrompt)
    }

    @Test
    fun `saving API key updates state and closes dialog`() = runTest(testDispatcher) {
        fakeApiKeyStorage.clearApiKey()
        val viewModel = ChatViewModel(fakeRepository, fakeApiKeyStorage)

        viewModel.onOpenApiKeyDialog()
        assertTrue(viewModel.uiState.value.showApiKeyDialog)

        viewModel.onSaveApiKey("new_valid_key")
        assertTrue(viewModel.uiState.value.isApiKeyConfigured)
        assertFalse(viewModel.uiState.value.showApiKeyDialog)
        assertEquals("new_valid_key", fakeApiKeyStorage.getApiKey())
    }

    @Test
    fun `successful prompt submission updates response and clears errors`() = runTest(testDispatcher) {
        fakeRepository.result = Result.success("This is the AI response.")
        val viewModel = ChatViewModel(fakeRepository, fakeApiKeyStorage)

        viewModel.onPromptChange("Tell me a joke")
        viewModel.onSend()

        advanceUntilIdle()

        assertEquals("This is the AI response.", viewModel.uiState.value.response)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `sensitive network and API errors are sanitized in UI state`() = runTest(testDispatcher) {
        // Leaked URL and internal details should be replaced with safe user message
        fakeRepository.result = Result.failure(
            RuntimeException("HTTP 400: API_KEY_INVALID https://generativelanguage.googleapis.com/v1beta?key=secret")
        )
        val viewModel = ChatViewModel(fakeRepository, fakeApiKeyStorage)

        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()

        val errorMessage = viewModel.uiState.value.errorMessage.orEmpty()
        assertFalse("Error message must not leak raw API key or URL", errorMessage.contains("key="))
        assertFalse("Error message must not leak URL endpoint", errorMessage.contains("googleapis.com"))
        assertTrue("Error message should guide user to check settings", errorMessage.contains("settings"))
    }

    @Test
    fun `safety blocked exception produces safe explanatory error`() = runTest(testDispatcher) {
        fakeRepository.result = Result.failure(SafetyBlockedException("Content filtered"))
        val viewModel = ChatViewModel(fakeRepository, fakeApiKeyStorage)

        viewModel.onPromptChange("Unsafe prompt")
        viewModel.onSend()
        advanceUntilIdle()

        val errorMessage = viewModel.uiState.value.errorMessage.orEmpty()
        assertTrue(errorMessage.contains("safety filters", ignoreCase = true))
    }

    @Test
    fun `network failure produces connection error`() = runTest(testDispatcher) {
        fakeRepository.result = Result.failure(UnknownHostException("Failed to resolve host"))
        val viewModel = ChatViewModel(fakeRepository, fakeApiKeyStorage)

        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()

        val errorMessage = viewModel.uiState.value.errorMessage.orEmpty()
        assertTrue(errorMessage.contains("network connection", ignoreCase = true))
    }
}

private class FakeGeminiRepository : GeminiRepository {
    var result: Result<String> = Result.success("Mock reply")
    var lastPrompt: String? = null

    override suspend fun generateText(prompt: String): Result<String> {
        lastPrompt = prompt
        return result
    }
}

private class FakeApiKeyStorage(private var key: String? = null) : ApiKeyStorage {
    override fun getApiKey(): String? = key
    override fun saveApiKey(key: String) { this.key = key }
    override fun clearApiKey() { this.key = null }
    override fun hasApiKey(): Boolean = !key.isNullOrBlank()
}
