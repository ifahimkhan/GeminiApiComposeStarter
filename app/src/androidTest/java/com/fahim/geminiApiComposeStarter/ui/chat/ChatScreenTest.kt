package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatScreen_rendersInitialEmptyStateAndInputBar() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceInput = {},
                    onToggleTheme = {},
                    onClearHistory = {},
                    onDismissError = {},
                )
            }
        }

        // Verify empty state welcome message is shown
        composeTestRule.onNodeWithText("Welcome to Gemini AI Chat").assertIsDisplayed()

        // Verify prompt input, voice button, and send button exist
        composeTestRule.onNodeWithTag("prompt_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("voice_button").assertIsDisplayed()

        // Send button should be disabled when prompt is empty
        composeTestRule.onNodeWithTag("send_button").assertIsNotEnabled()
    }

    @Test
    fun chatScreen_typingPrompt_invokesOnPromptChange() {
        var capturedPrompt = ""
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = capturedPrompt),
                    onPromptChange = { capturedPrompt = it },
                    onSend = {},
                    onVoiceInput = {},
                    onToggleTheme = {},
                    onClearHistory = {},
                    onDismissError = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("prompt_input").performTextInput("Hello Gemini")
        assertEquals("Hello Gemini", capturedPrompt)
    }

    @Test
    fun chatScreen_sendButtonClick_triggersOnSend() {
        var sendClicked = false
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = "Testing send"),
                    onPromptChange = {},
                    onSend = { sendClicked = true },
                    onVoiceInput = {},
                    onToggleTheme = {},
                    onClearHistory = {},
                    onDismissError = {},
                )
            }
        }

        val sendButton = composeTestRule.onNodeWithTag("send_button")
        sendButton.assertIsEnabled()
        sendButton.performClick()

        assertTrue(sendClicked)
    }

    @Test
    fun chatScreen_rendersConversationMessages() {
        val testMessages = listOf(
            ChatMessage(id = 1, text = "What is Kotlin?", isFromUser = true),
            ChatMessage(id = 2, text = "Kotlin is a modern programming language.", isFromUser = false),
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = testMessages),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceInput = {},
                    onToggleTheme = {},
                    onClearHistory = {},
                    onDismissError = {},
                )
            }
        }

        composeTestRule.onNodeWithText("What is Kotlin?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kotlin is a modern programming language.").assertIsDisplayed()
    }

    @Test
    fun chatScreen_displaysLoadingIndicatorWhenLoading() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        messages = listOf(ChatMessage(1, "Question", true)),
                        isLoading = true,
                    ),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceInput = {},
                    onToggleTheme = {},
                    onClearHistory = {},
                    onDismissError = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gemini is thinking…").assertIsDisplayed()
    }

    @Test
    fun chatScreen_displaysErrorMessageInSnackbar() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(errorMessage = "Network timeout"),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceInput = {},
                    onToggleTheme = {},
                    onClearHistory = {},
                    onDismissError = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Network timeout").assertIsDisplayed()
    }
}
