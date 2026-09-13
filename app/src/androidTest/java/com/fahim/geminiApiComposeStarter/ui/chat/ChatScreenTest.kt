package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.model.Author
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val conversation = listOf(
        ChatMessage(1L, "What is Jetpack Compose?", Author.USER, 1L),
        ChatMessage(2L, "A declarative UI toolkit for Android.", Author.MODEL, 2L),
    )

    private fun setScreen(
        state: ChatUiState,
        onPromptChange: (String) -> Unit = {},
        onSend: () -> Unit = {},
        onVoiceClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme(dynamicColor = false) {
                ChatScreen(
                    state = state,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                    onRetry = {},
                    onErrorShown = {},
                    onClearConversation = {},
                    onThemeModeChange = {},
                    onVoiceClick = onVoiceClick,
                )
            }
        }
    }

    @Test
    fun bothTurnsOfTheConversationAreRendered() {
        setScreen(ChatUiState(messages = conversation))

        composeTestRule.onNodeWithTag(ChatTestTags.MESSAGE_LIST).assertIsDisplayed()
        composeTestRule.onNodeWithText("What is Jetpack Compose?").assertIsDisplayed()
        composeTestRule.onNodeWithText("A declarative UI toolkit for Android.").assertIsDisplayed()
    }

    @Test
    fun emptyConversationShowsThePlaceholder() {
        setScreen(ChatUiState())

        composeTestRule
            .onNodeWithText(context.getString(R.string.empty_state_title))
            .assertIsDisplayed()
    }

    @Test
    fun progressIndicatorIsVisibleWhileWaitingForAReply() {
        setScreen(ChatUiState(messages = conversation, isLoading = true))

        composeTestRule.onNodeWithTag(ChatTestTags.LOADING).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.thinking))
            .assertIsDisplayed()
    }

    @Test
    fun validationErrorIsShownUnderThePromptField() {
        setScreen(ChatUiState(promptError = PromptError.EMPTY))

        composeTestRule
            .onNodeWithText(context.getString(R.string.field_cannot_be_empty))
            .assertIsDisplayed()
    }

    @Test
    fun typingInThePromptFieldReportsEachChange() {
        val typed = mutableListOf<String>()
        setScreen(ChatUiState(), onPromptChange = { typed += it })

        composeTestRule.onNodeWithTag(ChatTestTags.PROMPT_FIELD).performTextInput("Hi")

        assertTrue(typed.isNotEmpty())
        assertEquals("Hi", typed.last())
    }

    @Test
    fun tappingSendAndMicInvokeTheirCallbacks() {
        var sends = 0
        var voiceTaps = 0
        setScreen(
            ChatUiState(prompt = "Hello"),
            onSend = { sends++ },
            onVoiceClick = { voiceTaps++ },
        )

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.send))
            .performClick()
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.voice_input))
            .performClick()

        assertEquals(1, sends)
        assertEquals(1, voiceTaps)
    }
}
