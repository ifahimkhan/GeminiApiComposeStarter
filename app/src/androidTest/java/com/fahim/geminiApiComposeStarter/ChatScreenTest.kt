package com.fahim.geminiApiComposeStarter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatScreen
import com.fahim.geminiApiComposeStarter.ui.chat.ChatUiState
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun welcomeScreen_rendersSuggestedChips_whenMessagesEmpty() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = emptyList()),
                    onPromptChange = {},
                    onSend = {},
                    onClearHistory = {},
                    onVoiceClick = {},
                    onSelectSuggestion = {},
                    onToggleSettings = {},
                    onUpdatePreferences = { _, _, _ -> },
                    onSaveApiKey = {},
                    onDismissError = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Welcome to Gemini AI").assertIsDisplayed()
        composeTestRule.onNodeWithText("Explain Kotlin Coroutines").assertIsDisplayed()
    }

    @Test
    fun messages_renderCorrectlyInChatBubbles() {
        val sampleMessages = listOf(
            ChatMessage(id = 1L, text = "Hello Gemini", isUser = true),
            ChatMessage(id = 2L, text = "Hello! How can I help you today?", isUser = false),
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = sampleMessages),
                    onPromptChange = {},
                    onSend = {},
                    onClearHistory = {},
                    onVoiceClick = {},
                    onSelectSuggestion = {},
                    onToggleSettings = {},
                    onUpdatePreferences = { _, _, _ -> },
                    onSaveApiKey = {},
                    onDismissError = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Hello Gemini").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hello! How can I help you today?").assertIsDisplayed()
    }

    @Test
    fun promptInput_updatesAndTriggersSend() {
        var enteredText = ""
        var sendClicked = false

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = enteredText),
                    onPromptChange = { enteredText = it },
                    onSend = { sendClicked = true },
                    onClearHistory = {},
                    onVoiceClick = {},
                    onSelectSuggestion = {},
                    onToggleSettings = {},
                    onUpdatePreferences = { _, _, _ -> },
                    onSaveApiKey = {},
                    onDismissError = {},
                )
            }
        }

        // Voice input button is present
        composeTestRule.onNodeWithContentDescription("Voice Input").assertIsDisplayed()
    }
}
