package com.fahim.geminiApiComposeStarter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.geminiApiComposeStarter.data.local.room.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.room.MessageRole
import com.fahim.geminiApiComposeStarter.ui.chat.ChatScreen
import com.fahim.geminiApiComposeStarter.ui.chat.ChatUiState
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests using createComposeRule().
 * Validates UI rendering, state transitions, message bubbles, empty states,
 * loading indicators, error banners, and user interactions.
 */
@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyState_displaysWelcomeAndSuggestions() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Welcome to Gemini Chat").assertIsDisplayed()
        composeTestRule.onNodeWithText("Try asking:").assertIsDisplayed()
        composeTestRule.onNodeWithText("Explain Android Jetpack Compose in simple terms").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Voice input").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Send message").assertIsDisplayed()
    }

    @Test
    fun messagesList_displaysUserAndGeminiBubbles() {
        val messages = listOf(
            ChatMessageEntity(
                id = 1,
                content = "What is Kotlin coroutines?",
                role = MessageRole.USER,
                timestamp = System.currentTimeMillis() - 10000,
            ),
            ChatMessageEntity(
                id = 2,
                content = "Coroutines are lightweight threads for asynchronous programming.",
                role = MessageRole.GEMINI,
                timestamp = System.currentTimeMillis(),
            ),
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = messages),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithText("What is Kotlin coroutines?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Coroutines are lightweight threads for asynchronous programming.").assertIsDisplayed()
    }

    @Test
    fun loadingState_displaysThinkingIndicator() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(isLoading = true),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Gemini is thinking...").assertIsDisplayed()
        // Send button should be disabled while loading
        composeTestRule.onNodeWithContentDescription("Send message").assertIsNotEnabled()
    }

    @Test
    fun sendButton_enabledOnlyWhenPromptIsNotEmpty() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = "Valid query", isLoading = false),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Send message").assertIsEnabled()
    }

    @Test
    fun sendButton_clickTriggersOnSendCallback() {
        var sendClicked = false

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = "My Question", isLoading = false),
                    onPromptChange = {},
                    onSend = { sendClicked = true },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Send message").performClick()
        assertTrue(sendClicked)
    }

    @Test
    fun suggestionChip_clickUpdatesPrompt() {
        var clickedSuggestion = ""

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = { clickedSuggestion = it },
                    onSend = {},
                )
            }
        }

        val suggestion = "Explain Android Jetpack Compose in simple terms"
        composeTestRule.onNodeWithText(suggestion).performClick()
        assertEquals(suggestion, clickedSuggestion)
    }

    @Test
    fun errorCard_displaysRetryButtonWhenFailed() {
        var retryClicked = false

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        lastFailedPrompt = "Failed prompt",
                        isLoading = false,
                    ),
                    onPromptChange = {},
                    onSend = {},
                    onRetry = { retryClicked = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Failed to receive response").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").performClick()
        assertTrue(retryClicked)
    }
}
