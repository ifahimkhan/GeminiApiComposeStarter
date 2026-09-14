package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatScreenDisplaysGeminiTitle() {
        composeTestRule.setContent {
            ChatScreen(
                state = ChatUiState(),
                onPromptChange = {},
                onSend = {},
                onCancelRequest = {},
                onClearError = {},
                onNewChat = {},
                onSelectConversation = {},
                isDarkMode = false,
                onDarkModeChange = {}
            )
        }

        composeTestRule
            .onNodeWithText("Gemini AI")
            .assertIsDisplayed()
    }

    @Test
    fun chatScreenDisplaysNewChat() {
        composeTestRule.setContent {
            ChatScreen(
                state = ChatUiState(),
                onPromptChange = {},
                onSend = {},
                onCancelRequest = {},
                onClearError = {},
                onNewChat = {},
                onSelectConversation = {},
                isDarkMode = false,
                onDarkModeChange = {}
            )
        }

        composeTestRule
            .onNodeWithText("New Chat")
            .assertIsDisplayed()
    }

    @Test
    fun userMessageIsDisplayed() {
        val state = ChatUiState(
            messages = listOf(
                ChatMessage(
                    id = 1L,
                    text = "Hello Gemini",
                    isUser = true
                )
            )
        )

        composeTestRule.setContent {
            ChatScreen(
                state = state,
                onPromptChange = {},
                onSend = {},
                onCancelRequest = {},
                onClearError = {},
                onNewChat = {},
                onSelectConversation = {},
                isDarkMode = false,
                onDarkModeChange = {}
            )
        }

        composeTestRule
            .onNodeWithText("Hello Gemini")
            .assertIsDisplayed()
    }

    @Test
    fun geminiResponseIsDisplayed() {
        val state = ChatUiState(
            messages = listOf(
                ChatMessage(
                    id = 2L,
                    text = "Hello! I am Gemini.",
                    isUser = false
                )
            )
        )

        composeTestRule.setContent {
            ChatScreen(
                state = state,
                onPromptChange = {},
                onSend = {},
                onCancelRequest = {},
                onClearError = {},
                onNewChat = {},
                onSelectConversation = {},
                isDarkMode = false,
                onDarkModeChange = {}
            )
        }

        composeTestRule
            .onNodeWithText("Hello! I am Gemini.")
            .assertIsDisplayed()
    }

    @Test
    fun loadingMessageIsDisplayed() {
        val state = ChatUiState(
            isLoading = true
        )

        composeTestRule.setContent {
            ChatScreen(
                state = state,
                onPromptChange = {},
                onSend = {},
                onCancelRequest = {},
                onClearError = {},
                onNewChat = {},
                onSelectConversation = {},
                isDarkMode = false,
                onDarkModeChange = {}
            )
        }

        composeTestRule
            .onNodeWithText("Gemini is thinking...")
            .assertIsDisplayed()
    }

    @Test
    fun errorMessageIsDisplayed() {
        val state = ChatUiState(
            errorMessage = "Something went wrong. Please try again."
        )

        composeTestRule.setContent {
            ChatScreen(
                state = state,
                onPromptChange = {},
                onSend = {},
                onCancelRequest = {},
                onClearError = {},
                onNewChat = {},
                onSelectConversation = {},
                isDarkMode = false,
                onDarkModeChange = {}
            )
        }

        composeTestRule
            .onNodeWithText(
                "Something went wrong. Please try again."
            )
            .assertIsDisplayed()
    }
}