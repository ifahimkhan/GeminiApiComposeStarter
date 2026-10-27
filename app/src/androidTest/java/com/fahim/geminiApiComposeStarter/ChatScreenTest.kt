package com.fahim.geminiApiComposeStarter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.fahim.geminiApiComposeStarter.data.local.MessageEntity
import com.fahim.geminiApiComposeStarter.ui.chat.ChatScreen
import com.fahim.geminiApiComposeStarter.ui.chat.ChatUiState
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsEmptyStatePlaceholderInitially() {
        composeRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeRule.onNodeWithText("Response will be displayed here!")
            .assertIsDisplayed()
    }

    @Test
    fun rendersConversationBubblesForUserAndGemini() {
        val sampleHistory = listOf(
            MessageEntity(id = 1, content = "Hi Gemini", fromUser = true),
            MessageEntity(id = 2, content = "Hello! How can I help?", fromUser = false),
        )

        composeRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = sampleHistory),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeRule.onNodeWithText("Hi Gemini").assertIsDisplayed()
        composeRule.onNodeWithText("Hello! How can I help?").assertIsDisplayed()
    }

    @Test
    fun acceptsTextInputInPromptField() {
        var currentInput = ""
        composeRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = currentInput),
                    onPromptChange = { currentInput = it },
                    onSend = {},
                )
            }
        }

        composeRule.onNodeWithText("Enter your prompt here")
            .performTextInput("Compose UI test query")
    }
}
