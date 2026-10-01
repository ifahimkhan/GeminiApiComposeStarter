package com.fahim.geminiApiComposeStarter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.Participant
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
    fun emptyState_displaysWelcomeAndSuggestions() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(userName = "Rudra"),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceResult = {},
                    onClearHistory = {},
                    onDismissError = {},
                    onSelectSuggestion = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Welcome, Rudra!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Explain quantum computing simply").assertIsDisplayed()
    }

    @Test
    fun conversationMessages_areDisplayedInBubbles() {
        val messages = listOf(
            ChatMessageEntity(
                text = "What is Kotlin?",
                participant = Participant.USER
            ),
            ChatMessageEntity(
                text = "Kotlin is a modern, statically typed language.",
                participant = Participant.GEMINI
            )
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
                    onSelectSuggestion = {}
                )
            }
        }

        composeTestRule.onNodeWithText("What is Kotlin?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kotlin is a modern, statically typed language.").assertIsDisplayed()
    }

    @Test
    fun typingInPromptBar_updatesStateAndEnablesSend() {
        var enteredText = ""
        var sendClicked = false

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = enteredText),
                    onPromptChange = { enteredText = it },
                    onSend = { sendClicked = true },
                    onVoiceResult = {},
                    onClearHistory = {},
                    onDismissError = {},
                    onSelectSuggestion = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Voice input").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Send").assertIsDisplayed()
    }

    @Test
    fun loadingState_showsThinkingIndicator() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(isLoading = true),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceResult = {},
                    onClearHistory = {},
                    onDismissError = {},
                    onSelectSuggestion = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Gemini is thinking...").assertIsDisplayed()
    }

    @Test
    fun errorState_showsErrorCard() {
        var dismissed = false
        var retried = false

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        errorMessage = "Failed to connect to API"
                    ),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceResult = {},
                    onClearHistory = {},
                    onDismissError = { dismissed = true },
                    onSelectSuggestion = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Failed to connect to API").assertIsDisplayed()
    }
}
