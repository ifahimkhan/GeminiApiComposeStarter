package com.fahim.geminiApiComposeStarter

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.ui.chat.*
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class ChatScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun typingEnablesSendAndDispatchesAction() {
        var sent = false
        compose.setContent {
            var state by remember { mutableStateOf(ChatUiState(historyLoaded = true)) }
            MaterialTheme { ChatScreen(state, onInputChange = { state = state.copy(input = it) },
                onSend = { sent = true }, onDismissError = {}, onCompactChange = {}, onClearHistory = {}, onVoice = {}) }
        }
        compose.onNodeWithTag("send").assertIsNotEnabled()
        compose.onNodeWithTag("prompt").performTextInput("Hello")
        compose.onNodeWithTag("send").assertIsEnabled().performClick()
        compose.runOnIdle { assertTrue(sent) }
    }
    @Test fun loadingDisablesSendAndShowsProgress() {
        compose.setContent { MaterialTheme { ChatScreen(ChatUiState(input = "Hello", isLoading = true, historyLoaded = true),
            onInputChange = {}, onSend = {}, onDismissError = {}, onCompactChange = {}, onClearHistory = {}, onVoice = {}) } }
        compose.onNodeWithTag("loading").assertIsDisplayed()
        compose.onNodeWithTag("send").assertIsNotEnabled()
    }
    @Test fun errorCanBeDismissedAndVoiceActionWorks() {
        var dismissed = false; var voice = false
        compose.setContent { MaterialTheme { ChatScreen(ChatUiState(errorMessage = "Offline"),
            onInputChange = {}, onSend = {}, onDismissError = { dismissed = true },
            onCompactChange = {}, onClearHistory = {}, onVoice = { voice = true }) } }
        compose.onNodeWithText("Offline").assertIsDisplayed()
        compose.onNodeWithText("Dismiss").performClick()
        compose.onNodeWithContentDescription("Voice input").performClick()
        compose.runOnIdle { assertTrue(dismissed); assertTrue(voice) }
    }
    @Test fun expandedLayoutShowsHistoryAndPreference() {
        var compact = false
        compose.setContent { MaterialTheme { ChatScreen(ChatUiState(messages = listOf(
            ChatMessageEntity(1, "Hello", true), ChatMessageEntity(2, "Welcome", false))),
            widthSizeClass = WindowWidthSizeClass.Expanded,
            onInputChange = {}, onSend = {}, onDismissError = {}, onCompactChange = { compact = it }, onClearHistory = {}, onVoice = {}) } }
        compose.onNodeWithText("Hello").assertIsDisplayed()
        compose.onNodeWithText("Welcome").assertIsDisplayed()
        compose.onNodeWithTag("compactSwitch").performClick()
        compose.runOnIdle { assertTrue(compact) }
    }
}
