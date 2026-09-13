package com.example.c001manavassignment1.ui.chat

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.example.c001manavassignment1.data.GeminiRepository
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Test
    fun chatScreen_elementsExist() {
        val uiState = ChatUiState(
            inputText = "Hello",
            isLoading = false
        )
        
        composeTestRule.setContent {
            ChatScreen(
                uiState = uiState,
                windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(400.dp, 800.dp)),
                onTextChange = {},
                onSend = {},
                onMicClick = {},
                onToggleDarkMode = {},
                onDismissError = {},
                onNewChat = {},
                onSelectConversation = {},
                onDeleteConversation = {},
                onError = {}
            )
        }

        // Check for title
        composeTestRule.onNodeWithText("Gemini AI Chat").assertExists()
        
        // Check for buttons
        composeTestRule.onNodeWithContentDescription("Send").assertExists()
        composeTestRule.onNodeWithContentDescription("Voice Input").assertExists()
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Test
    fun chatScreen_showsEmptyState() {
        val uiState = ChatUiState(
            messages = emptyList(),
            isLoading = false
        )
        
        composeTestRule.setContent {
            ChatScreen(
                uiState = uiState,
                windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(400.dp, 800.dp)),
                onTextChange = {},
                onSend = {},
                onMicClick = {},
                onToggleDarkMode = {},
                onDismissError = {},
                onNewChat = {},
                onSelectConversation = {},
                onDeleteConversation = {},
                onError = {}
            )
        }

        composeTestRule.onNodeWithText("Welcome to Gemini AI").assertExists()
        composeTestRule.onNodeWithText("Start a conversation below!").assertExists()
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Test
    fun chatScreen_displaysMessages() {
        val messages = listOf(
            GeminiRepository.ChatMessage(1, "User message", true),
            GeminiRepository.ChatMessage(2, "AI response", false)
        )
        val uiState = ChatUiState(
            messages = messages,
            isLoading = false
        )
        
        composeTestRule.setContent {
            ChatScreen(
                uiState = uiState,
                windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(400.dp, 800.dp)),
                onTextChange = {},
                onSend = {},
                onMicClick = {},
                onToggleDarkMode = {},
                onDismissError = {},
                onNewChat = {},
                onSelectConversation = {},
                onDeleteConversation = {},
                onError = {}
            )
        }

        composeTestRule.onNodeWithText("User message").assertExists()
        composeTestRule.onNodeWithText("AI response").assertExists()
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Test
    fun chatScreen_showsError() {
        val uiState = ChatUiState(
            error = "Test Error Message"
        )
        
        composeTestRule.setContent {
            ChatScreen(
                uiState = uiState,
                windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(400.dp, 800.dp)),
                onTextChange = {},
                onSend = {},
                onMicClick = {},
                onToggleDarkMode = {},
                onDismissError = {},
                onNewChat = {},
                onSelectConversation = {},
                onDeleteConversation = {},
                onError = {}
            )
        }

        composeTestRule.onNodeWithText("Test Error Message").assertExists()
        composeTestRule.onNodeWithText("Dismiss").assertExists()
    }
}
