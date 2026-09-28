package com.fahim.geminiApiComposeStarter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.geminiApiComposeStarter.data.local.room.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.room.MessageDeliveryStatus
import com.fahim.geminiApiComposeStarter.data.local.room.SenderRole
import com.fahim.geminiApiComposeStarter.ui.chat.ChatScreen
import com.fahim.geminiApiComposeStarter.ui.chat.ChatUiState
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

    @Test
    fun chatScreen_rendersEmptyStateInitially() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceInputResult = {},
                    onRetry = {},
                    onDismissError = {},
                    onClearChat = {}
                )
            }
        }

        // Welcome header and empty state prompts
        composeTestRule.onNodeWithText("Welcome, Vivaan!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Quick Prompts").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Voice input").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Send").assertIsDisplayed()
    }

    @Test
    fun chatScreen_displaysUserAndGeminiMessages() {
        val testMessages = listOf(
            ChatMessageEntity(
                id = 1,
                senderRole = SenderRole.USER,
                content = "What is Jetpack Compose?"
            ),
            ChatMessageEntity(
                id = 2,
                senderRole = SenderRole.ASSISTANT,
                content = "Jetpack Compose is Android's modern declarative UI toolkit."
            )
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = testMessages),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceInputResult = {},
                    onRetry = {},
                    onDismissError = {},
                    onClearChat = {}
                )
            }
        }

        composeTestRule.onNodeWithText("What is Jetpack Compose?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jetpack Compose is Android's modern declarative UI toolkit.")
            .assertIsDisplayed()
    }

    @Test
    fun chatScreen_showsLoadingIndicatorWhenLoading() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(isLoading = true),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceInputResult = {},
                    onRetry = {},
                    onDismissError = {},
                    onClearChat = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Gemini is thinking...").assertIsDisplayed()
        // Send button should be disabled while loading
        composeTestRule.onNodeWithContentDescription("Send").assertIsNotEnabled()
    }

    @Test
    fun chatScreen_showsErrorCardAndTriggersRetry() {
        var retryClicked = false

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        errorMessage = "Network timeout occurred",
                        lastFailedPrompt = "Explain Kotlin Coroutines"
                    ),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceInputResult = {},
                    onRetry = { retryClicked = true },
                    onDismissError = {},
                    onClearChat = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Network timeout occurred").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed().performClick()

        assertTrue("Retry callback should be triggered", retryClicked)
    }

    @Test
    fun chatScreen_enteringTextEnablesSendButton() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = "Hello Gemini"),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceInputResult = {},
                    onRetry = {},
                    onDismissError = {},
                    onClearChat = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Send").assertIsEnabled()
    }

    @Test
    fun chatScreen_clickingSamplePrompt_triggersOnPromptChange() {
        var capturedPrompt = ""

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = { capturedPrompt = it },
                    onSend = {},
                    onVoiceInputResult = {},
                    onRetry = {},
                    onDismissError = {},
                    onClearChat = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Explain Jetpack Compose state hoisting").performClick()
        assertEquals("Explain Jetpack Compose state hoisting", capturedPrompt)
    }
}
