package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setScreen(
        state: ChatUiState,
        onPromptChange: (String) -> Unit = {},
        onSend: () -> Unit = {},
        onSuggestionClick: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = state,
                    widthSizeClass = WindowWidthSizeClass.Compact,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                    onRetry = {},
                    onErrorShown = {},
                    onSuggestionClick = onSuggestionClick,
                    onVoiceResult = {},
                    onClearChat = {},
                    onThemeModeChange = {},
                )
            }
        }
    }

    @Test
    fun emptyState_showsGreetingAndSuggestions() {
        setScreen(ChatUiState())

        composeRule.onNodeWithText("Hi, I'm Gemini").assertIsDisplayed()
        composeRule.onNodeWithText("Explain Kotlin coroutines simply").assertIsDisplayed()
    }

    @Test
    fun conversation_rendersUserAndGeminiBubbles() {
        setScreen(
            ChatUiState(
                messages = listOf(
                    ChatMessage(1, "What is Compose?", isUser = true, timestamp = 1_000L),
                    ChatMessage(2, "A declarative UI toolkit.", isUser = false, timestamp = 2_000L),
                ),
            ),
        )

        composeRule.onNodeWithText("What is Compose?").assertIsDisplayed()
        composeRule.onNodeWithText("A declarative UI toolkit.").assertIsDisplayed()
    }

    @Test
    fun loading_showsThinkingIndicator() {
        setScreen(
            ChatUiState(
                messages = listOf(ChatMessage(1, "Hi", isUser = true, timestamp = 1_000L)),
                isLoading = true,
            ),
        )

        composeRule.onNodeWithText("Gemini is thinking…").assertIsDisplayed()
    }

    @Test
    fun typingAndSending_invokesCallbacks() {
        var typed = ""
        var sent = false
        composeRule.setContent {
            // Hold the prompt like the ViewModel would, so the text field keeps what is typed.
            var prompt by remember { mutableStateOf("") }
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = prompt),
                    widthSizeClass = WindowWidthSizeClass.Compact,
                    onPromptChange = { prompt = it; typed = it },
                    onSend = { sent = true },
                    onRetry = {},
                    onErrorShown = {},
                    onSuggestionClick = {},
                    onVoiceResult = {},
                    onClearChat = {},
                    onThemeModeChange = {},
                )
            }
        }

        composeRule.onNodeWithText("Message Gemini…").performTextInput("hello")
        composeRule.onNodeWithContentDescription("Send").performClick()

        assertEquals("hello", typed)
        assertTrue(sent)
    }

    @Test
    fun tappingSuggestion_reportsItsText() {
        var clicked = ""
        setScreen(ChatUiState(), onSuggestionClick = { clicked = it })

        composeRule.onNodeWithText("Write a short poem about rain").performClick()

        assertEquals("Write a short poem about rain", clicked)
    }

    @Test
    fun longReply_replacingThinkingBubble_scrollsToTheBottom() {
        val history = (1L..12L).map { ChatMessage(it, "Question or answer number $it", isUser = it % 2 == 1L, timestamp = 1_000L * it) }
        var state by mutableStateOf(ChatUiState(messages = history, isLoading = true))
        composeRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = state,
                    widthSizeClass = WindowWidthSizeClass.Compact,
                    onPromptChange = {}, onSend = {}, onRetry = {}, onErrorShown = {},
                    onSuggestionClick = {}, onVoiceResult = {}, onClearChat = {}, onThemeModeChange = {},
                )
            }
        }

        // The reply arrives: the typing bubble disappears and one message appears, so the item count is unchanged.
        val longReply = ChatMessage(13, "word ".repeat(400) + "THE END", isUser = false, timestamp = 13_000L)
        state = ChatUiState(messages = history + longReply, isLoading = false)
        composeRule.waitForIdle()

        // The timestamp sits under the bubble, so seeing it means the end of the reply is on screen.
        composeRule.onNodeWithTag("time_13").assertIsDisplayed()
    }
}
