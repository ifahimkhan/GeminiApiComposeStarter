package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatScreen_displaysEmptyState_whenNoMessages() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = emptyList()),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceInputResult = {},
                    onClearChat = {},
                    onToggleAutoScroll = {},
                    onDismissError = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("How can I help you today?")
            .assertIsDisplayed()
    }

    @Test
    fun chatScreen_displaysUserAndGeminiMessages() {
        val messages = listOf(
            ChatMessage(id = "1", text = "Hello Gemini", isFromUser = true),
            ChatMessage(id = "2", text = "Hello Human! How can I assist?", isFromUser = false)
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = messages),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceInputResult = {},
                    onClearChat = {},
                    onToggleAutoScroll = {},
                    onDismissError = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Hello Gemini").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hello Human! How can I assist?").assertIsDisplayed()
    }

    @Test
    fun chatScreen_typingTextAndClickingSend_triggersCallbacks() {
        var typedPrompt = ""
        var sendClicked = false

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = typedPrompt),
                    onPromptChange = { typedPrompt = it },
                    onSend = { sendClicked = true },
                    onVoiceInputResult = {},
                    onClearChat = {},
                    onToggleAutoScroll = {},
                    onDismissError = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Ask Gemini anything...")
            .performTextInput("What is Kotlin?")

        assertEquals("What is Kotlin?", typedPrompt)

        // Re-compose with updated prompt to enable send button
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = typedPrompt),
                    onPromptChange = { typedPrompt = it },
                    onSend = { sendClicked = true },
                    onVoiceInputResult = {},
                    onClearChat = {},
                    onToggleAutoScroll = {},
                    onDismissError = {}
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Send")
            .performClick()

        assertTrue(sendClicked)
    }

    @Test
    fun chatScreen_displaysThinkingIndicator_whenLoading() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(isLoading = true, messages = listOf(ChatMessage(text = "Hi", isFromUser = true))),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceInputResult = {},
                    onClearChat = {},
                    onToggleAutoScroll = {},
                    onDismissError = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Gemini is thinking...")
            .assertIsDisplayed()
    }
}
