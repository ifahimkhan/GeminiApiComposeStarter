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
    fun emptyState_displaysGreetingAndPromptField() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceInput = {},
                    onRetry = {},
                    onClearHistory = {},
                )
            }
        }

        composeTestRule.onNodeWithText("How can I help you today?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ask Gemini anything…").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Send").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Voice input").assertIsDisplayed()
    }

    @Test
    fun typingInPrompt_callsOnPromptChange() {
        var enteredText = ""
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = enteredText),
                    onPromptChange = { enteredText = it },
                    onSend = {},
                    onVoiceInput = {},
                    onRetry = {},
                    onClearHistory = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Ask Gemini anything…").performTextInput("Hello Gemini")
        assertEquals("Hello Gemini", enteredText)
    }

    @Test
    fun messagesList_displaysUserAndGeminiBubbles() {
        val userMsg = "What is Kotlin?"
        val geminiMsg = "Kotlin is a modern, statically typed programming language."

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        messages = listOf(
                            ChatMessage(id = 1, sender = MessageSender.USER, text = userMsg),
                            ChatMessage(id = 2, sender = MessageSender.GEMINI, text = geminiMsg),
                        ),
                    ),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceInput = {},
                    onRetry = {},
                    onClearHistory = {},
                )
            }
        }

        composeTestRule.onNodeWithText(userMsg).assertIsDisplayed()
        composeTestRule.onNodeWithText(geminiMsg).assertIsDisplayed()
    }

    @Test
    fun loadingState_displaysThinkingIndicator() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        isLoading = true,
                        messages = listOf(
                            ChatMessage(id = 1, sender = MessageSender.USER, text = "Thinking question"),
                        ),
                    ),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceInput = {},
                    onRetry = {},
                    onClearHistory = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Gemini is thinking…").assertIsDisplayed()
    }

    @Test
    fun errorState_displaysRetryButtonAndTriggersCallback() {
        var retried = false
        val errorMessage = "Network timeout occurred"

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        messages = listOf(
                            ChatMessage(
                                id = 1,
                                sender = MessageSender.GEMINI,
                                text = errorMessage,
                                isError = true,
                            ),
                        ),
                    ),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceInput = {},
                    onRetry = { retried = true },
                    onClearHistory = {},
                )
            }
        }

        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed().performClick()
        assertTrue(retried)
    }
}
