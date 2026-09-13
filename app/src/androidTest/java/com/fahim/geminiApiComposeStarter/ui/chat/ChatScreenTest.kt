package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.fahim.geminiApiComposeStarter.data.ChatEntity
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Test
    fun chatScreen_displaysMessages() {
        val messages = listOf(
            ChatEntity(id = 1, role = "user", content = "Hi"),
            ChatEntity(id = 2, role = "model", content = "Hello")
        )
        val state = ChatUiState(messages = messages)
        val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(400.dp, 800.dp))

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = state,
                    windowSizeClass = windowSizeClass,
                    onSend = {},
                    onClear = {},
                    onErrorDismissed = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Hi").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hello").assertIsDisplayed()
    }
}
