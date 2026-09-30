package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.fahim.geminiApiComposeStarter.data.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.ui.theme.ThemeMode
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyState_showsExpectedElements() {
        composeTestRule.setContent {
            ChatScreen(
                state = ChatUiState(),
                windowSizeClass = null,
                onPromptChange = {},
                onSend = {},
                onClearHistory = {},
                onThemeChange = {},
                onDismissError = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Send").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Voice Input").assertIsDisplayed()
    }

    @Test
    fun userCanTypeMessage() {
        var typedText = ""
        composeTestRule.setContent {
            ChatScreen(
                state = ChatUiState(prompt = typedText),
                windowSizeClass = null,
                onPromptChange = { typedText = it },
                onSend = {},
                onClearHistory = {},
                onThemeChange = {},
                onDismissError = {}
            )
        }

        composeTestRule.onNodeWithText("Enter your prompt here").performTextInput("Hello Gemini")
    }

    @Test
    fun chatBubbleRendersSuppliedState() {
        val messages = listOf(
            ChatMessageEntity(id = 1, text = "User message", isUser = true, timestamp = 0L),
            ChatMessageEntity(id = 2, text = "Gemini message", isUser = false, timestamp = 1L)
        )
        
        composeTestRule.setContent {
            ChatScreen(
                state = ChatUiState(messages = messages),
                windowSizeClass = null,
                onPromptChange = {},
                onSend = {},
                onClearHistory = {},
                onThemeChange = {},
                onDismissError = {}
            )
        }

        composeTestRule.onNodeWithText("User message").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gemini message").assertIsDisplayed()
    }

    @Test
    fun loadingIndicatorAppears() {
        composeTestRule.setContent {
            ChatScreen(
                state = ChatUiState(isLoading = true),
                windowSizeClass = null,
                onPromptChange = {},
                onSend = {},
                onClearHistory = {},
                onThemeChange = {},
                onDismissError = {}
            )
        }

    }

    @Test
    fun errorCardAppears() {
        composeTestRule.setContent {
            ChatScreen(
                state = ChatUiState(errorMessage = "Test Error"),
                windowSizeClass = null,
                onPromptChange = {},
                onSend = {},
                onClearHistory = {},
                onThemeChange = {},
                onDismissError = {}
            )
        }

        composeTestRule.onNodeWithText("Test Error").assertIsDisplayed()
    }

    @Test
    fun clearHistoryControlExists() {
        composeTestRule.setContent {
            ChatScreen(
                state = ChatUiState(),
                windowSizeClass = null,
                onPromptChange = {},
                onSend = {},
                onClearHistory = {},
                onThemeChange = {},
                onDismissError = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Clear History").assertIsDisplayed()
    }
}
