package com.fahim.geminiApiComposeStarter

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class LiveGeminiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun realModelResponseAndRuntimeScreenshots() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveGemini") == "true")
        assumeTrue(BuildConfig.GEMINI_API_KEY.isNotBlank())
        val app = compose.activity.application as ChatApplication
        compose.waitForIdle()
        var previous: List<ChatMessage> = emptyList()
        runBlocking { previous = app.conversations.messages.first() }
        compose.onNodeWithText("Light").performClick()
        compose.onNodeWithTag("prompt").performTextInput("Explain Jetpack Compose in two short sentences.")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Send").performClick()
        compose.onNodeWithTag("response_progress").assertIsDisplayed()
        screenshot("phone-loading.png")
        compose.waitUntil(timeoutMillis = 90_000) {
            runBlocking { app.conversations.messages.first().count { !it.isUser } } > previous.count { !it.isUser }
        }
        compose.waitForIdle()
        val reply = runBlocking { app.conversations.messages.first().last() }
        assertTrue("A real Gemini response must be persisted", !reply.isUser && reply.text.isNotBlank())
        screenshot("phone-light-conversation.png")
        compose.onNodeWithText("Dark").performClick()
        compose.waitForIdle()
        screenshot("phone-dark-conversation.png")
    }

    private fun screenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "assignment-screenshots")
        directory.mkdirs()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        checkNotNull(bitmap)
        File(directory, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
