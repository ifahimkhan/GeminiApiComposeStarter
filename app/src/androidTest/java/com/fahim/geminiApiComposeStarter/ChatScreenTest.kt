package com.fahim.geminiApiComposeStarter

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.fahim.geminiApiComposeStarter.data.*
import com.fahim.geminiApiComposeStarter.ui.chat.*
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.*
import org.junit.Assert.*
import java.io.File

class ChatScreenTest {
    @get:Rule val compose = createComposeRule()
    private fun render(state: ChatUiState, actions: ChatActions = ChatActions()) { compose.setContent { GeminiApiComposeStarterTheme { ChatScreen(state, actions) } } }
    @Test fun modelSelectorOpensFromChatHeader() {
        var opened = Sheet.NONE
        render(ChatUiState(destination = Destination.CHAT, isInitializing = false), ChatActions(sheet = { opened = it }))
        compose.onNodeWithTag("model-selector").performClick()
        assertEquals(Sheet.MODEL, opened)
    }
    @Test fun modelPickerDisplaysSelectionAndChoosesAnotherModel() {
        val first = GeminiModels.options.first().id
        val other = GeminiModels.options[1].id
        var selected = ""
        render(ChatUiState(destination = Destination.CHAT, sheet = Sheet.MODEL, preferences = UserPreferences(modelId = first, reducedMotion = true)), ChatActions(model = { selected = it }))
        compose.onNodeWithTag("model-$first").assertIsSelected()
        screenshot("model-picker")
        compose.onNodeWithTag("model-$other").performScrollTo().performClick()
        assertEquals(other, selected)
    }
    @Test fun modelChangeDuringReplyExplainsNextMessageBehavior() {
        render(ChatUiState(destination = Destination.CHAT, sheet = Sheet.MODEL, isLoading = true, preferences = UserPreferences(reducedMotion = true)))
        compose.onNodeWithText("Your reply is still on its way. This choice applies to your next message.").performScrollTo().assertIsDisplayed()
    }
    @Test fun composerSendsTypedValue() {
        val state = mutableStateOf(ChatUiState(destination = Destination.CHAT, isInitializing = false))
        var sent = ""
        compose.setContent { GeminiApiComposeStarterTheme { ChatScreen(state.value, ChatActions(prompt = { state.value = state.value.copy(prompt = it) }, send = { sent = state.value.prompt })) } }
        compose.onNodeWithTag("prompt").performTextInput("Hello Judy")
        compose.onNodeWithTag("send").performClick()
        assertEquals("Hello Judy", sent)
    }
    @Test fun pendingRequestShowsCircularIndicatorAndDisablesSend() {
        render(ChatUiState(destination = Destination.CHAT, isInitializing = false, isLoading = true))
        compose.onNodeWithTag("loading").assertIsDisplayed()
        compose.onNodeWithTag("send").assertIsNotEnabled()
    }
    @Test fun longPendingPromptKeepsProgressVisible() {
        val user = ChatMessage("long", "c", "t", 0, Role.USER, "A long question. ".repeat(300), Delivery.PENDING)
        render(ChatUiState(destination = Destination.CHAT, conversationId = "c", isInitializing = false, isLoading = true, pendingTurnId = "t", messages = listOf(user)))
        compose.onNodeWithTag("loading").assertIsDisplayed()
    }
    @Test fun failedMessageOffersRetry() {
        val message = ChatMessage("u", "c", "t", 0, Role.USER, "A failed question", Delivery.FAILED)
        var retried = false
        render(ChatUiState(destination = Destination.CHAT, conversationId = "c", messages = listOf(message), isInitializing = false), ChatActions(retry = { retried = true }))
        compose.onNodeWithText("Retry").performClick(); assertTrue(retried)
    }
    @Test fun restoredHistoryOpensTheCorrectConversation() {
        var selected = ""
        render(ChatUiState(destination = Destination.HISTORY, conversations = conversations, isInitializing = false), ChatActions(openConversation = { selected = it.id }))
        compose.onNodeWithText("AI improve disease detection").performClick(); assertEquals("health", selected)
    }
    @Test fun voiceReviewDoesNotSendAutomatically() {
        var opened = false
        render(ChatUiState(destination = Destination.VOICE, transcript = "Explain data preprocessing", voiceStatus = VoiceStatus.TRANSCRIPT, preferences = UserPreferences(reducedMotion = true)), ChatActions(useTranscript = { opened = true }))
        compose.onNodeWithText("Explain data preprocessing").assertIsDisplayed()
        compose.onNodeWithContentDescription("Open chat").performClick(); assertTrue(opened)
    }
    @Test fun homeReferenceScreenshot() {
        render(ChatUiState(conversations = conversations, isInitializing = false))
        compose.onNodeWithText("How can I assist\nyou today?").assertIsDisplayed()
        screenshot("home-reference")
    }
    @Test fun voiceReferenceScreenshot() {
        render(ChatUiState(destination = Destination.VOICE, transcript = "Explain the data pre-processing process in machine learning", voiceStatus = VoiceStatus.TRANSCRIPT, preferences = UserPreferences(reducedMotion = true)))
        screenshot("voice-reference")
    }
    @Test fun chatReferenceScreenshot() {
        render(ChatUiState(destination = Destination.CHAT, conversationId = "c", isInitializing = false, messages = listOf(
            ChatMessage("u", "c", "t", 0, Role.USER, "Explain the data pre-processing process in machine learning before it is fed into the model."),
            ChatMessage("a", "c", "t", 1, Role.ASSISTANT, "Data pre-processing is a critical stage in machine learning that aim to clean, transform and format raw data into a structured, understandable, and efficient form for ML algorithms. The guiding principle is \"Garbage In, Garbage Out,\" meaning inconsistent or poor-quality data will inevitably result in a poorly performing model. Transform format raw data into a structured, understandable, and efficient form for ML algorithms.")
        )))
        compose.onNodeWithContentDescription("Copy answer").assertIsDisplayed()
        screenshot("chat-reference")
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val folder = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        val bitmap = if (name == "model-picker") InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            else compose.onRoot().captureToImage().asAndroidBitmap()
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    companion object {
        val conversations = listOf(
            Conversation("research", "AI for Research Paper Drafting", Category.RESEARCH, "\"Can you help me draft an abstract for a paper…\""),
            Conversation("health", "AI improve disease detection", Category.HEALTH, "\"What is the main challenge of implementing…\""),
            Conversation("paper", "AI for Research Paper", Category.RESEARCH, "Smarter research, faster results with AI-powered assistance.")
        )
    }
}
