package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.Author
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.ThemeMode
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** ChatScreen is stateless, so each test just hands it a state and records the callbacks. */
class ChatScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun string(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private val conversation = listOf(
        ChatMessage(0, Author.USER, "What is 2+2?"),
        ChatMessage(1, Author.GEMINI, "It is **4**."),
    )

    private class Calls {
        var sent = 0
        var stopped = 0
        var retried = 0
        var errorShown = 0
        var cleared = 0
        var voice = 0
        var gallery = 0
        var camera = 0
        var imageRemoved = 0
        var typed: String? = null
        var theme: ThemeMode? = null
        var spoken: Long? = null
    }

    private fun setScreen(state: ChatUiState, calls: Calls = Calls()): Calls {
        composeRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = state,
                    widthSizeClass = WindowWidthSizeClass.Compact,
                    onPromptChange = { calls.typed = it },
                    onSend = { calls.sent++ },
                    onStopGenerating = { calls.stopped++ },
                    onRetry = { calls.retried++ },
                    onErrorShown = { calls.errorShown++ },
                    onThemeModeSelected = { calls.theme = it },
                    onClearConversation = { calls.cleared++ },
                    onVoiceInput = { calls.voice++ },
                    onPickFromGallery = { calls.gallery++ },
                    onTakePhoto = { calls.camera++ },
                    onRemoveImage = { calls.imageRemoved++ },
                    onSpeakMessage = { calls.spoken = it },
                )
            }
        }
        return calls
    }

    @Test
    fun emptyConversation_showsHint() {
        setScreen(ChatUiState())

        composeRule.onNodeWithText(string(R.string.empty_chat_hint)).assertIsDisplayed()
    }

    @Test
    fun conversation_showsBothBubbles() {
        setScreen(ChatUiState(messages = conversation))

        composeRule.onNodeWithTag(ChatTestTags.MESSAGE_LIST).assertIsDisplayed()
        composeRule.onNodeWithText("What is 2+2?").assertIsDisplayed()
        // The **bold** markers are rendered, not shown literally.
        composeRule.onNodeWithText("It is 4.").assertIsDisplayed()
    }

    @Test
    fun loading_showsSpinnerAndDisablesTheOtherInputs() {
        setScreen(ChatUiState(messages = conversation.take(1), isLoading = true))

        composeRule.onNodeWithTag(ChatTestTags.LOADING_INDICATOR).assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTestTags.VOICE_BUTTON).assertIsNotEnabled()
        composeRule.onNodeWithTag(ChatTestTags.ATTACH_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun loading_replacesSendWithStop() {
        setScreen(ChatUiState(messages = conversation.take(1), isLoading = true))

        composeRule.onNodeWithTag(ChatTestTags.STOP_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTestTags.SEND_BUTTON).assertDoesNotExist()
    }

    @Test
    fun stopButton_invokesOnStopGenerating() {
        val calls = setScreen(ChatUiState(messages = conversation.take(1), isLoading = true))

        composeRule.onNodeWithTag(ChatTestTags.STOP_BUTTON).performClick()

        assertEquals(1, calls.stopped)
        assertEquals(0, calls.sent)
    }

    @Test
    fun idle_hidesSpinnerAndStopAndEnablesSend() {
        setScreen(ChatUiState(messages = conversation))

        composeRule.onNodeWithTag(ChatTestTags.LOADING_INDICATOR).assertDoesNotExist()
        composeRule.onNodeWithTag(ChatTestTags.STOP_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithTag(ChatTestTags.SEND_BUTTON).assertIsEnabled()
    }

    @Test
    fun typing_reportsTheText() {
        val calls = setScreen(ChatUiState())

        composeRule.onNodeWithTag(ChatTestTags.PROMPT_FIELD).performTextInput("hello")

        assertEquals("hello", calls.typed)
    }

    @Test
    fun sendButton_invokesOnSend() {
        val calls = setScreen(ChatUiState(prompt = "hello"))

        composeRule.onNodeWithTag(ChatTestTags.SEND_BUTTON).performClick()

        assertEquals(1, calls.sent)
    }

    @Test
    fun voiceButton_invokesOnVoiceInput() {
        val calls = setScreen(ChatUiState())

        composeRule.onNodeWithTag(ChatTestTags.VOICE_BUTTON).performClick()

        assertEquals(1, calls.voice)
    }

    @Test
    fun emptyPromptError_isShown() {
        setScreen(ChatUiState(promptError = PromptError.EMPTY))

        composeRule.onNodeWithText(string(R.string.field_cannot_be_empty)).assertIsDisplayed()
    }

    // --- Image input ---

    @Test
    fun attachMenu_galleryItem_invokesCallback() {
        val calls = setScreen(ChatUiState())

        composeRule.onNodeWithTag(ChatTestTags.ATTACH_BUTTON).performClick()
        composeRule.onNodeWithTag(ChatTestTags.GALLERY_ITEM).performClick()

        assertEquals(1, calls.gallery)
        assertEquals(0, calls.camera)
    }

    @Test
    fun attachMenu_cameraItem_invokesCallback() {
        val calls = setScreen(ChatUiState())

        composeRule.onNodeWithTag(ChatTestTags.ATTACH_BUTTON).performClick()
        composeRule.onNodeWithTag(ChatTestTags.CAMERA_ITEM).performClick()

        assertEquals(1, calls.camera)
        assertEquals(0, calls.gallery)
    }

    @Test
    fun noAttachment_hidesThePreview() {
        setScreen(ChatUiState())

        composeRule.onNodeWithTag(ChatTestTags.ATTACHMENT_PREVIEW).assertDoesNotExist()
    }

    @Test
    fun attachment_showsPreviewAndRemoveButton() {
        val calls = setScreen(ChatUiState(attachedImagePath = "/does/not/exist.jpg"))

        composeRule.onNodeWithTag(ChatTestTags.ATTACHMENT_PREVIEW).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.remove_image)).performClick()

        assertEquals(1, calls.imageRemoved)
    }

    // --- Read aloud ---

    @Test
    fun onlyGeminiBubblesHaveASpeakButton() {
        setScreen(ChatUiState(messages = conversation))

        composeRule.onAllNodesWithTag(ChatTestTags.SPEAK_BUTTON).assertCountEquals(1)
    }

    @Test
    fun speakButton_reportsTheMessageId() {
        val calls = setScreen(ChatUiState(messages = conversation))

        composeRule.onAllNodesWithTag(ChatTestTags.SPEAK_BUTTON).onFirst().performClick()

        assertEquals(1L, calls.spoken)
    }

    @Test
    fun speakButton_showsStopWhileThatReplyIsBeingRead() {
        setScreen(ChatUiState(messages = conversation, speakingMessageId = 1))

        composeRule.onNodeWithContentDescription(string(R.string.stop_reading)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.read_aloud)).assertDoesNotExist()
    }

    @Test
    fun speakButton_showsReadAloudWhenSilent() {
        setScreen(ChatUiState(messages = conversation))

        composeRule.onNodeWithContentDescription(string(R.string.read_aloud)).assertIsDisplayed()
    }

    // --- Errors ---

    @Test
    fun failedRequest_showsErrorWithRetry() {
        val calls = setScreen(
            ChatUiState(
                messages = conversation.take(1),
                errorMessage = "Network down",
                errorIsRetryable = true,
            ),
        )

        composeRule.onNodeWithText("Network down").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.retry)).performClick()
        composeRule.waitForIdle()

        assertEquals(1, calls.errorShown)
        assertEquals(1, calls.retried)
    }

    @Test
    fun nonRetryableError_hasNoRetryAction() {
        setScreen(ChatUiState(errorMessage = "No speech service", errorIsRetryable = false))

        composeRule.onNodeWithText("No speech service").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.retry)).assertDoesNotExist()
    }

    // --- Menu ---

    @Test
    fun menu_clearConversation_invokesCallback() {
        val calls = setScreen(ChatUiState(messages = conversation))

        composeRule.onNodeWithTag(ChatTestTags.MENU_BUTTON).performClick()
        composeRule.onNodeWithText(string(R.string.clear_conversation)).performClick()

        assertEquals(1, calls.cleared)
    }

    @Test
    fun menu_clearConversation_isDisabledWhenEmpty() {
        setScreen(ChatUiState())

        composeRule.onNodeWithTag(ChatTestTags.MENU_BUTTON).performClick()

        composeRule.onNodeWithText(string(R.string.clear_conversation)).assertIsNotEnabled()
    }

    @Test
    fun menu_selectingDarkTheme_invokesCallback() {
        val calls = setScreen(ChatUiState())

        composeRule.onNodeWithTag(ChatTestTags.MENU_BUTTON).performClick()
        composeRule.onNodeWithText(string(R.string.theme_dark)).performClick()

        assertTrue(calls.theme == ThemeMode.DARK)
    }
}
