package com.fahim.geminiApiComposeStarter

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.fahim.geminiApiComposeStarter.ui.chat.*
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatScreen_rendersCorrectly_showsWelcomeState() {
        val state = ChatUiState(messages = emptyList())
        
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = state,
                    onPromptChange = {},
                    onSend = {}
                )
            }
        }

        // Verify Top Bar
        composeTestRule.onNodeWithText("Aditri's Gemini Assistant").assertIsDisplayed()
        
        // Verify Welcome State
        composeTestRule.onNodeWithText("Welcome to Gemini Chat").assertIsDisplayed()
        
        // Verify Input Bar elements
        composeTestRule.onNodeWithContentDescription("Voice input").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enter your prompt here").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Send").assertIsDisplayed()
    }

    @Test
    fun chatScreen_userEntersText_sendButtonEnabled() {
        var promptValue = ""
        composeTestRule.setContent {
            var state by remember { mutableStateOf(ChatUiState()) }
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = state,
                    onPromptChange = { 
                        state = state.copy(prompt = it)
                        promptValue = it
                    },
                    onSend = {}
                )
            }
        }

        val input = composeTestRule.onNodeWithText("Enter your prompt here")
        input.performTextInput("Hello Assistant")
        
        assertEquals("Hello Assistant", promptValue)
        composeTestRule.onNodeWithContentDescription("Send").assertIsEnabled()
    }

    @Test
    fun chatScreen_messageFlow_showsUserAndGeminiBubbles() {
        val messages = listOf(
            ChatMessage(text = "Hello", sender = MessageSender.USER),
            ChatMessage(text = "Hi there!", sender = MessageSender.GEMINI)
        )
        val state = ChatUiState(messages = messages)

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = state,
                    onPromptChange = {},
                    onSend = {}
                )
            }
        }

        // Verify messages appear
        composeTestRule.onNodeWithText("Hello").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hi there!").assertIsDisplayed()
        
        // Welcome state should be gone
        composeTestRule.onNodeWithText("Welcome to Gemini Chat").assertDoesNotExist()
    }

    @Test
    fun chatScreen_loadingState_showsTypingIndicator() {
        val state = ChatUiState(
            messages = listOf(ChatMessage(text = "Msg", sender = MessageSender.USER)), 
            isLoading = true
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = state,
                    onPromptChange = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Gemini is typing...").assertIsDisplayed()
    }

    @Test
    fun chatScreen_errorState_showsRetrySnackbar() {
        val state = ChatUiState(errorMessage = "Network Error")
        var retryCalled = false

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = state,
                    onPromptChange = {},
                    onSend = {},
                    onRetry = { retryCalled = true }
                )
            }
        }

        // Verify error message in Snackbar
        composeTestRule.onNodeWithText("Network Error").assertIsDisplayed()
        
        // Verify Retry button (Material 3 Snackbar action labels are usually uppercase)
        val retryBtn = composeTestRule.onNodeWithText("Retry", ignoreCase = true)
        retryBtn.assertIsDisplayed()
        retryBtn.performClick()
        
        // Verify callback
        assertTrue(retryCalled)
    }

    @Test
    fun chatScreen_microphoneButton_isPresent() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Voice input").assertIsDisplayed()
    }
}
