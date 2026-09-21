package com.fahim.geminiApiComposeStarter

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Opt-in live smoke test: uses quota only when -Pandroid.testInstrumentationRunnerArguments.liveGemini=true. */
class LiveChatTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun liveReplyAndScreenshots() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveGemini") == "true")
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("prompt").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("prompt").performTextInput("Reply with exactly: Hello N103")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("send").assertIsEnabled().performClick()
        saveScreenshot("phone-loading.png")
        compose.waitUntil(60_000) { compose.onAllNodesWithTag("loading").fetchSemanticsNodes().isEmpty() }
        saveScreenshot("phone-request-result.png")
        compose.onNodeWithTag("error").assertDoesNotExist()
        compose.onAllNodesWithText("Gemini").onLast().assertIsDisplayed()
        saveScreenshot("phone-live-chat.png")
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Gemini").fetchSemanticsNodes().isNotEmpty() }
        saveScreenshot("phone-restored-chat.png")
    }
    private fun saveScreenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val output = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        val dir = (output?.let { File(it) } ?: File(context.getExternalFilesDir(null), "assignment-screenshots")).apply { mkdirs() }
        File(dir, name).outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
