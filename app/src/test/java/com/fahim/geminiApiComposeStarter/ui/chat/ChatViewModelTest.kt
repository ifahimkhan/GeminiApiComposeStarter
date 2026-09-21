package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.Author
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.ThemeMode
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val gemini = FakeGeminiRepository()
    private val history = FakeChatHistoryRepository()
    private val preferences = FakeUserPreferencesRepository()
    private val imageStore = FakeImageStore()
    private val speaker = FakeTextSpeaker()

    private fun viewModel(
        hasApiKey: Boolean = true,
        history: FakeChatHistoryRepository = this.history,
        preferences: FakeUserPreferencesRepository = this.preferences,
    ) = ChatViewModel(gemini, history, preferences, imageStore, speaker, hasApiKey)

    private fun ChatViewModel.send(prompt: String) {
        onPromptChange(prompt)
        onSend()
    }

    private fun ChatViewModel.texts() = uiState.value.messages.map { it.author to it.text }

    @Test
    fun `empty prompt shows a field error and does not call Gemini`() {
        val vm = viewModel()

        vm.send("   ")

        assertEquals(PromptError.EMPTY, vm.uiState.value.promptError)
        assertTrue(gemini.prompts.isEmpty())
        assertTrue(vm.uiState.value.messages.isEmpty())
    }

    @Test
    fun `typing clears the field error`() {
        val vm = viewModel()
        vm.send("")

        vm.onPromptChange("h")

        assertNull(vm.uiState.value.promptError)
    }

    @Test
    fun `missing api key reports an error and does not call Gemini`() {
        val vm = viewModel(hasApiKey = false)

        vm.send("hello")

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.errorIsRetryable)
        assertTrue(gemini.prompts.isEmpty())
    }

    @Test
    fun `successful send adds the prompt and the reply and clears the input`() {
        val vm = viewModel()

        vm.send("  What is 2+2?  ")

        assertEquals(listOf("What is 2+2?"), gemini.prompts)
        assertEquals(
            listOf(Author.USER to "What is 2+2?", Author.GEMINI to "Fake reply"),
            vm.texts(),
        )
        with(vm.uiState.value) {
            assertEquals("", prompt)
            assertFalse(isLoading)
            assertNull(errorMessage)
        }
    }

    @Test
    fun `isLoading is true while waiting for the reply`() {
        val gate = CompletableDeferred<Unit>()
        gemini.gate = gate
        val vm = viewModel()

        vm.send("hi")
        assertTrue(vm.uiState.value.isLoading)
        assertEquals(listOf(Author.USER to "hi"), vm.texts())

        gate.complete(Unit)
        assertFalse(vm.uiState.value.isLoading)
        assertEquals(2, vm.uiState.value.messages.size)
    }

    @Test
    fun `sending while a request is in flight is ignored`() {
        gemini.gate = CompletableDeferred()
        val vm = viewModel()

        vm.send("first")
        vm.send("second")

        assertEquals(listOf("first"), gemini.prompts)
        assertEquals(1, vm.uiState.value.messages.size)
    }

    @Test
    fun `failed request keeps the prompt, reports the error and allows retry`() {
        gemini.result = Result.failure(IllegalStateException("boom"))
        val vm = viewModel()

        vm.send("hello")

        with(vm.uiState.value) {
            assertEquals("boom", errorMessage)
            assertTrue(errorIsRetryable)
            assertFalse(isLoading)
            assertTrue(canRetry)
            assertEquals(listOf(Author.USER to "hello"), vm.texts())
        }
    }

    @Test
    fun `failure without a message falls back to a generic error`() {
        gemini.result = Result.failure(RuntimeException())
        val vm = viewModel()

        vm.send("hello")

        assertEquals("Something went wrong", vm.uiState.value.errorMessage)
    }

    @Test
    fun `retry resends the last prompt without duplicating the bubble`() {
        gemini.result = Result.failure(IllegalStateException("boom"))
        val vm = viewModel()
        vm.send("hello")

        gemini.result = Result.success("Recovered")
        vm.onRetry()

        assertEquals(listOf("hello", "hello"), gemini.prompts)
        assertEquals(
            listOf(Author.USER to "hello", Author.GEMINI to "Recovered"),
            vm.texts(),
        )
        with(vm.uiState.value) {
            assertNull(errorMessage)
            assertFalse(errorIsRetryable)
            assertFalse(canRetry)
        }
    }

    @Test
    fun `retry does nothing when the last message is already answered`() {
        val vm = viewModel()
        vm.send("hello")

        vm.onRetry()

        assertEquals(listOf("hello"), gemini.prompts)
    }

    @Test
    fun `onErrorShown clears the error`() {
        gemini.result = Result.failure(IllegalStateException("boom"))
        val vm = viewModel()
        vm.send("hello")

        vm.onErrorShown()

        assertNull(vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.errorIsRetryable)
    }

    @Test
    fun `voice result is appended to the existing prompt`() {
        val vm = viewModel()
        vm.onPromptChange("Tell me")

        vm.onVoiceResult("  about rainbows ")

        assertEquals("Tell me about rainbows", vm.uiState.value.prompt)
    }

    @Test
    fun `voice result fills an empty prompt and blank results are ignored`() {
        val vm = viewModel()

        vm.onVoiceResult("hello there")
        vm.onVoiceResult("   ")

        assertEquals("hello there", vm.uiState.value.prompt)
    }

    @Test
    fun `voice unavailable reports a non retryable error`() {
        val vm = viewModel()

        vm.onVoiceUnavailable()

        assertEquals(ChatViewModel.SPEECH_UNAVAILABLE_MESSAGE, vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.errorIsRetryable)
    }

    @Test
    fun `saved conversation is loaded on start`() {
        val saved = listOf(ChatMessage(0, Author.USER, "old"), ChatMessage(1, Author.GEMINI, "reply"))
        val vm = viewModel(history = FakeChatHistoryRepository(saved))

        assertEquals(saved, vm.uiState.value.messages)
    }

    @Test
    fun `clearing the conversation empties the list`() {
        val vm = viewModel()
        vm.send("hello")

        vm.onClearConversation()

        assertTrue(vm.uiState.value.messages.isEmpty())
    }

    @Test
    fun `saved theme is loaded and selecting a theme updates it`() {
        val vm = viewModel(preferences = FakeUserPreferencesRepository(ThemeMode.DARK))
        assertEquals(ThemeMode.DARK, vm.uiState.value.themeMode)

        vm.onThemeModeSelected(ThemeMode.LIGHT)

        assertEquals(ThemeMode.LIGHT, vm.uiState.value.themeMode)
    }

    // --- Stop generating ---

    @Test
    fun `stop generating cancels the request and keeps the prompt in the chat`() {
        val gate = CompletableDeferred<Unit>()
        gemini.gate = gate
        val vm = viewModel()
        vm.send("slow question")
        assertTrue(vm.uiState.value.isLoading)

        vm.onStopGenerating()
        gate.complete(Unit)

        with(vm.uiState.value) {
            assertFalse(isLoading)
            assertNull(errorMessage)
            assertEquals(listOf(Author.USER to "slow question"), vm.texts())
        }
    }

    @Test
    fun `can send again after stopping`() {
        gemini.gate = CompletableDeferred()
        val vm = viewModel()
        vm.send("first")
        vm.onStopGenerating()

        gemini.gate = null
        vm.send("second")

        assertEquals(
            listOf(Author.USER to "first", Author.USER to "second", Author.GEMINI to "Fake reply"),
            vm.texts(),
        )
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `stop generating when idle does nothing`() {
        val vm = viewModel()

        vm.onStopGenerating()

        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.messages.isEmpty())
    }

    // --- Image input ---

    @Test
    fun `selecting an image attaches it`() {
        val vm = viewModel()

        vm.onImageSelected("content://photo/1")

        assertEquals("stored/content://photo/1", vm.uiState.value.attachedImagePath)
    }

    @Test
    fun `failing to load an image shows an error and attaches nothing`() {
        imageStore.failImport = true
        val vm = viewModel()

        vm.onImageSelected("content://photo/1")

        assertNull(vm.uiState.value.attachedImagePath)
        assertEquals(ChatViewModel.IMAGE_LOAD_FAILED_MESSAGE, vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.errorIsRetryable)
    }

    @Test
    fun `removing the image detaches it and deletes the file`() {
        val vm = viewModel()
        vm.onImageSelected("content://photo/1")

        vm.onImageRemoved()

        assertNull(vm.uiState.value.attachedImagePath)
        assertEquals(listOf("stored/content://photo/1"), imageStore.deleted)
    }

    @Test
    fun `attaching a second image replaces and deletes the first`() {
        val vm = viewModel()
        vm.onImageSelected("content://photo/1")

        vm.onImageSelected("content://photo/2")

        assertEquals("stored/content://photo/2", vm.uiState.value.attachedImagePath)
        assertEquals(listOf("stored/content://photo/1"), imageStore.deleted)
    }

    @Test
    fun `sending text with an image sends both and clears the attachment`() {
        val vm = viewModel()
        vm.onImageSelected("content://photo/1")

        vm.send("What is this?")

        assertEquals(listOf("What is this?"), gemini.prompts)
        assertEquals(listOf<String?>("stored/content://photo/1"), gemini.imagePaths)
        val userMessage = vm.uiState.value.messages.first()
        assertEquals("stored/content://photo/1", userMessage.imagePath)
        assertNull(vm.uiState.value.attachedImagePath)
        assertEquals("", vm.uiState.value.prompt)
    }

    @Test
    fun `an image alone can be sent without text`() {
        val vm = viewModel()
        vm.onImageSelected("content://photo/1")

        vm.onSend()

        assertNull(vm.uiState.value.promptError)
        assertEquals(listOf(""), gemini.prompts)
        assertEquals(2, vm.uiState.value.messages.size)
    }

    @Test
    fun `retry resends the image with the prompt`() {
        gemini.result = Result.failure(IllegalStateException("boom"))
        val vm = viewModel()
        vm.onImageSelected("content://photo/1")
        vm.send("What is this?")

        gemini.result = Result.success("A cat")
        vm.onRetry()

        assertEquals(listOf<String?>("stored/content://photo/1", "stored/content://photo/1"), gemini.imagePaths)
        assertEquals(Author.GEMINI to "A cat", vm.texts().last())
    }

    @Test
    fun `clearing the conversation deletes stored images`() {
        val vm = viewModel()
        vm.onImageSelected("content://photo/1")
        vm.send("What is this?")

        vm.onClearConversation()

        assertTrue(vm.uiState.value.messages.isEmpty())
        assertEquals(listOf("stored/content://photo/1"), imageStore.deleted)
    }

    @Test
    fun `camera unavailable reports a non retryable error`() {
        val vm = viewModel()

        vm.onCameraUnavailable()

        assertEquals(ChatViewModel.CAMERA_UNAVAILABLE_MESSAGE, vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.errorIsRetryable)
    }

    // --- Read aloud ---

    @Test
    fun `speaking a reply reads it without bold markers`() {
        gemini.result = Result.success("The answer is **42**.")
        val vm = viewModel()
        vm.send("hi")
        val replyId = vm.uiState.value.messages.last().id

        vm.onSpeakMessage(replyId)

        assertEquals(listOf(replyId to "The answer is 42."), speaker.spoken)
        assertEquals(replyId, vm.uiState.value.speakingMessageId)
    }

    @Test
    fun `speaking the same reply again stops it`() {
        val vm = viewModel()
        vm.send("hi")
        val replyId = vm.uiState.value.messages.last().id
        vm.onSpeakMessage(replyId)

        vm.onSpeakMessage(replyId)

        assertNull(vm.uiState.value.speakingMessageId)
        assertEquals(1, speaker.spoken.size)
    }

    @Test
    fun `speaking an unknown message does nothing`() {
        val vm = viewModel()

        vm.onSpeakMessage(999)

        assertTrue(speaker.spoken.isEmpty())
    }

    @Test
    fun `text to speech unavailable reports an error`() {
        speaker.available = false
        val vm = viewModel()
        vm.send("hi")

        vm.onSpeakMessage(vm.uiState.value.messages.last().id)

        assertEquals(ChatViewModel.TTS_UNAVAILABLE_MESSAGE, vm.uiState.value.errorMessage)
        assertNull(vm.uiState.value.speakingMessageId)
    }

    @Test
    fun `sending a new message stops any reply being read`() {
        val vm = viewModel()
        vm.send("first")
        vm.onSpeakMessage(vm.uiState.value.messages.last().id)

        vm.send("second")

        assertNull(vm.uiState.value.speakingMessageId)
    }

    @Test
    fun `clearing the conversation stops speech`() {
        val vm = viewModel()
        vm.send("hi")
        vm.onSpeakMessage(vm.uiState.value.messages.last().id)

        vm.onClearConversation()

        assertNull(vm.uiState.value.speakingMessageId)
    }
}
