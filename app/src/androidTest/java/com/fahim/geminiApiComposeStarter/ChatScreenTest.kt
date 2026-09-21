package com.fahim.geminiApiComposeStarter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatScreen
import com.fahim.geminiApiComposeStarter.ui.chat.ChatUiState
import com.fahim.geminiApiComposeStarter.ui.chat.MessageRole
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun inputFieldAndButtonsAreDisplayed() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceResult = {},
                    onClearHistory = {},
                    onDismissError = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Ask Gemini something…").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Voice input").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Send message").assertIsDisplayed()
    }

    @Test
    fun typingInInputFieldTriggersOnPromptChange() {
        var enteredText = ""
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = enteredText),
                    onPromptChange = { enteredText = it },
                    onSend = {},
                    onVoiceResult = {},
                    onClearHistory = {},
                    onDismissError = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Ask Gemini something…").performTextInput("Hello World")
        assertEquals("Hello World", enteredText)
    }

    @Test
    fun messagesRenderInBubbles() {
        val messages = listOf(
            ChatMessage(id = 1, role = MessageRole.USER, text = "User test message"),
            ChatMessage(id = 2, role = MessageRole.GEMINI, text = "Gemini test message"),
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = messages),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceResult = {},
                    onClearHistory = {},
                    onDismissError = {},
                )
            }
        }

        composeTestRule.onNodeWithText("User test message").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gemini test message").assertIsDisplayed()
    }

    @Test
    fun loadingStateDisplaysThinkingIndicator() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(isLoading = true),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceResult = {},
                    onClearHistory = {},
                    onDismissError = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Gemini is thinking…").assertIsDisplayed()
    }
}
