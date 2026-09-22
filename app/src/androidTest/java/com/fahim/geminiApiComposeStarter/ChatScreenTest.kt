package com.fahim.geminiApiComposeStarter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.ui.chat.ChatScreen
import com.fahim.geminiApiComposeStarter.ui.chat.ChatUiState
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatScreen_displaysTitleAndEmptyStateWhenNoMessages() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = emptyList()),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceInput = {},
                    onClearHistory = {},
                    onThemeChange = {},
                    onDismissError = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Welcome to Gemini AI").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ask Gemini anything...").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Voice Input").assertIsDisplayed()
    }

    @Test
    fun chatScreen_rendersUserAndGeminiMessages() {
        val messages = listOf(
            ChatMessageEntity(id = 1, text = "What is Jetpack Compose?", role = "USER"),
            ChatMessageEntity(id = 2, text = "Jetpack Compose is Android's modern UI toolkit.", role = "GEMINI")
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = messages),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceInput = {},
                    onClearHistory = {},
                    onThemeChange = {},
                    onDismissError = {}
                )
            }
        }

        composeTestRule.onNodeWithText("What is Jetpack Compose?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jetpack Compose is Android's modern UI toolkit.").assertIsDisplayed()
    }

    @Test
    fun chatScreen_displaysLoadingIndicatorWhenStateIsLoading() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(isLoading = true),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceInput = {},
                    onClearHistory = {},
                    onThemeChange = {},
                    onDismissError = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Gemini is thinking...").assertIsDisplayed()
    }
}
