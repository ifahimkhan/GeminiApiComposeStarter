package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun displaysUserAndGeminiMessages() {
        composeRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    messages = listOf(
                        ChatMessageEntity(id = 1, role = "user", content = "Hi Gemini"),
                        ChatMessageEntity(id = 2, role = "gemini", content = "Hello there"),
                    ),
                    windowWidthSizeClass = WindowWidthSizeClass.Compact,
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeRule.onNodeWithText("Hi Gemini").assertIsDisplayed()
        composeRule.onNodeWithText("Hello there").assertIsDisplayed()
    }
}